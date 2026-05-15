package cz.nicolsburg.boardflow.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import cz.nicolsburg.boardflow.R
import cz.nicolsburg.boardflow.data.CanonicalCollectionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class InsightsWidget : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        scheduleRotation(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val insights = computeInsights(context)
        val index = currentIndex(context, insights.size)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, insights, index)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ROTATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, InsightsWidget::class.java))
            if (ids.isEmpty()) return
            val insights = computeInsights(context)
            advanceIndex(context, insights.size)
            val index = currentIndex(context, insights.size)
            for (id in ids) {
                updateWidget(context, appWidgetManager, id, insights, index)
            }
        }
    }

    override fun onDisabled(context: Context) {
        cancelRotation(context)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        insights: List<String>,
        index: Int
    ) {
        val insightText = insights.getOrElse(index) { "No plays this week" }
        val density = context.resources.displayMetrics.density
        val maxWidthPx = (INSIGHT_MAX_WIDTH_DP * density).toInt()

        val views = RemoteViews(context.packageName, R.layout.widget_insights)

        views.setOnClickPendingIntent(
            R.id.widget_insights_root,
            PendingIntent.getBroadcast(
                context, 0,
                Intent(context, InsightsWidget::class.java).apply { action = ACTION_ROTATE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        runCatching {
            views.setImageViewBitmap(
                R.id.widget_insights_header,
                renderSingleLine(context, "THIS WEEK", 9f, Color.parseColor("#FEB316"))
            )
            views.setImageViewBitmap(
                R.id.widget_insights_text,
                renderWrapped(context, insightText, 11f, Color.WHITE, maxWidthPx)
            )
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun computeInsights(context: Context): List<String> {
        return runCatching {
            val plays = runBlocking(Dispatchers.IO) {
                CanonicalCollectionStore.getInstance(context).getLoggedPlays()
            }
            val weekAgo = LocalDate.now().minusDays(7)
            val weekPlays = plays.filter { play ->
                runCatching { LocalDate.parse(play.date) >= weekAgo }.getOrDefault(false)
            }
            if (weekPlays.isEmpty()) return listOf("No plays this week")

            val insights = mutableListOf<String>()

            val totalPlays = weekPlays.sumOf { it.quantity.coerceAtLeast(1) }
            val uniqueGames = weekPlays.map { it.gameName }.distinct().size
            insights.add("$totalPlays plays across $uniqueGames game${if (uniqueGames != 1) "s" else ""}")

            weekPlays.groupBy { it.gameName }
                .mapValues { (_, p) -> p.sumOf { it.quantity.coerceAtLeast(1) } }
                .maxByOrNull { it.value }
                ?.let { insights.add("Most played: ${it.key} ×${it.value}") }

            weekPlays.flatMap { it.players }
                .groupBy { it.name }
                .mapValues { it.value.size }
                .maxByOrNull { it.value }
                ?.let { insights.add("Most active: ${it.key} (${it.value} game${if (it.value != 1) "s" else ""})") }

            val totalMinutes = weekPlays.filter { it.durationMinutes > 0 }.sumOf { it.durationMinutes }
            if (totalMinutes > 0) {
                val h = totalMinutes / 60
                val m = totalMinutes % 60
                insights.add("Time at table: ${if (h > 0) "${h}h ${m}m" else "${m}m"}")
            }

            weekPlays.flatMap { it.players }
                .filter { it.isWinner }
                .groupBy { it.name }
                .mapValues { it.value.size }
                .maxByOrNull { it.value }
                ?.takeIf { it.value > 0 }
                ?.let { insights.add("Top winner: ${it.key} (${it.value} win${if (it.value != 1) "s" else ""})") }

            insights
        }.getOrElse { listOf("No plays this week") }
    }

    private fun currentIndex(context: Context, size: Int): Int {
        if (size == 0) return 0
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_INDEX, 0) % size
    }

    private fun advanceIndex(context: Context, size: Int) {
        if (size == 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_INDEX, (prefs.getInt(KEY_INDEX, 0) + 1) % size).apply()
    }

    private fun renderSingleLine(context: Context, text: String, textSizeSp: Float, color: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val typeface = ResourcesCompat.getFont(context, R.font.cinzel_decorative_bold)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = textSizeSp * density
            this.color = color
            textAlign = Paint.Align.LEFT
        }
        val fm = paint.fontMetrics
        val w = paint.measureText(text).toInt().coerceAtLeast(1)
        val h = (fm.descent - fm.ascent).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawText(text, 0f, -fm.ascent, paint)
        return bitmap
    }

    private fun renderWrapped(context: Context, text: String, textSizeSp: Float, color: Int, maxWidthPx: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val typeface = ResourcesCompat.getFont(context, R.font.cinzel_decorative_bold)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = textSizeSp * density
            this.color = color
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidthPx)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.15f)
            .setMaxLines(3)
            .build()
        val bitmap = Bitmap.createBitmap(
            layout.width.coerceAtLeast(1),
            layout.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        layout.draw(Canvas(bitmap))
        return bitmap
    }

    private fun scheduleRotation(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + INTERVAL_MS,
            INTERVAL_MS,
            rotationPendingIntent(context)
        )
    }

    private fun cancelRotation(context: Context) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .cancel(rotationPendingIntent(context))
    }

    private fun rotationPendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, InsightsWidget::class.java).apply { action = ACTION_ROTATE },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        private const val ACTION_ROTATE = "cz.nicolsburg.boardflow.ACTION_ROTATE_INSIGHT"
        private const val PREFS_NAME = "InsightsWidgetPrefs"
        private const val KEY_INDEX = "insight_index"
        private const val INTERVAL_MS = 5 * 60 * 1000L
        private const val INSIGHT_MAX_WIDTH_DP = 175f
    }
}
