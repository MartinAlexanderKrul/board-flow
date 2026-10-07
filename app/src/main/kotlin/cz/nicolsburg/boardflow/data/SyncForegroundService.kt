package cz.nicolsburg.boardflow.data

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import cz.nicolsburg.boardflow.R

/**
 * Keeps the app process running while a sync runs, so switching to another app does not freeze
 * it mid-sync (Android pauses the network of cached background apps). It does no work itself:
 * the sync keeps running in `SyncViewModel`; this only holds a foreground notification until
 * [stop]. [finished] posts the result when the user is not looking at the app.
 */
class SyncForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = this
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Syncing"
        val notification = BackgroundNotifications.progress(
            this,
            title = title,
            text = "Keeps running if you switch apps.",
            icon = R.drawable.ic_stat_sync,
            openIntent = BackgroundNotifications.openApp(this, BackgroundNotifications.ACTION_OPEN_SYNC)
        )
        runCatching {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
            )
        }.onFailure { finish() }
        // A sync that already ended: Android still requires startForeground above, then it can go.
        if (stopRequested) finish()
        // Not restarted by the system: if the process dies, the sync it was guarding is gone too.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (running === this) running = null
        super.onDestroy()
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        // Stopped directly rather than with another startService call, which Android may refuse
        // from the background.
        @Volatile private var running: SyncForegroundService? = null
        @Volatile private var stopRequested = false
        private const val EXTRA_TITLE = "title"
        private const val NOTIFICATION_ID = 43_000_001
        private const val RESULT_ID = 43_000_002

        fun start(context: Context, title: String) {
            stopRequested = false
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, SyncForegroundService::class.java).putExtra(EXTRA_TITLE, title)
                )
            }
        }

        fun stop() {
            stopRequested = true
            running?.finish()
        }

        /** The sync's outcome as a notification, only when the app is not on screen. */
        fun finished(context: Context, title: String, error: String?) {
            if (BackgroundNotifications.isAppVisible()) return
            BackgroundNotifications.result(
                context, RESULT_ID,
                title = if (error == null) "$title finished" else "$title failed",
                text = error ?: "Open BoardFlow to see the sync log.",
                icon = R.drawable.ic_stat_sync,
                openIntent = BackgroundNotifications.openApp(context, BackgroundNotifications.ACTION_OPEN_SYNC)
            )
        }
    }
}
