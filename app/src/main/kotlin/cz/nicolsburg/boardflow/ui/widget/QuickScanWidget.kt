package cz.nicolsburg.boardflow.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import cz.nicolsburg.boardflow.MainActivity
import cz.nicolsburg.boardflow.R

class QuickScanWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_QUICK_SCAN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val views = RemoteViews(context.packageName, R.layout.widget_quick_scan)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val ACTION_QUICK_SCAN = "cz.nicolsburg.boardflow.ACTION_QUICK_SCAN"
    }
}
