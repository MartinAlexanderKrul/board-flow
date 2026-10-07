package cz.nicolsburg.boardflow.data.setupguide

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
 * Drafts a guide from a staged rulebook PDF while the app may be closed. Runs as a foreground
 * job with a quiet "Drafting a guide" notification, then posts "guide is ready" (tapping it
 * opens Quick Setup for the game) or what went wrong. Uses the shared [AppContainer], so an
 * open Quick Setup screen shows the guide as soon as it is saved.
 */
class GuideDraftWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val gameId get() = inputData.getInt(KEY_GAME_ID, 0)
    private val gameName get() = inputData.getString(KEY_GAME_NAME).orEmpty()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = BackgroundNotifications.progress(
            applicationContext,
            title = "Drafting a guide for $gameName",
            text = "Gemini is reading the rulebook. This can take a few minutes.",
            icon = R.drawable.ic_stat_guide,
            openIntent = openQuickSetup()
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
            AppContainer.get(applicationContext).guideDraftService
                .draftFromFile(pdf, inputData.getString(KEY_SOURCE_NAME), gameId, gameName)
        } finally {
            pdf.delete()
        }

        if (problem == null) {
            BackgroundNotifications.result(
                applicationContext, resultId(),
                title = "$gameName guide is ready",
                text = "Drafted from the rulebook. Check it against the rulebook before you rely on it.",
                icon = R.drawable.ic_stat_guide,
                openIntent = openQuickSetup()
            )
            return Result.success()
        }
        BackgroundNotifications.result(
            applicationContext, resultId(),
            title = "Could not draft the $gameName guide",
            text = problem,
            icon = R.drawable.ic_stat_guide,
            openIntent = openQuickSetup()
        )
        return Result.failure(workDataOf(KEY_ERROR to problem))
    }

    private fun openQuickSetup() =
        BackgroundNotifications.openApp(applicationContext, BackgroundNotifications.ACTION_OPEN_QUICK_SETUP).apply {
            putExtra(BackgroundNotifications.EXTRA_GAME_ID, gameId)
            putExtra(BackgroundNotifications.EXTRA_GAME_NAME, gameName)
        }

    private fun progressId() = PROGRESS_ID_BASE + gameId % 100_000
    private fun resultId() = RESULT_ID_BASE + gameId % 100_000

    companion object {
        const val TAG = "guide_draft"
        const val KEY_GAME_ID = "game_id"
        const val KEY_GAME_NAME = "game_name"
        const val KEY_PDF_PATH = "pdf_path"
        const val KEY_SOURCE_NAME = "source_name"
        const val KEY_ERROR = "error"
        private const val PROGRESS_ID_BASE = 41_000_000
        private const val RESULT_ID_BASE = 42_000_000
    }
}
