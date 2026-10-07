package cz.nicolsburg.boardflow.data.setupguide

import android.util.Log
import cz.nicolsburg.boardflow.data.GeminiModels
import cz.nicolsburg.boardflow.model.SetupGuide
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The Gemini keys and models a draft may use, models in the order to try them. */
data class GuideDraftAiConfig(
    val apiKeys: List<String>,
    val models: List<String>,
    /** The model answered 429/503: busy for now, skip it for a while. */
    val onModelBusy: (String) -> Unit = {},
    /** 404 or a rejected request: the model is gone or cannot read PDFs. */
    val onModelUnavailable: (String) -> Unit = {}
)

/**
 * Drafts a Quick Setup guide from a rulebook PDF with Gemini. Small PDFs go inline; larger ones
 * are uploaded through the File API (once per key, since uploads belong to the key's project).
 *
 * Busy models are not waited for one after another: [PARALLEL_MODELS] models work on the same
 * rulebook at once, the first valid guide wins and the other requests are cancelled. A model
 * that fails (busy, retired, slower than [MODEL_TIMEOUT_MS], or an answer that is still invalid
 * after one retry with the validator's problems) is replaced by the next candidate straight away.
 *
 * Full Flash models are preferred over Flash-Lite: compared on the Arcs rulebook, the full
 * models made no factual errors, while Lite runs dropped whole setup steps and one gave a wrong
 * card count that no validation can catch. Lite models are tried only after every full model,
 * and a Lite guide that arrives while a full model is still working is held as a fallback.
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
            val inline = if (pdf.length() <= INLINE_LIMIT_BYTES) Base64.getEncoder().encodeToString(pdf.readBytes()) else null
            val uploads = mutableMapOf<String, String>()
            val uploadLock = Mutex()
            val candidates = config.models.take(MAX_MODELS)
            val queue = ArrayDeque(candidates.filterNot(::isLite) + candidates.filter(::isLite))
            var fullInFlight = 0
            var liteFallback: SetupGuide? = null
            val queueLock = Mutex()
            val failures = mutableListOf<String>()
            val winner = CompletableDeferred<SetupGuide>()

            suspend fun pdfPart(key: String): JSONObject = if (inline != null) {
                JSONObject().put("inline_data", JSONObject().put("mime_type", PDF).put("data", inline))
            } else {
                val uri = uploadLock.withLock { uploads[key] ?: upload(pdf, key).also { uploads[key] = it } }
                JSONObject().put("file_data", JSONObject().put("mime_type", PDF).put("file_uri", uri))
            }

            coroutineScope {
                val lanes = List(PARALLEL_MODELS) { lane ->
                    launch {
                        // Staggered a little so two lanes do not hit the same busy moment.
                        if (lane > 0) delay(LANE_STAGGER_MS)
                        while (!winner.isCompleted) {
                            val model = queueLock.withLock {
                                // With a Lite guide already held, only a full model is worth another request.
                                val next = if (liteFallback == null) queue.firstOrNull() else queue.firstOrNull { !isLite(it) }
                                next?.also { queue.remove(it); if (!isLite(it)) fullInFlight++ }
                            } ?: break
                            val outcome = withTimeoutOrNull(MODEL_TIMEOUT_MS) {
                                tryModel(model, ::pdfPart, gameId, gameName, sourceName, config)
                            } ?: Outcome.Failed(SLOW).also { log("draft model=$model gave up after ${MODEL_TIMEOUT_MS / 1000} s") }
                            queueLock.withLock {
                                if (!isLite(model)) fullInFlight--
                                val fullLeft = fullInFlight > 0 || queue.any { !isLite(it) }
                                when (outcome) {
                                    is Outcome.Guide ->
                                        if (isLite(model) && fullLeft) {
                                            if (liteFallback == null) liteFallback = outcome.guide
                                        } else {
                                            winner.complete(outcome.guide)
                                        }
                                    is Outcome.Failed -> synchronized(failures) { failures += outcome.reason }
                                }
                                // No full model left to wait for: a held Lite guide is the answer.
                                if (!fullLeft) liteFallback?.let { winner.complete(it) }
                            }
                        }
                    }
                }
                launch {
                    lanes.joinAll()
                    liteFallback?.let { winner.complete(it) }
                    if (!winner.isCompleted) {
                        winner.completeExceptionally(IllegalStateException(summarise(synchronized(failures) { failures.toList() })))
                    }
                }
                val guide = winner.await()
                // The other lanes' requests are cancelled; their answers are not needed.
                coroutineContext[kotlinx.coroutines.Job]?.children?.forEach { it.cancel() }
                guide
            }
        }.recoverCatching { error ->
            // Network failures carry raw exception text; the user needs to know only that Gemini was unreachable.
            if (error is IOException) throw IllegalStateException("Could not reach Gemini. Check the connection and try again.", error)
            throw error
        }
    }

    private sealed interface Outcome {
        data class Guide(val guide: SetupGuide) : Outcome
        data class Failed(val reason: String) : Outcome
    }

    /** One model: its keys in turn while they are rate limited, and one retry with the validator's problems. */
    private suspend fun tryModel(
        model: String,
        pdfPart: suspend (String) -> JSONObject,
        gameId: Int,
        gameName: String,
        sourceName: String?,
        config: GuideDraftAiConfig
    ): Outcome {
        var keyIndex = 0
        var problems = emptyList<String>()
        var retriedWithProblems = false
        while (true) {
            val key = config.apiKeys[keyIndex]
            val body = requestBody(pdfPart(key), GuideDraftPrompt.build(gameId, gameName, problems))
            val endpoint = if (model.contains("/")) model else "v1beta/models/$model"
            log("draft model=$model key=${keyIndex + 1}/${config.apiKeys.size} retryWithProblems=${problems.size}")
            val started = System.currentTimeMillis()
            val (code, text) = post("$BASE/$endpoint:generateContent?key=$key", body)
            log("draft response model=$model code=$code length=${text.length} seconds=${(System.currentTimeMillis() - started) / 1000}")
            when {
                code in 200..299 -> {
                    val result = answerText(text)?.let {
                        runCatching { GuideDraftPrompt.parse(it, gameId, gameName, model.substringAfterLast('/'), sourceName) }.getOrNull()
                    }
                    if (result != null && result.second.isEmpty()) return Outcome.Guide(result.first)
                    val found = result?.second ?: listOf("The answer was not a guide in the requested JSON format")
                    if (retriedWithProblems) return Outcome.Failed(found.first())
                    retriedWithProblems = true
                    problems = found
                }
                code == 404 || (code == 400 && GeminiModels.isModelRejection(text)) -> {
                    config.onModelUnavailable(model)
                    return Outcome.Failed(UNAVAILABLE)
                }
                code == 429 || code == 503 -> {
                    if (!text.contains("limit: 0") && keyIndex + 1 < config.apiKeys.size) {
                        keyIndex++
                        continue
                    }
                    config.onModelBusy(model)
                    return Outcome.Failed(BUSY)
                }
                code == 400 && text.contains("pages", ignoreCase = true) ->
                    throw IllegalStateException("Gemini could not read this PDF. Try a smaller rulebook file.")
                else -> return Outcome.Failed("Gemini error $code")
            }
        }
    }

    private fun summarise(failures: List<String>): String {
        val problem = failures.firstOrNull { it != BUSY && it != UNAVAILABLE && it != SLOW && !it.startsWith("Gemini error") }
        return when {
            problem != null -> "The draft did not pass the guide checks: $problem"
            failures.isNotEmpty() && failures.all { it == BUSY || it == UNAVAILABLE || it == SLOW } -> "Gemini is busy right now. Try again in a few minutes."
            else -> failures.firstOrNull() ?: "Gemini could not draft a guide. Try again later."
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

    /** POSTs and returns the status and body; cancelling the coroutine cancels the HTTP call. */
    private suspend fun post(url: String, body: String): Pair<Int, String> {
        val response = client.newCall(Request.Builder().url(url).post(body.toRequestBody(JSON)).build()).await()
        return response.use { it.code to withContext(Dispatchers.IO) { it.body?.string().orEmpty() } }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) = cont.resume(response)
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isCancelled) cont.resumeWithException(e)
            }
        })
        cont.invokeOnCancellation { runCatching { cancel() } }
    }

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
        ).await()
        val uploadUrl = start.use { it.header("X-Goog-Upload-URL") }
            ?: throw IllegalStateException("Could not upload the rulebook (${start.code})")
        val uploaded = client.newCall(
            Request.Builder()
                .url(uploadUrl)
                .header("X-Goog-Upload-Offset", "0")
                .header("X-Goog-Upload-Command", "upload, finalize")
                .post(pdf.asRequestBody(PDF.toMediaType()))
                .build()
        ).await()
        val file = uploaded.use { response ->
            val text = withContext(Dispatchers.IO) { response.body?.string().orEmpty() }
            if (!response.isSuccessful) throw IllegalStateException("Could not upload the rulebook (${response.code})")
            JSONObject(text).getJSONObject("file")
        }
        var state = file.optString("state")
        val name = file.getString("name")
        repeat(30) {
            if (state != "PROCESSING") return@repeat
            delay(2000)
            val text = client.newCall(Request.Builder().url("$BASE/v1beta/$name?key=$key").get().build())
                .await().use { withContext(Dispatchers.IO) { it.body?.string().orEmpty() } }
            state = runCatching { JSONObject(text).optString("state") }.getOrDefault("")
        }
        if (state == "FAILED") throw IllegalStateException("Gemini could not read this PDF")
        log("uploaded rulebook ${pdf.length()} bytes as $name state=$state")
        return file.getString("uri")
    }

    private fun isLite(model: String) = model.contains("lite", ignoreCase = true)

    private fun log(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    private companion object {
        const val TAG = "GuideDraft"
        const val BASE = "https://generativelanguage.googleapis.com"
        const val PDF = "application/pdf"
        val JSON = "application/json".toMediaType()
        // Inline requests are capped at 20 MB and base64 adds a third.
        const val INLINE_LIMIT_BYTES = 14L * 1024 * 1024
        /** Models working on one draft at the same time. */
        const val PARALLEL_MODELS = 2
        const val LANE_STAGGER_MS = 1_500L
        /** Candidates tried at most, across all lanes. */
        const val MAX_MODELS = 6
        const val BUSY = "busy"
        const val SLOW = "slow"
        /** A model still thinking after this long is given up (one took over 10 minutes on Arcs). */
        const val MODEL_TIMEOUT_MS = 4 * 60 * 1000L
        const val UNAVAILABLE = "unavailable"
    }
}
