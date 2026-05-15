package cz.nicolsburg.boardflow.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import cz.nicolsburg.boardflow.MainActivity
import cz.nicolsburg.boardflow.R

class LauncherWidget : AppWidgetProvider() {

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
            action = QuickScanWidget.ACTION_QUICK_SCAN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val views = RemoteViews(context.packageName, R.layout.widget_launcher)
        views.setOnClickPendingIntent(R.id.widget_launcher_root, pendingIntent)
        views.setImageViewBitmap(
            R.id.widget_launcher_label,
            renderTextBitmap(context, context.getString(R.string.widget_launcher_label))
        )
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun renderTextBitmap(context: Context, text: String): Bitmap {
        val density = context.resources.displayMetrics.density
        val typeface = ResourcesCompat.getFont(context, R.font.cinzel_decorative_bold)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 12f * density
            color = Color.WHITE
            textAlign = Paint.Align.LEFT
        }
        val fm = paint.fontMetrics
        val textWidth = paint.measureText(text)
        val textHeight = fm.descent - fm.ascent
        val bitmap = Bitmap.createBitmap(
            textWidth.toInt().coerceAtLeast(1),
            textHeight.toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        Canvas(bitmap).drawText(text, 0f, -fm.ascent, paint)
        return bitmap
    }
}
