package cz.nicolsburg.boardflow.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import cz.nicolsburg.boardflow.data.CanonicalCollectionStore
import cz.nicolsburg.boardflow.model.InsightRarity
import cz.nicolsburg.boardflow.ui.history.buildSmartObservations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class DailyInsightGlanceWidget : SessionGlanceWidget() {

    override suspend fun computeSnapshot(
        context: Context,
    ): Pair<SessionGlanceWidget.WidgetSnapshot, List<SessionGlanceWidget.WidgetSnapshot>> {
        return runCatching {
            val plays = withContext(Dispatchers.IO) {
                CanonicalCollectionStore.getInstance(context).getLoggedPlays()
            }
            val observations = plays.buildSmartObservations()
            if (observations.isEmpty()) {
                val fb = fallback()
                return@runCatching fb to listOf(fb)
            }

            val sorted   = observations.sortedByDescending { it.rarity.sortWeight }
            val today    = LocalDate.now().toString()
            val prefs    = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastDay  = prefs.getString(KEY_LAST_DAY, null)
            val lastText = prefs.getString(KEY_LAST_TEXT, null)

            val choice = if (lastDay == today && lastText != null) {
                sorted.firstOrNull { it.text == lastText } ?: sorted.first()
            } else {
                val prevIdx = sorted.indexOfFirst { it.text == lastText }
                val next    = if (prevIdx >= 0) sorted.getOrElse(prevIdx + 1) { sorted.first() }
                              else sorted.first()
                prefs.edit().putString(KEY_LAST_DAY, today).putString(KEY_LAST_TEXT, next.text).apply()
                next
            }

            fun toSnapshot(obs: cz.nicolsburg.boardflow.ui.history.SmartObservation) =
                SessionGlanceWidget.WidgetSnapshot(
                    id          = "obs_${obs.subtext}_${obs.text.hashCode()}",
                    header      = obs.subtext ?: "Daily Insight",
                    primaryText = obs.text,
                    accentColor = obs.rarity.toWidgetColor(),
                )

            toSnapshot(choice) to sorted.map(::toSnapshot)
        }.getOrElse {
            val fb = fallback()
            fb to listOf(fb)
        }
    }

    private fun fallback() = SessionGlanceWidget.WidgetSnapshot(
        header      = "Daily Insight",
        primaryText = "Every collection starts somewhere.",
        accentColor = SessionGlanceWidget.NEUTRAL,
    )

    private fun InsightRarity.toWidgetColor() = when (this) {
        InsightRarity.LEGENDARY, InsightRarity.EPIC -> SessionGlanceWidget.TEAL
        InsightRarity.RARE                          -> SessionGlanceWidget.BLUE
        else                                        -> SessionGlanceWidget.NEUTRAL
    }

    companion object {
        private const val PREFS_NAME    = "DailyInsightWidgetPrefs"
        private const val KEY_LAST_DAY  = "last_day"
        private const val KEY_LAST_TEXT = "last_text"
    }
}

class DailyInsightWidget : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = DailyInsightGlanceWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_ROTATE -> MainScope().launch { DailyInsightGlanceWidget().updateAll(context) }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_ENABLED -> scheduleRotation(context)
        }
    }

    private fun scheduleRotation(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + SessionGlanceWidget.INTERVAL_MS,
            SessionGlanceWidget.INTERVAL_MS,
            PendingIntent.getBroadcast(
                context, 3,
                Intent(context, DailyInsightWidget::class.java).apply { action = ACTION_ROTATE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    companion object {
        const val ACTION_ROTATE = "cz.nicolsburg.boardflow.ACTION_ROTATE_DAILY_INSIGHT"
    }
}
