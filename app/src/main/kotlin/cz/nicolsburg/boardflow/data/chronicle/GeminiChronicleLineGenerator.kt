package cz.nicolsburg.boardflow.data.chronicle

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiChronicleLineGenerator : ChronicleLineGenerator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun generate(request: ChronicleRequest, config: ChronicleAiConfig): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val allKeys = listOf(config.apiKey) + config.availableApiKeys
                var currentModel = config.modelName
                var currentKeyIndex = 0
                var attempts = 0

                while (attempts < MAX_ATTEMPTS) {
                    attempts++
                    val currentApiKey = allKeys[currentKeyIndex]
                    val endpoint = if (currentModel.contains("/")) currentModel else "v1beta/models/$currentModel"
                    val url = "https://generativelanguage.googleapis.com/$endpoint:generateContent?key=$currentApiKey"
                    val requestBody = buildRequestBody(request)
                    logGemini("request chronicle attempt=$attempts/$MAX_ATTEMPTS model=$currentModel key=${currentKeyIndex + 1}/${allKeys.size} url=${redactApiKey(url)} body=${preview(requestBody)}")
                    val httpRequest = Request.Builder()
                        .url(url)
                        .post(requestBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    val response = client.newCall(httpRequest).execute()
                    val body = response.body?.string().orEmpty()
                    logGemini("response chronicle attempt=$attempts/$MAX_ATTEMPTS model=$currentModel code=${response.code} body=${preview(body)}")
                    when {
                        response.isSuccessful -> return@runCatching parseChronicleLine(body)
                        response.code == 429 || response.code == 503 -> {
                            if (attempts >= MAX_ATTEMPTS) throw IllegalStateException("All models are currently experiencing high demand (${response.code}). Please try again in a moment.")
                            val nextKeyIndex = currentKeyIndex + 1
                            if (nextKeyIndex < allKeys.size) {
                                logGemini("rotate-key chronicle http=${response.code} model=$currentModel key=${nextKeyIndex + 1}/${allKeys.size} attempt=$attempts/$MAX_ATTEMPTS")
                                currentKeyIndex = nextKeyIndex
                                delay(1000)
                                continue
                            }
                            val nextModel = findNextModel(currentModel, config.availableModels)
                            if (nextModel != null) {
                                logGemini("rotate-model chronicle http=${response.code} from=$currentModel to=$nextModel resetKey=1/${allKeys.size} attempt=$attempts/$MAX_ATTEMPTS")
                                currentModel = nextModel
                                currentKeyIndex = 0
                                delay(1000)
                                continue
                            }
                            throw IllegalStateException("All models are currently experiencing high demand (${response.code}). Please try again in a moment.")
                        }
                        else -> throw IllegalStateException("Chronicle API error ${response.code} (using $currentModel):\n$body")
                    }
                }

                throw IllegalStateException("Chronicle failed after $MAX_ATTEMPTS attempts")
            }
        }

    private fun buildRequestBody(request: ChronicleRequest): String {
        val prompt = """
            Write one short tabletop memory line.

            Use ONLY the concrete details provided.
            Prefer the quote when present.
            Do not add generic board game imagery.
            Do not mention dice, quests, adventure, heroes, victory, defeat, twists, turns, unless those words appear in the input.
            Do not soften negative quotes.
            Do not invent what happened.
            Do not summarize the game.
            Capture the emotional truth of the session.

            Style:
            - understated
            - specific
            - dry if the quote is negative
            - premium but natural
            - max 110 characters

            Additional constraints:
            - Avoid using the game name or player names unless absolutely necessary.
            - Avoid repeating mood words exactly as written; translate the feeling into fresher language.
            - Use player colors only if they help and stay concrete.
            - If the quote does not fit naturally, do not use it.
			
			Write one short premium tabletop chronicle line for a saved board game session.

			Your job:
			Capture the emotional memory of the session in one concise line.

			Very important:
			Be specific, not generic.
			Use the supplied details directly when useful.
			Do NOT invent gameplay events.
			Do NOT invent story details.
			Do NOT add fantasy narration.
			Do NOT sound like marketing copy.
			Do NOT summarize the game itself.
			Do NOT explain the mood.
			Do NOT mention AI.

			Theme handling:
			You MAY lightly infer atmosphere from the game title if the game is well known.
			Use at most ONE subtle thematic metaphor.
			Keep the metaphor understated.
			If unsure about the game theme, use neutral tabletop language instead.
			Never invent lore, locations, creatures, factions, or characters.

			Tone:
			reflective
			understated
			human
			emotionally observant
			premium
			concise
			sometimes dry or honest

			Good qualities:
			feels like a remembered moment
			sounds natural
			feels personal
			slightly poetic is okay
			grounded is better than dramatic

			Avoid:
			epic language
			“quest”
			“adventure”
			“heroes”
			“battle”
			“legendary”
			“unforgettable”
			“twists and turns”
			“dice rolled”
			“victory”
			“defeat”
			generic board game narration
			
			Generation rules:
			If a quote exists:
			Prefer building around the quote or its emotional meaning.
			Preserve negative or blunt tone honestly.
			Do not soften criticism.
			If moods exist but no quote:
			Create a short atmospheric reflection.
			Keep it grounded and subtle.
			If both exist:
			Combine them naturally without sounding written by AI.
			If the input is emotionally flat:
			Keep the output simple and restrained.

            Return JSON only:
            {"chronicleLine":"..."}

            Input:
            Game: ${request.gameName.ifBlank { "Unknown" }}
            Moods: ${request.moods.joinToString(", ").ifBlank { "None" }}
            Quote: ${request.quote.ifBlank { "None" }}
            Players: ${request.playerNames.joinToString(", ").ifBlank { "None" }}
            Colors: ${request.playerColors.joinToString(", ").ifBlank { "None" }}
        """.trimIndent()

        return JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.65)
                put("maxOutputTokens", 96)
                put("responseMimeType", "application/json")
            })
        }.toString()
    }

    private fun parseChronicleLine(responseJson: String): String {
        val root = JSONObject(responseJson)
        val rawText = root.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return JSONObject(rawText)
            .optString("chronicleLine", "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(100)
    }

    private fun findNextModel(currentModel: String, availableModels: List<String>): String? {
        if (availableModels.isEmpty()) return null
        val sorted = availableModels.sortedWith(compareByDescending { it.startsWith("gemini") })
        val currentIndex = sorted.indexOf(currentModel)
        return when {
            currentIndex >= 0 && currentIndex < sorted.lastIndex -> sorted[currentIndex + 1]
            currentIndex == -1 && sorted.isNotEmpty() -> sorted.first()
            else -> null
        }
    }

    private companion object {
        private const val TAG = "Chronicle"
        private const val MAX_ATTEMPTS = 10
    }

    private fun logGemini(message: String) {
        Log.d(TAG, "GEMINI $message")
    }

    private fun redactApiKey(url: String): String = url.replace(Regex("key=[^&]+"), "key=REDACTED")

    private fun preview(text: String, maxLength: Int = 320): String {
        val compact = text.replace(Regex("\\s+"), " ").trim()
        return if (compact.length <= maxLength) compact else compact.take(maxLength) + "..."
    }
}
