package cz.nicolsburg.boardflow.ui.common

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import cz.nicolsburg.boardflow.data.BackgroundNotifications

/**
 * Returns a function that asks for the notification permission (Android 13+) when it is not
 * granted yet. Called when the user starts work that reports back by notification (a guide
 * draft, a sync), so the question comes with its reason. Android stops showing the dialog by
 * itself after the user declines twice.
 */
@Composable
fun rememberNotificationPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !BackgroundNotifications.canNotify(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
