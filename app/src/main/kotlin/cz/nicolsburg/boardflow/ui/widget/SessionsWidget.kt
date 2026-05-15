package cz.nicolsburg.boardflow.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import cz.nicolsburg.boardflow.MainActivity
import cz.nicolsburg.boardflow.R
import cz.nicolsburg.boardflow.data.CanonicalCollectionStore
import cz.nicolsburg.boardflow.model.LoggedPlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════════════
// WidgetSnapshot — unified content model for both SessionWidget and
// DailyInsightWidget. Layouts are pure templates that render these fields
// without knowing what kind of content they're displaying.
//
//   header       → Cinzel Decorative bitmap at top (e.g. "Last Session")
//   primaryText  → bold main line (compact + expanded title)
//   subtitleText → dimmed secondary line, expanded only (optional)
//   detailText   → below-divider block, expanded only (optional)
//   accentColor  → drives header and accent tints
// ══════════════════════════════════════════════════════════════════════════════

open class SessionGlanceWidget : GlanceAppWidget() {

    companion object {
        val CELL_1X1 = DpSize( 40.dp,  40.dp)
        val CELL_1X2 = DpSize( 40.dp, 110.dp)
        val CELL_1X3 = DpSize( 40.dp, 180.dp)
        val CELL_2X1 = DpSize(110.dp,  40.dp)
        val CELL_2X2 = DpSize(110.dp, 110.dp)
        val CELL_2X3 = DpSize(110.dp, 180.dp)
        val CELL_3X1 = DpSize(180.dp,  40.dp)
        val CELL_3X2 = DpSize(180.dp, 110.dp)
        val CELL_3X3 = DpSize(180.dp, 180.dp)

        const val ACTION_ROTATE     = "cz.nicolsburg.boardflow.ACTION_ROTATE_SESSION"
        const val ACTION_QUICK_SCAN = "cz.nicolsburg.boardflow.ACTION_QUICK_SCAN"
        const val ACTION_OPEN_PLAY  = "cz.nicolsburg.boardflow.ACTION_OPEN_PLAY"
        const val EXTRA_GAME_ID     = "extra_game_id"
        val PLAY_GAME_ID_KEY = ActionParameters.Key<Int>("game_id")
        internal const val INTERVAL_MS = 5 * 60 * 1000L

        internal val NEUTRAL = Color(android.graphics.Color.parseColor("#FEB316"))
        internal val AMBER   = Color(android.graphics.Color.parseColor("#FEB316"))
        internal val BLUE    = Color(android.graphics.Color.parseColor("#7EA7FF"))
        internal val TEAL    = Color(android.graphics.Color.parseColor("#80CBC4"))
    }

    // ── Data model ────────────────────────────────────────────────────────────

    internal data class WidgetSnapshot(
        val id:           String = "default",
        val header:       String,
        val primaryText:  String,
        val subtitleText: String = "",
        val detailText:   String = "",
        val accentColor:  Color,
        val priority:     Int    = 0,
        val gameId:       Int    = 0,
    )

    // ── Size tiers ────────────────────────────────────────────────────────────

    override val sizeMode = SizeMode.Responsive(
        setOf(
            CELL_1X1, CELL_1X2, CELL_1X3,
            CELL_2X1, CELL_2X2, CELL_2X3,
            CELL_3X1, CELL_3X2, CELL_3X3,
        )
    )

    // ── Glance entry point ────────────────────────────────────────────────────

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (snapshot, _) = computeSnapshot(context)

        val fullTitle = "Board Flow – ${snapshot.header}"
        val headerBmp = withContext(Dispatchers.Default) {
            runCatching { renderHeader(context, fullTitle, 17f, snapshot.accentColor) }.getOrNull()
        }
        val (headerSmallBmp, headerLargeBmp) = headerBmp to headerBmp

