package cz.nicolsburg.boardflow.data

import android.Manifest
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cz.nicolsburg.boardflow.MainActivity

/**
 * Notifications for work that keeps running when the user leaves the app: a quiet ongoing one
 * while it runs (it also keeps the process alive, as a foreground service), and a result when
 * it is done. Used by the guide draft worker and by sync.
 */
object BackgroundNotifications {
    const val CHANNEL_PROGRESS = "background_work"
    const val CHANNEL_RESULTS = "background_results"

    const val ACTION_OPEN_QUICK_SETUP = "cz.nicolsburg.boardflow.ACTION_OPEN_QUICK_SETUP"
    const val ACTION_OPEN_SYNC = "cz.nicolsburg.boardflow.ACTION_OPEN_SYNC"
    const val EXTRA_GAME_ID = "game_id"
    const val EXTRA_GAME_NAME = "game_name"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, "Running in the background", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Shown while a sync or a guide draft is running" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_RESULTS, "Finished in the background", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "A sync or a guide draft is done" }
        )
    }

    fun progress(context: Context, title: String, text: String, @DrawableRes icon: Int, openIntent: Intent): Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(pendingIntent(context, 0, openIntent))
            .build()
    }

    /** Posts a result when notifications are allowed; returns false when they are not. */
    fun result(context: Context, id: Int, title: String, text: String, @DrawableRes icon: Int, openIntent: Intent): Boolean {
        if (!canNotify(context)) return false
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_RESULTS)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent(context, id, openIntent))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
        return true
    }

    fun openApp(context: Context, action: String? = null): Intent =
        Intent(context, MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** True while the user is looking at the app; a result notification is then not needed. */
    fun isAppVisible(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private fun pendingIntent(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
