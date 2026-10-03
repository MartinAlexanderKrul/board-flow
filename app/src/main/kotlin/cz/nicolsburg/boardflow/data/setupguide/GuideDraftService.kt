package cz.nicolsburg.boardflow.data.setupguide

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import cz.nicolsburg.boardflow.data.SecurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Turns a rulebook PDF the user picked into a draft guide: copies the file, asks Gemini with the
 * user's keys and models, and saves the result as the user's guide marked "not reviewed".
 */
class GuideDraftService(
    private val context: Context,
    private val prefs: SecurePreferences,
    private val repository: SetupGuideRepository,
    private val generator: GeminiGuideDraftGenerator = GeminiGuideDraftGenerator()
) {
    fun hasGeminiKey(): Boolean = prefs.hasGeminiKey()

    /** Returns null on success, or a message for the user. */
    suspend fun draftFromRulebook(pdf: Uri, gameId: Int, gameName: String): String? {
        if (!prefs.hasGeminiKey()) return "Add a Gemini key in Settings > Scan first"
        val file = File(context.cacheDir, "rulebook-draft.pdf")
        return try {
            val copied = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(pdf)?.use { input ->
                        file.outputStream().use { input.copyTo(it) }
                    } != null
                }.getOrDefault(false)
            }
            if (!copied) return "Could not open that file"
            val name = withContext(Dispatchers.IO) { runCatching { displayName(pdf) }.getOrNull() }
            if (file.length() > MAX_PDF_BYTES) return "That PDF is too large (over 50 MB)"
            val config = GuideDraftAiConfig(
                apiKeys = listOf(prefs.geminiApiKey) + prefs.getGeminiExtraApiKeys(),
                models = prefs.getGeminiModelCandidates(preferLite = false),
                onModelExhausted = { prefs.markModelExhausted(it) },
                onModelUnavailable = { prefs.markGeminiModelUnavailable(it) }
            )
            val draft = generator.draft(file, name, gameId, gameName, config)
                .getOrElse { return it.message ?: "Gemini could not draft a guide" }
            repository.saveDraftGuide(draft).firstOrNull()?.let { "The draft did not pass the guide checks: $it" }
        } finally {
            withContext(Dispatchers.IO) { file.delete() }
        }
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private companion object {
        // Gemini reads PDFs up to 50 MB.
        const val MAX_PDF_BYTES = 50L * 1024 * 1024
    }
}