        provideContent { WidgetRoot(snapshot, headerSmallBmp, headerLargeBmp) }
    }

    // ── Layout dispatcher ─────────────────────────────────────────────────────

    @Composable
    private fun WidgetRoot(snapshot: WidgetSnapshot, headerSmallBmp: Bitmap?, headerLargeBmp: Bitmap?) {
        val size       = LocalSize.current
        val openAction: Action = if (snapshot.gameId != 0) {
            actionRunCallback<OpenPlayCallback>(actionParametersOf(PLAY_GAME_ID_KEY to snapshot.gameId))
        } else {
            actionStartActivity<MainActivity>()
        }
        val scanAction: Action = actionRunCallback<QuickScanCallback>()

        when {
            size.width  <  90.dp                        -> TinyLayout(snapshot, openAction, scanAction)
            size.height <  90.dp                        -> CompactLayout(snapshot, headerSmallBmp, openAction, scanAction)
            size.height >= 140.dp && size.width >= 160.dp -> ExpandedLayout(snapshot, headerLargeBmp, openAction, scanAction)
            else                                        -> SmallLayout(snapshot, headerSmallBmp, openAction, scanAction)
        }
    }

    // ── Tiny (1×1) — camera button only ──────────────────────────────────────

    @Composable
    private fun TinyLayout(snapshot: WidgetSnapshot, openAction: Action, scanAction: Action) {
        Box(
            modifier         = GlanceModifier.fillMaxSize().clickable(openAction),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(44.dp)
                    .background(ImageProvider(R.drawable.widget_background))
                    .clickable(scanAction),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider           = ImageProvider(R.drawable.ic_widget_camera),
                    contentDescription = null,
                    modifier           = GlanceModifier.size(24.dp),
                    contentScale       = ContentScale.Fit,
                )
            }
        }
    }

    // ── Compact (3×1 / short wide) — title bitmap + primary + camera ────────

    @Composable
    private fun CompactLayout(snapshot: WidgetSnapshot, headerBmp: Bitmap?, openAction: Action, scanAction: Action) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .clickable(openAction)
                .padding(start = 10.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.Start) {
                    if (headerBmp != null) {
                        Image(
                            provider           = ImageProvider(headerBmp),
                            contentDescription = snapshot.header,
                            modifier           = GlanceModifier.height(18.dp),
                            contentScale       = ContentScale.Fit,
                        )
                        Spacer(GlanceModifier.height(4.dp))
                    }
                    Text(
                        text     = snapshot.primaryText,
                        maxLines = 1,
                        style    = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    )
                }
                Spacer(GlanceModifier.width(6.dp))
                Image(
                    provider           = ImageProvider(R.drawable.ic_widget_camera),
                    contentDescription = null,
                    modifier           = GlanceModifier.size(30.dp).clickable(scanAction),
                    contentScale       = ContentScale.Fit,
                )
            }
        }
    }

    // ── Small (2×2) — Board Flow + header + primary + subtitle + camera ──────

    @Composable
    private fun SmallLayout(snapshot: WidgetSnapshot, headerBmp: Bitmap?, openAction: Action, scanAction: Action) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .clickable(openAction)
                .padding(start = 10.dp, end = 8.dp, top = 10.dp, bottom = 4.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.Start) {
                    if (headerBmp != null) {
                        Image(
                            provider           = ImageProvider(headerBmp),
                            contentDescription = snapshot.header,
                            modifier           = GlanceModifier.height(22.dp),
                            contentScale       = ContentScale.Fit,
                        )
                    }
                    Spacer(GlanceModifier.height(10.dp))
                    Text(
                        text     = snapshot.primaryText,
                        maxLines = 2,
                        style    = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    )
                    if (snapshot.subtitleText.isNotBlank()) {
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text     = snapshot.subtitleText,
                            maxLines = 1,
                            style    = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.65f)), fontSize = 10.sp),
                        )
                    }
                }
                Spacer(GlanceModifier.width(6.dp))
                Box(
                    modifier         = GlanceModifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider           = ImageProvider(R.drawable.ic_widget_camera),
                        contentDescription = null,
                        modifier           = GlanceModifier.size(38.dp).clickable(scanAction),
                        contentScale       = ContentScale.Fit,
                    )
                }
            }
        }
    }

    // ── Expanded (3×2+) — Board Flow + header + primary + subtitle + detail ──

    @Composable
    private fun ExpandedLayout(snapshot: WidgetSnapshot, headerBmp: Bitmap?, openAction: Action, scanAction: Action) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .clickable(openAction)
                .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 8.dp),
        ) {
            Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.Start) {
                    if (headerBmp != null) {
                        Image(
                            provider           = ImageProvider(headerBmp),
                            contentDescription = snapshot.header,
                            modifier           = GlanceModifier.height(22.dp),
                            contentScale       = ContentScale.Fit,
                        )
                    }

                    Spacer(GlanceModifier.height(12.dp))

                    Text(
                        text     = snapshot.primaryText,
                        maxLines = 2,
                        style    = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    )

                    if (snapshot.subtitleText.isNotBlank()) {
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text     = snapshot.subtitleText,
                            maxLines = 1,
                            style    = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.65f)), fontSize = 12.sp),
                        )
                    }

                    if (snapshot.detailText.isNotBlank()) {
                        Spacer(GlanceModifier.height(6.dp))
                        Spacer(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(ColorProvider(Color.White.copy(alpha = 0.20f)))
                        )
                        Spacer(GlanceModifier.height(6.dp))
                        Text(
                            text     = snapshot.detailText,
                            maxLines = 6,
                            style    = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.80f)), fontSize = 11.sp),
                        )
                    }
                }

                Spacer(GlanceModifier.width(6.dp))
                Box(
                    modifier         = GlanceModifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider           = ImageProvider(R.drawable.ic_widget_camera),
                        contentDescription = null,
                        modifier           = GlanceModifier.size(46.dp).clickable(scanAction),
                        contentScale       = ContentScale.Fit,
                    )
                }
            }
        }
    }

    // ── Snapshot computation ──────────────────────────────────────────────────

    internal open suspend fun computeSnapshot(context: Context): Pair<WidgetSnapshot, List<WidgetSnapshot>> {
        return runCatching {
            val plays      = withContext(Dispatchers.IO) { CanonicalCollectionStore.getInstance(context).getLoggedPlays() }
            val datedPlays = plays.mapNotNull { play -> parseDate(play)?.let { it to play } }
            val snapshot   = lastSession(datedPlays, LocalDate.now())
                ?: WidgetSnapshot(header = "Last Session", primaryText = "No plays logged yet.", accentColor = NEUTRAL)
            snapshot to listOf(snapshot)
        }.getOrElse {
            val fb = WidgetSnapshot(header = "Last Session", primaryText = "Every collection starts somewhere.", accentColor = NEUTRAL)
            fb to listOf(fb)
        }
    }

    private fun lastSession(datedPlays: List<Pair<LocalDate, LoggedPlay>>, today: LocalDate): WidgetSnapshot? {
        val (date, play) = datedPlays.maxByOrNull { it.first } ?: return null
        val dayStr   = dayLabel(date, today)
        val players  = play.players.filter { it.name.isNotBlank() }
        val winner   = players.firstOrNull { it.isWinner }?.name?.trim()

        val subtitleText = when {
            players.isEmpty() -> ""
            winner != null    -> "${players.size} player${if (players.size == 1) "" else "s"}  ·  $winner won"
            else              -> "${players.size} player${if (players.size == 1) "" else "s"}"
        }
        val detailText = players.joinToString("\n") { p ->
            buildString {
                append(p.name.trim())
                if (p.score.isNotBlank()) append("   ${p.score}")
                if (p.isWinner) append("  ★")
            }
        }

        return WidgetSnapshot(
            id           = "last_${play.id}",
            header       = "Last Session",
            primaryText  = "${play.gameName}  ·  $dayStr",
            subtitleText = subtitleText,
            detailText   = detailText,
            accentColor  = NEUTRAL,
            priority     = 80,
            gameId       = play.gameId,
        )
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    internal fun dayLabel(date: LocalDate?, today: LocalDate): String = when (date) {
        today              -> "Today"
        today.minusDays(1) -> "Yesterday"
        else               -> date?.dayOfWeek?.getDisplayName(JTextStyle.FULL, Locale.getDefault()) ?: ""
    }

    internal fun parseDate(play: LoggedPlay): LocalDate? =
        runCatching { LocalDate.parse(play.date) }.getOrNull()

    internal fun renderHeader(context: Context, text: String, textSizeSp: Float, color: Color): Bitmap {
        val density  = context.resources.displayMetrics.density
        val typeface = ResourcesCompat.getFont(context, R.font.cinzel_decorative_bold)
        val argb     = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red   * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue  * 255).toInt(),
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize      = textSizeSp * density
            this.color    = argb
            textAlign     = Paint.Align.LEFT
        }
        val fm  = paint.fontMetrics
        val w   = paint.measureText(text).toInt().coerceAtLeast(1)
        val h   = (fm.descent - fm.ascent).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawText(text, 0f, -fm.ascent, paint)
        return bmp
    }
}

// ── QuickScanCallback ─────────────────────────────────────────────────────────

class QuickScanCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                action = SessionGlanceWidget.ACTION_QUICK_SCAN
                flags  = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
    }
}

// ── OpenPlayCallback ──────────────────────────────────────────────────────────

class OpenPlayCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val gameId = parameters[SessionGlanceWidget.PLAY_GAME_ID_KEY] ?: 0
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                action = SessionGlanceWidget.ACTION_OPEN_PLAY
                putExtra(SessionGlanceWidget.EXTRA_GAME_ID, gameId)
                flags  = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
    }
}

// ── SessionWidget — receiver ──────────────────────────────────────────────────

class SessionWidget : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = SessionGlanceWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            SessionGlanceWidget.ACTION_ROTATE -> MainScope().launch { SessionGlanceWidget().updateAll(context) }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_ENABLED -> scheduleRefresh(context)
        }
    }

    private fun scheduleRefresh(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + SessionGlanceWidget.INTERVAL_MS,
            SessionGlanceWidget.INTERVAL_MS,
            PendingIntent.getBroadcast(
                context, 2,
                Intent(context, SessionWidget::class.java).apply { action = SessionGlanceWidget.ACTION_ROTATE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }
}
