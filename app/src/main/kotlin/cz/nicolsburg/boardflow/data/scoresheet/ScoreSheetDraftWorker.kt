package cz.nicolsburg.boardflow.data.scoresheet

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import cz.nicolsburg.boardflow.R
import cz.nicolsburg.boardflow.core.di.AppContainer
import cz.nicolsburg.boardflow.data.BackgroundNotifications
import java.io.File

/**
 * Drafts a score sheet from a staged rulebook PDF while the app may be closed, like
 * `GuideDraftWorker`: a quiet "Drafting a score sheet" notification while it runs, then one
 * saying the sheet is ready (tapping it opens the app) or what went wrong. Uses the shared
 * [AppContainer], so an open Log Play form shows the sheet as soon as it is saved.
 */
class ScoreSheetDraftWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val gameId get() = inputData.getInt(KEY_GAME_ID, 0)
    private val gameName get() = inputData.getString(KEY_GAME_NAME).orEmpty()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = BackgroundNotifications.progress(
            applicationContext,
            title = "Drafting a score sheet for $gameName",
            text = "Gemini is reading the rulebook. This can take a few minutes.",
            icon = R.drawable.ic_stat_guide,
            openIntent = openApp()
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(progressId(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(progressId(), notification)
        }
    }

    override suspend fun doWork(): Result {
        val path = inputData.getString(KEY_PDF_PATH) ?: return Result.failure()
        val pdf = File(path)
        if (gameId <= 0 || !pdf.exists()) return Result.failure(workDataOf(KEY_ERROR to "The rulebook file is gone. Pick it again."))
        runCatching { setForeground(getForegroundInfo()) }

        val problem = try {
            val container = AppContainer.get(applicationContext)
            container.scoreSheetRepository.load()
            container.scoreSheetDraftService.draftFromFile(pdf, inputData.getString(KEY_SOURCE_NAME), gameId, gameName)
        } finally {
            pdf.delete()
        }

        BackgroundNotifications.result(
            applicationContext, resultId(),
            title = if (problem == null) "$gameName score sheet is ready" else "Could not draft the $gameName score sheet",
            text = problem ?: "Drafted from the rulebook. Check it against the rulebook before you rely on it.",
            icon = R.drawable.ic_stat_guide,
            openIntent = openApp()
        )
        return if (problem == null) Result.success() else Result.failure(workDataOf(KEY_ERROR to problem))
    }

    private fun openApp() = BackgroundNotifications.openApp(applicationContext)

    private fun progressId() = PROGRESS_ID_BASE + gameId % 100_000
    private fun resultId() = RESULT_ID_BASE + gameId % 100_000

    companion object {
        const val TAG = "score_sheet_draft"
        const val KEY_GAME_ID = "game_id"
        const val KEY_GAME_NAME = "game_name"
        const val KEY_PDF_PATH = "pdf_path"
        const val KEY_SOURCE_NAME = "source_name"
        const val KEY_ERROR = "error"
        private const val PROGRESS_ID_BASE = 43_000_000
        private const val RESULT_ID_BASE = 44_000_000
    }
}
