package cz.nicolsburg.boardflow.data.scoresheet

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import cz.nicolsburg.boardflow.data.SecurePreferences
import cz.nicolsburg.boardflow.data.setupguide.DraftRefusedException
import cz.nicolsburg.boardflow.data.setupguide.GeminiGuideDraftGenerator
import cz.nicolsburg.boardflow.data.setupguide.GuideDraftAiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Turns a rulebook PDF the user picked into a draft score sheet, the way `GuideDraftService` does
 * for Quick Setup guides: [start] copies the file and hands it to [ScoreSheetDraftWorker], which
 * keeps running after the user leaves the app; [draftFromFile] is the work itself. The result is
 * saved as the user's sheet for the game, marked "not reviewed".
 */
class ScoreSheetDraftService(
    private val context: Context,
    private val prefs: SecurePreferences,
    private val repository: ScoreSheetRepository,
    private val generator: GeminiGuideDraftGenerator = GeminiGuideDraftGenerator()
) {
    sealed interface DraftState {
        data object Idle : DraftState
        data object Running : DraftState
        data object Succeeded : DraftState
        data class Failed(val message: String) : DraftState
    }

    fun hasGeminiKey(): Boolean = prefs.hasGeminiKey()

    /** Copies the PDF into app storage and queues the draft. Returns null when queued, or a message for the user. */
    suspend fun start(pdf: Uri, gameId: Int, gameName: String): String? {
        if (!prefs.hasGeminiKey()) return "Add a Gemini key in Settings > Scan first"
        val file = File(context.filesDir, "score-sheet-drafts/$gameId.pdf")
        val copied = withContext(Dispatchers.IO) {
            runCatching {
                file.parentFile?.mkdirs()
                context.contentResolver.openInputStream(pdf)?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                } != null
            }.getOrDefault(false)
        }
        if (!copied) return "Could not open that file"
        if (file.length() > MAX_PDF_BYTES) {
            file.delete()
            return "That PDF is too large (over 50 MB)"
        }
        val sourceName = withContext(Dispatchers.IO) { runCatching { displayName(pdf) }.getOrNull() }
        val request = OneTimeWorkRequestBuilder<ScoreSheetDraftWorker>()
            .setInputData(
                workDataOf(
                    ScoreSheetDraftWorker.KEY_GAME_ID to gameId,
                    ScoreSheetDraftWorker.KEY_GAME_NAME to gameName,
                    ScoreSheetDraftWorker.KEY_PDF_PATH to file.absolutePath,
                    ScoreSheetDraftWorker.KEY_SOURCE_NAME to sourceName
                )
            )
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(ScoreSheetDraftWorker.TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(gameId), ExistingWorkPolicy.REPLACE, request)
        return null
    }

    /** The draft for [gameId]: running, or how the last one ended. */
    fun state(gameId: Int): Flow<DraftState> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(workName(gameId)).map { infos ->
            val info = infos.lastOrNull() ?: return@map DraftState.Idle
            when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> DraftState.Running
                WorkInfo.State.SUCCEEDED -> DraftState.Succeeded
                WorkInfo.State.FAILED -> DraftState.Failed(
                    info.outputData.getString(ScoreSheetDraftWorker.KEY_ERROR) ?: "Gemini could not draft a score sheet"
                )
                WorkInfo.State.CANCELLED -> DraftState.Idle
            }
        }

    /** Drafts and saves the sheet. Returns null on success, or a message for the user. */
    suspend fun draftFromFile(file: File, sourceName: String?, gameId: Int, gameName: String): String? {
        if (!prefs.hasGeminiKey()) return "Add a Gemini key in Settings > Scan first"
        val config = GuideDraftAiConfig(
            apiKeys = listOf(prefs.geminiApiKey) + prefs.getGeminiExtraApiKeys(),
            models = modelsLastGoodFirst(),
            onModelBusy = { prefs.markModelExhausted(it, BUSY_MODEL_SKIP_MS) },
            onModelUnavailable = { prefs.markGeminiModelUnavailable(it) }
        )
        val sheet = generator.draft(file, ScoreSheetDraftPrompt.spec(gameId, gameName, sourceName), config).getOrElse { error ->
            // The rulebook has nothing to put on a sheet: remember it, so the draft is not offered again.
            if (error is DraftRefusedException) repository.markNoSheet(gameId, error.reason, ScoreSheetRepository.SOURCE_AI)
            return error.message ?: "Gemini could not draft a score sheet"
        }
        sheet.aiModel?.let { model ->
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LAST_MODEL, model).apply()
        }
        return repository.save(sheet).firstOrNull()?.let { "The draft did not pass the score sheet checks: $it" }
    }

    /** The model that drafted the last sheet goes first: it is likely to work again. */
    private fun modelsLastGoodFirst(): List<String> {
        val candidates = prefs.getGeminiModelCandidates(preferLite = false)
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_MODEL, null)
        val match = candidates.firstOrNull { it == last || it.substringAfterLast('/') == last } ?: return candidates
        return listOf(match) + (candidates - match)
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    companion object {
        private const val MAX_PDF_BYTES = 50L * 1024 * 1024
        private const val BUSY_MODEL_SKIP_MS = 15 * 60 * 1000L
        private const val PREFS = "score_sheet_draft"
        private const val KEY_LAST_MODEL = "last_model"

        fun workName(gameId: Int) = "score_sheet_draft_$gameId"
    }
}
