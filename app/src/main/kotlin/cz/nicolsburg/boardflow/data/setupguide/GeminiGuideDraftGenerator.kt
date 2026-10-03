package cz.nicolsburg.boardflow.data.setupguide

import android.util.Base64
import android.util.Log
import cz.nicolsburg.boardflow.data.GeminiModels
import cz.nicolsburg.boardflow.model.SetupGuide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** The Gemini keys and models a draft may use, in order. */
data class GuideDraftAiConfig(
    val apiKeys: List<String>,
    val models: List<String>,
    val onModelExhausted: (String) -> Unit = {},
    val onModelUnavailable: (String) -> Unit = {}
)

/**
 * Drafts a Quick Setup guide from a rulebook PDF with Gemini. Small PDFs go inline; larger ones
 * are uploaded through the File API (once per key, since uploads belong to the key's project).
 * An answer that fails [SetupGuideValidator] is sent back once with its problems listed.
 * Same model and key rotation as the score scan: 404 / rejected model -> next model,
 * 429 / 503 -> next key, then next model.
 */
class GeminiGuideDraftGenerator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        // Reading a whole rulebook and writing the guide can take a few minutes.
        .readTimeout(300, TimeUnit.SECONDS)
        .build()

    suspend fun draft(
        pdf: File,
        sourceName: String?,
        gameId: Int,
        gameName: String,
        config: GuideDraftAiConfig
    ): Result<SetupGuide> = withContext(Dispatchers.IO) {
        runCatching {
            require(config.apiKeys.isNotEmpty() && config.models.isNotEmpty()) { "No Gemini key" }
            val inline = if (pdf.length() <= INLINE_LIMIT_BYTES) Base64.encodeToString(pdf.readBytes(), Base64.NO_WRAP) else null
            val uploads = mutableMapOf<String, String>()
            val dropped = mutableSetOf<String>()
            var model = config.models.first()
            var keyIndex = 0
            var problems = emptyList<String>()
            var lastProblems = emptyList<String>()
            var attempts = 0

            while (attempts < MAX_ATTEMPTS) {
                attempts++
                val key = config.apiKeys[keyIndex]
                val pdfPart = if (inline != null) {
                    JSONObject().put("inline_data", JSONObject().put("mime_type", PDF).put("data", inline))
                } else {
                    val uri = uploads.getOrPut(key) { upload(pdf, key) }
                    JSONObject().put("file_data", JSONObject().put("mime_type", PDF).put("file_uri", uri))
                }
                val body = requestBody(pdfPart, GuideDraftPrompt.build(gameId, gameName, problems))
                val endpoint = if (model.contains("/")) model else "v1beta/models/$model"
                log("draft attempt=$attempts model=$model key=${keyIndex + 1}/${config.apiKeys.size} inline=${inline != null} retryWithProblems=${problems.size}")
                val response = client.newCall(
                    Request.Builder()
                        .url("$BASE/$endpoint:generateContent?key=$key")
                        .post(body.toRequestBody(JSON))
                        .build()
                ).execute()
                val text = response.body?.string().orEmpty()
                log("draft response model=$model code=${response.code} length=${text.length}")

                when {
                    response.isSuccessful -> {
                        val answer = answerText(text)
                        val result = answer?.let {
                            runCatching { GuideDraftPrompt.parse(it, gameId, gameName, model.substringAfterLast('/'), sourceName) }.getOrNull()
                        }
                        if (result != null && result.second.isEmpty()) return@runCatching result.first
                        lastProblems = result?.second ?: listOf("The answer was not a guide in the requested JSON format")
                        if (problems.isEmpty()) {
                            // One more try on the same model, told what was wrong.
                            problems = lastProblems
                            continue
                        }
                        // Still wrong after the retry: another model may do better.
                        dropped += model
                        val next = GeminiModels.next(model, config.models, dropped) ?: break
                        model = next
                        keyIndex = 0
                        problems = lastProblems
                    }
                    response.code == 404 || (response.code == 400 && GeminiModels.isModelRejection(text)) -> {
                        config.onModelUnavailable(model)
                        dropped += model
                        model = GeminiModels.next(model, config.models, dropped)
                            ?: throw IllegalStateException("No Gemini model could read the rulebook")
                        keyIndex = 0
                    }
                    response.code == 429 || response.code == 503 -> {
                        val noQuota = text.contains("limit: 0")
                        if (!noQuota && keyIndex + 1 < config.apiKeys.size) {
                            keyIndex++
                        } else {
                            config.onModelExhausted(model)
                            dropped += model
                            model = GeminiModels.next(model, config.models, dropped)
                                ?: throw IllegalStateException("Gemini is busy right now. Try again in a minute.")
                            keyIndex = 0
                        }
                        delay(1000)
                    }
                    response.code == 400 && text.contains("pages", ignoreCase = true) ->
                        throw IllegalStateException("Gemini could not read this PDF. Try a smaller rulebook file.")
                    else -> throw IllegalStateException("Gemini error ${response.code}")
                }
            }
            throw IllegalStateException(
                lastProblems.firstOrNull()?.let { "The draft did not pass the guide checks: $it" }
                    ?: "Gemini could not draft a guide. Try again later."
            )
        }.recoverCatching { error ->
            // Network failures carry raw exception text; the user needs to know only that Gemini was unreachable.
            if (error is IOException) throw IllegalStateException("Could not reach Gemini. Check the connection and try again.", error)
            throw error
        }
    }

    private fun requestBody(pdfPart: JSONObject, prompt: String): String = JSONObject().apply {
        put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(pdfPart).put(JSONObject().put("text", prompt)))))
        put("generationConfig", JSONObject().apply {
            put("temperature", 0.2)
            put("responseMimeType", "application/json")
            put("maxOutputTokens", 65536)
        })
    }.toString()

    /** The answer's text parts joined, or null when the model stopped early or answered nothing. */
    private fun answerText(response: String): String? = runCatching {
        val candidate = JSONObject(response).getJSONArray("candidates").getJSONObject(0)
        if (candidate.optString("finishReason") == "MAX_TOKENS") return null
        val parts = candidate.getJSONObject("content").getJSONArray("parts")
        (0 until parts.length()).map { parts.getJSONObject(it) }
            .filterNot { it.optBoolean("thought") }
            .joinToString("") { it.optString("text") }
            .ifBlank { null }
    }.getOrNull()

    /** Uploads the PDF with the File API and waits until Gemini has processed it; returns its uri. */
    private suspend fun upload(pdf: File, key: String): String {
        val start = client.newCall(
            Request.Builder()
                .url("$BASE/upload/v1beta/files?key=$key")
                .header("X-Goog-Upload-Protocol", "resumable")
                .header("X-Goog-Upload-Command", "start")
                .header("X-Goog-Upload-Header-Content-Length", pdf.length().toString())
                .header("X-Goog-Upload-Header-Content-Type", PDF)
                .post(JSONObject().put("file", JSONObject().put("display_name", "rulebook")).toString().toRequestBody(JSON))
                .build()
        ).execute()
        val uploadUrl = start.use { it.header("X-Goog-Upload-URL") }
            ?: throw IllegalStateException("Could not upload the rulebook (${start.code})")
        val uploaded = client.newCall(
            Request.Builder()
                .url(uploadUrl)
                .header("X-Goog-Upload-Offset", "0")
                .header("X-Goog-Upload-Command", "upload, finalize")
                .post(pdf.asRequestBody(PDF.toMediaType()))
                .build()
        ).execute()
        val file = uploaded.use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("Could not upload the rulebook (${response.code})")
            JSONObject(text).getJSONObject("file")
        }
        var state = file.optString("state")
        val name = file.getString("name")
        repeat(30) {
            if (state != "PROCESSING") return@repeat
            delay(2000)
            val text = client.newCall(Request.Builder().url("$BASE/v1beta/$name?key=$key").get().build())
                .execute().use { it.body?.string().orEmpty() }
            state = runCatching { JSONObject(text).optString("state") }.getOrDefault("")
        }
        if (state == "FAILED") throw IllegalStateException("Gemini could not read this PDF")
        log("uploaded rulebook ${pdf.length()} bytes as $name state=$state")
        return file.getString("uri")
    }

    private fun log(message: String) = Log.d(TAG, message)

    private companion object {
        const val TAG = "GuideDraft"
        const val BASE = "https://generativelanguage.googleapis.com"
        const val PDF = "application/pdf"
        val JSON = "application/json".toMediaType()
        // Inline requests are capped at 20 MB and base64 adds a third.
        const val INLINE_LIMIT_BYTES = 14L * 1024 * 1024
        const val MAX_ATTEMPTS = 6
    }
}
