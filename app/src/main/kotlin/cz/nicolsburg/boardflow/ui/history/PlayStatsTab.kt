package cz.nicolsburg.boardflow.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.ui.common.BoardFlowMotion
import cz.nicolsburg.boardflow.ui.common.SectionCard
import cz.nicolsburg.boardflow.ui.common.boardFlowTween
import cz.nicolsburg.boardflow.ui.common.withTabularNumbers
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// ── Private data holders ──────────────────────────────────────────────────────

private data class ActivityBucket(
    val label: String,
    val peakLabel: String,
    val count: Int,
    val highlight: Boolean = false
)

private data class GameStat(val gameId: Int, val name: String, val plays: Int)
private data class PlayerStat(val displayName: String, val plays: Int, val wins: Int)
private data class StatsInsight(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val detail: String? = null,
    val gameFilter: Pair<Int, String>? = null,
    val playerFilter: String? = null
)

// ── Pure computations ─────────────────────────────────────────────────────────

private fun buildActivityBuckets(plays: List<LoggedPlay>, range: StatsTimeRange): List<ActivityBucket> {
    val now = LocalDate.now()

    fun dailyBuckets(start: LocalDate, end: LocalDate): List<ActivityBucket> {
        val byDay = plays
            .mapNotNull { play ->
                runCatching { LocalDate.parse(play.date) }.getOrNull()
                    ?.let { day -> day to play.quantity.coerceAtLeast(1) }
            }
            .groupBy({ it.first }) { it.second }
            .mapValues { it.value.sum() }
        val days = (end.toEpochDay() - start.toEpochDay()).coerceAtLeast(0).toInt()
        return (0..days).map { offset ->
            val day = start.plusDays(offset.toLong())
            ActivityBucket(
                label = day.dayOfMonth.toString(),
                peakLabel = day.format(DateTimeFormatter.ofPattern("MMM d")),
                count = byDay[day] ?: 0,
                highlight = day == now
            )
        }
    }

    when (range) {
        StatsTimeRange.THIS_MONTH -> {
            val start = LocalDate.of(now.year, now.monthValue, 1)
            return dailyBuckets(start, now)
        }
        StatsTimeRange.LAST_30 -> return dailyBuckets(now.minusDays(29), now)
        else -> Unit
    }

    val byMonth = plays
        .mapNotNull { play ->
            runCatching { LocalDate.parse(play.date) }.getOrNull()
                ?.let { d -> (d.year * 100 + d.monthValue) to play.quantity.coerceAtLeast(1) }
        }
        .groupBy({ it.first }) { it.second }
        .mapValues { it.value.sum() }
    val currentYM = now.year * 100 + now.monthValue

    return when (range) {
        StatsTimeRange.THIS_YEAR -> {
            (1..now.monthValue).map { m ->
                val d = LocalDate.of(now.year, m, 1)
                val ym = d.year * 100 + d.monthValue
                ActivityBucket(
                    label = d.format(DateTimeFormatter.ofPattern("MMM")),
                    peakLabel = d.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                    count = byMonth[ym] ?: 0,
                    highlight = ym == currentYM
                )
            }
        }
        StatsTimeRange.ALL -> {
            val earliestYM = byMonth.keys.minOrNull() ?: currentYM
            val earliestYear = earliestYM / 100
            val earliestMonth = earliestYM % 100
            val monthSpan = (now.year - earliestYear) * 12 + (now.monthValue - earliestMonth) + 1
            if (monthSpan > 18) {
                (earliestYear..now.year).map { year ->
                    val yearCount = byMonth.entries.filter { it.key / 100 == year }.sumOf { it.value }
                    ActivityBucket(
                        label = year.toString(),
                        peakLabel = year.toString(),
                        count = yearCount,
                        highlight = year == now.year
                    )
                }
            } else {
                val start = LocalDate.of(earliestYear, earliestMonth, 1)
                (0 until monthSpan).map { m ->
                    val d = start.plusMonths(m.toLong())
                    val ym = d.year * 100 + d.monthValue
                    ActivityBucket(
                        label = d.format(DateTimeFormatter.ofPattern("MMM ''yy")),
                        peakLabel = d.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                        count = byMonth[ym] ?: 0,
                        highlight = ym == currentYM
                    )
                }
            }
        }
        else -> emptyList()
    }
}

private fun buildTopGames(plays: List<LoggedPlay>, limit: Int = 10): List<GameStat> =
    plays.filter { it.gameName.isNotBlank() }
        .groupBy { it.gameId }
        .mapValues { (_, g) ->
            val name = g.maxByOrNull { it.date }?.gameName ?: g.first().gameName
            name to g.sumOf { it.quantity.coerceAtLeast(1) }
        }
        .entries.sortedByDescending { it.value.second }.take(limit)
        .map { (gameId, pair) -> GameStat(gameId, pair.first, pair.second) }

private fun buildTopPlayers(plays: List<LoggedPlay>, roster: List<Player>, limit: Int = 8): List<PlayerStat> {
    fun resolveDisplayName(raw: String): String {
        val lower = raw.lowercase().trim()
        return roster.firstOrNull { p ->
            p.displayName.lowercase().trim() == lower ||
                p.aliases.any { a -> a.lowercase().trim() == lower }
        }?.displayName ?: "Unknown"
    }
    val map = mutableMapOf<String, Pair<Int, Int>>()
    plays.forEach { play ->
        play.players.filter { it.name.isNotBlank() }.forEach { pr ->
            val name = resolveDisplayName(pr.name)
            val (p, w) = map[name] ?: (0 to 0)
            map[name] = (p + 1) to (if (pr.isWinner) w + 1 else w)
        }
    }
    return map.entries.sortedByDescending { it.value.first }.take(limit)
        .map { (name, pw) -> PlayerStat(name, pw.first, pw.second) }
}

private fun computeHIndex(plays: List<LoggedPlay>): Int {
    val counts = plays.groupBy { it.gameName }
        .mapValues { (_, g) -> g.sumOf { it.quantity.coerceAtLeast(1) } }
        .values.sortedDescending()
    var h = 0
    counts.forEachIndexed { i, c -> if (c >= i + 1) h = i + 1 }
    return h
}

private fun computeStreaks(plays: List<LoggedPlay>): Pair<Int, Int> {
    val dates = plays.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSortedSet()
    if (dates.isEmpty()) return 0 to 0
    val sorted = dates.toList()
    var best = 1; var run = 1
    for (i in 1 until sorted.size) {
        run = if (sorted[i] == sorted[i - 1].plusDays(1)) run + 1 else 1
        if (run > best) best = run
    }
    val today = LocalDate.now()
    val current = if (sorted.last() >= today.minusDays(1)) {
        var s = 1; var d = sorted.last().minusDays(1)
        while (dates.contains(d)) { s++; d = d.minusDays(1) }
        s
    } else 0
    return current to best
}

private fun buildDayOfWeekDist(plays: List<LoggedPlay>): List<Pair<String, Int>> {
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val counts = IntArray(7)
    plays.forEach { play ->
        runCatching {
            val dow = LocalDate.parse(play.date).dayOfWeek.value - 1
            counts[dow] += play.quantity.coerceAtLeast(1)
        }
    }
    return labels.zip(counts.toList())
}

private fun formatOneDecimal(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

private fun countRecentPlays(plays: List<LoggedPlay>, days: Long): Int {
    val cutoff = LocalDate.now().minusDays(days - 1)
    return plays.sumOf { play ->
        val date = runCatching { LocalDate.parse(play.date) }.getOrNull()
        if (date != null && !date.isBefore(cutoff)) play.quantity.coerceAtLeast(1) else 0
    }
}

private fun buildInsights(
    totalPlays: Int,
    uniqueGames: Int,
    totalMinutes: Int,
    plays: List<LoggedPlay>,
    topGames: List<GameStat>,
    topPlayers: List<PlayerStat>,
    dayDist: List<Pair<String, Int>>,
    hIndex: Int,
    currentStreak: Int,
    bestStreak: Int,
    avgDuration: Int?,
    longestSession: Pair<String, Int>?,
    hotPlayerStreak: Pair<String, Int>?,
    mostThisMonth: Pair<String, Int>?
): List<StatsInsight> {
    val activeDays = plays.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet().size
    val avgPlayers = plays.takeIf { it.isNotEmpty() }
        ?.let { list -> list.sumOf { it.players.count { p -> p.name.isNotBlank() } }.toDouble() / list.size }
    val depth = if (uniqueGames > 0) totalPlays.toDouble() / uniqueGames else null
    val recent7 = countRecentPlays(plays, 7)
    val busiestDay = dayDist.maxByOrNull { it.second }?.takeIf { it.second > 0 }
    val completeRate = plays.takeIf { it.isNotEmpty() }
        ?.let { list -> (list.count { !it.incomplete } * 100.0 / list.size).roundToInt() }

    return buildList {
        add(StatsInsight(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            value = "H-$hIndex",
            label = "H-index",
            detail = if (hIndex > 0) "Played $hIndex games at least $hIndex times" else "Grow this by replaying favorites"
        ))
        if (bestStreak > 1) add(StatsInsight(
            icon = Icons.Default.LocalFireDepartment,
            value = "${bestStreak}d",
            label = "Best streak",
            detail = if (currentStreak > 1) "Current: ${currentStreak}d" else null
        ))
        if (currentStreak > 1) add(StatsInsight(Icons.Default.History, "${currentStreak}d", "Current streak", "You're on a roll"))
        avgDuration?.let { avg -> add(StatsInsight(Icons.Default.Schedule, formatDuration(avg), "Avg duration")) }
        longestSession?.let { (name, minutes) ->
            val gameId = plays.firstOrNull { it.gameName == name }?.gameId
            add(StatsInsight(Icons.Default.EmojiEvents, formatDuration(minutes), "Longest session", name, gameFilter = gameId?.let { it to name }))
        }
        topGames.firstOrNull()?.let { game ->
            add(StatsInsight(Icons.Default.Star, "${game.plays}", "Most played", game.name, gameFilter = game.gameId to game.name))
        }
        topPlayers.firstOrNull()?.let { player ->
            add(StatsInsight(Icons.Default.Group, "${player.plays}", "Most active", player.displayName, playerFilter = player.displayName))
        }
        hotPlayerStreak?.let { (name, streak) ->
            add(StatsInsight(Icons.Default.LocalFireDepartment, "${streak}W", "Hot streak", name, playerFilter = name))
        }
        busiestDay?.let { (day, count) ->
            add(StatsInsight(Icons.Default.History, day, "Busiest day", "$count ${if (count == 1) "play" else "plays"}"))
        }
        depth?.let { add(StatsInsight(Icons.Default.Star, formatOneDecimal(it), "Depth", "plays per game")) }
        avgPlayers?.let { add(StatsInsight(Icons.Default.Group, formatOneDecimal(it), "Table size", "players per play")) }
        if (recent7 > 0) add(StatsInsight(Icons.Default.LocalFireDepartment, "$recent7", "Last 7 days", "$recent7 ${if (recent7 == 1) "play" else "plays"}"))
        if (activeDays > 0) add(StatsInsight(Icons.Default.History, "$activeDays", "Active days", "days with plays"))
        if (totalMinutes > 0 && activeDays > 0) add(StatsInsight(Icons.Default.Schedule, formatDuration(totalMinutes / activeDays), "Time per active day"))
        completeRate?.let { add(StatsInsight(Icons.Default.EmojiEvents, "$it%", "Complete plays")) }
        mostThisMonth?.let { (name, count) ->
            val gameId = plays.firstOrNull { it.gameName == name }?.gameId
            add(StatsInsight(Icons.Default.Star, "${count}x", "This month", name, gameFilter = gameId?.let { it to name }))
        }
    }
}

// ── Main composable ───────────────────────────────────────────────────────────

@Composable
internal fun StatsContent(
    plays: List<LoggedPlay>,
    players: List<Player>,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
    onGameTapped: (gameId: Int, gameName: String) -> Unit = { _, _ -> },
    onPlayerTapped: (String) -> Unit = {}
) {
    var timeRange by remember { mutableStateOf(StatsTimeRange.ALL) }
    val statPlays = remember(plays, timeRange) { plays.filterByTimeRange(timeRange) }

    if (plays.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
                Text(
                    "No stats yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Log some plays to see your story here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // ── Derived stats ──────────────────────────────────────────────────────────
    val totalPlays    = remember(statPlays) { statPlays.sumOf { it.quantity.coerceAtLeast(1) } }
    val uniqueGames   = remember(statPlays) { statPlays.map { it.gameName }.toSet().size }
    val totalMinutes  = remember(statPlays) { statPlays.filter { it.durationMinutes > 0 }.sumOf { it.durationMinutes } }
    val activity      = remember(statPlays, timeRange) { buildActivityBuckets(statPlays, timeRange) }
    val topGames      = remember(statPlays) { buildTopGames(statPlays) }
    val topPlayers    = remember(statPlays, players) { buildTopPlayers(statPlays, players) }
    val hIndex        = remember(statPlays) { computeHIndex(statPlays) }
    val (curStreak, bestStreak) = remember(statPlays) { computeStreaks(statPlays) }
    val dayDist       = remember(statPlays) { buildDayOfWeekDist(statPlays) }
    val avgDuration   = remember(statPlays) {
        val d = statPlays.filter { it.durationMinutes > 0 }
        if (d.isEmpty()) null else d.sumOf { it.durationMinutes } / d.size
    }
    val longestSession = remember(statPlays) {
        statPlays.filter { it.durationMinutes > 0 }.maxByOrNull { it.durationMinutes }
            ?.let { it.gameName to it.durationMinutes }
    }
    val hotStreak     = remember(statPlays, players) { statPlays.hotPlayerStreak(players) }
    val mostThisMonth = remember(statPlays) {
        if (timeRange == StatsTimeRange.ALL) statPlays.mostPlayedThisMonth() else null
    }
    val insights      = remember(totalPlays, uniqueGames, totalMinutes, statPlays, topGames, topPlayers, dayDist, hIndex, curStreak, bestStreak, avgDuration, longestSession, hotStreak, mostThisMonth) {
        buildInsights(totalPlays, uniqueGames, totalMinutes, statPlays, topGames, topPlayers, dayDist, hIndex, curStreak, bestStreak, avgDuration, longestSession, hotStreak, mostThisMonth)
    }

    // ── New narrative computations ─────────────────────────────────────────────
    val observations   = remember(plays) { plays.buildSmartObservations() }
    val headerText     = remember(statPlays, timeRange, curStreak) { statPlays.buildContextualHeader(timeRange, curStreak) }
    val heatmapData    = remember(plays) { plays.buildHeatmapData() }
    val onThisDay      = remember(plays) { plays.buildOnThisDay() }
    val archetype      = remember(plays) { if (timeRange == StatsTimeRange.ALL) plays.computeGamerArchetype() else null }
    val rivalryPairs   = remember(statPlays) { statPlays.buildTopRivalryPairs() }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Time range filter ──────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsTimeRange.entries.forEach { range ->
                    FilterChip(
                        selected = timeRange == range,
                        onClick = { timeRange = range },
                        label = { Text(range.label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        }

        if (statPlays.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Text(
                            "No plays in this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Try a different time range",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {

            // ── Contextual narrative header ────────────────────────────────────
            item { NarrativeHeader(headerText, curStreak) }

            // ── Hero rotating observation ──────────────────────────────────────
            if (observations.isNotEmpty() && timeRange == StatsTimeRange.ALL) {
                item { HeroObservationCard(observations) }
            }

            // ── Summary ────────────────────────────────────────────────────────
            item { SummarySection(totalPlays, uniqueGames, totalMinutes, players.size) }

            // ── 52-week heatmap (all-time only) ───────────────────────────────
            if (timeRange == StatsTimeRange.ALL) {
                item { HeatmapSection(heatmapData) }
            }

            // ── Activity chart (filtered ranges) ──────────────────────────────
            if (timeRange != StatsTimeRange.ALL && activity.any { it.count > 0 }) {
                item { ActivitySection(activity, rangeLabel = timeRange.subtitle) }
            }

            // ── Top games ──────────────────────────────────────────────────────
            if (topGames.isNotEmpty()) {
                item { TopGamesSection(topGames, rangeLabel = timeRange.subtitle, onGameTapped = onGameTapped) }
            }

            // ── Rivalries ─────────────────────────────────────────────────────
            if (rivalryPairs.isNotEmpty()) {
                item { RivalryPairsSection(rivalryPairs) }
            }

            // ── Day of week ────────────────────────────────────────────────────
            if (dayDist.any { it.second > 0 }) {
                item { DayOfWeekSection(dayDist, rangeLabel = timeRange.subtitle) }
            }

            // ── Top players ────────────────────────────────────────────────────
            if (topPlayers.isNotEmpty()) {
                item { TopPlayersSection(topPlayers, rangeLabel = timeRange.subtitle, onPlayerTapped = onPlayerTapped) }
            }

            // ── On This Day ────────────────────────────────────────────────────
            if (onThisDay.isNotEmpty() && timeRange == StatsTimeRange.ALL) {
                item { OnThisDaySection(onThisDay, onGameTapped) }
            }

            // ── Archetype ─────────────────────────────────────────────────────
            if (archetype != null) {
                item { ArchetypeCard(archetype) }
            }

            // ── Insights grid ─────────────────────────────────────────────────
            item {
                InsightsSection(
                    insights = insights,
                    rangeLabel = timeRange.subtitle,
                    onGameTapped = onGameTapped,
                    onPlayerTapped = onPlayerTapped
                )
            }

        } // end else (statPlays not empty)
    }
}

// ── Count-up animation ────────────────────────────────────────────────────────

@Composable
private fun AnimatedCount(
    target: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
    color: Color = MaterialTheme.colorScheme.primary,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val animatable = remember(target) { Animatable(0f) }
    LaunchedEffect(target) {
        animatable.snapTo(0f)
        animatable.animateTo(target.toFloat(), animationSpec = tween(700, easing = FastOutSlowInEasing))
    }
    Text(
        animatable.value.roundToInt().toString(),
        modifier = modifier,
        style = style.withTabularNumbers(),
        color = color,
        fontWeight = fontWeight
    )
}

// ── Narrative header ──────────────────────────────────────────────────────────

@Composable
private fun NarrativeHeader(text: String, currentStreak: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (currentStreak >= 3) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "$currentStreak-day streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Hero rotating observation ─────────────────────────────────────────────────

@Composable
private fun HeroObservationCard(observations: List<SmartObservation>) {
    if (observations.isEmpty()) return
    val dayOfYear = remember { LocalDate.now().dayOfYear }
    var offset by remember { mutableIntStateOf(0) }
    val observation = observations[(dayOfYear + offset) % observations.size]

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { offset = (offset + 1) % observations.size },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Text(
                    "This week's insight",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                observation.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 26.sp
            )
            Text(
                "Tap for another · ${(offset % observations.size) + 1} of ${observations.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ── Summary ───────────────────────────────────────────────────────────────────

@Composable
private fun SummarySection(
    totalPlays: Int,
    uniqueGames: Int,
    totalMinutes: Int,
    playerCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Hero plays number
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AnimatedCount(
                    target = totalPlays,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "plays logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Supporting numbers in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                SupportingStat(
                    value = uniqueGames.toString(),
                    label = "games",
                    modifier = Modifier.weight(1f)
                )
                SupportingStat(
                    value = if (totalMinutes > 0) formatDuration(totalMinutes) else "—",
                    label = "at the table",
                    modifier = Modifier.weight(1f)
                )
                SupportingStat(
                    value = playerCount.toString(),
                    label = "players",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SupportingStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.withTabularNumbers(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── 52-week heatmap ────────────────────────────────────────────────────────────

@Composable
private fun HeatmapSection(heatmapData: HeatmapData) {
    val cellSizeDp = 11.dp
    val cellGapDp = 2.dp
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dayLabels = listOf("M", "", "W", "", "F", "", "S")

    SectionCard {
        StatsCardHeader(title = "Play History", subtitle = "Last 52 weeks")
        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // Fixed day-of-week labels
            Column(
                modifier = Modifier.padding(top = 16.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(cellGapDp)
            ) {
                dayLabels.forEach { label ->
                    Box(modifier = Modifier.size(cellSizeDp), contentAlignment = Alignment.Center) {
                        if (label.isNotEmpty()) {
                            Text(
                                label,
                                fontSize = 7.sp,
                                color = onSurfaceVariantColor.copy(alpha = 0.45f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Scrollable grid with month labels
            val scrollState = rememberScrollState()
            LaunchedEffect(heatmapData.weeks.size) { scrollState.scrollTo(scrollState.maxValue) }

            Column(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
                // Month label row (one label per first week of each month)
                Row(horizontalArrangement = Arrangement.spacedBy(cellGapDp)) {
                    heatmapData.weeks.forEachIndexed { weekIdx, _ ->
                        val label = heatmapData.monthLabels.find { it.first == weekIdx }?.second ?: ""
                        Box(modifier = Modifier.size(cellSizeDp).height(14.dp)) {
                            if (label.isNotEmpty()) {
                                Text(
                                    label,
                                    fontSize = 7.sp,
                                    color = onSurfaceVariantColor.copy(alpha = 0.55f),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.BottomStart)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                // Grid
                Row(horizontalArrangement = Arrangement.spacedBy(cellGapDp)) {
                    heatmapData.weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(cellGapDp)) {
                            week.days.forEach { day ->
                                val alpha = when {
                                    day.count < 0 -> 0f
                                    day.count == 0 -> 0f
                                    else -> ((day.count.toFloat() / heatmapData.maxCount) * 0.85f + 0.15f).coerceIn(0f, 1f)
                                }
                                val isToday = day.date == LocalDate.now()
                                Box(
                                    modifier = Modifier
                                        .size(cellSizeDp)
                                        .background(
                                            when {
                                                isToday && day.count == 0 -> primaryColor.copy(alpha = 0.18f)
                                                day.count > 0 -> primaryColor.copy(alpha = alpha)
                                                else -> surfaceVariantColor.copy(alpha = 0.45f)
                                            },
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Less",
                fontSize = 8.sp,
                color = onSurfaceVariantColor.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(Modifier.width(4.dp))
            listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f).forEach { level ->
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            if (level == 0f) surfaceVariantColor.copy(alpha = 0.45f)
                            else primaryColor.copy(alpha = level * 0.85f + 0.15f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "More",
                fontSize = 8.sp,
                color = onSurfaceVariantColor.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ── Activity bar chart ─────────────────────────────────────────────────────────

@Composable
private fun ActivitySection(activity: List<ActivityBucket>, rangeLabel: String = "All time") {
    val totalInWindow = activity.sumOf { it.count }
    val peakBucket = activity.maxByOrNull { it.count }
    val subtitle = "$rangeLabel · $totalInWindow ${if (totalInWindow == 1) "play" else "plays"}"

    SectionCard {
        StatsCardHeader(title = "Activity", subtitle = subtitle)
        Spacer(Modifier.height(12.dp))
        BucketBarChart(
            values = activity.map { it.count },
            labels = activity.map { it.label },
            highlightIndex = activity.indexOfFirst { it.highlight }
        )
        peakBucket?.takeIf { it.count > 0 }?.let { peak ->
            Spacer(Modifier.height(8.dp))
            Text(
                "Peak: ${peak.peakLabel} · ${peak.count} ${if (peak.count == 1) "play" else "plays"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun BucketBarChart(values: List<Int>, labels: List<String>, highlightIndex: Int = -1) {
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        progress.animateTo(1f, boardFlowTween(BoardFlowMotion.ChartBaseDuration))
    }

    val maxVal = (values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
    val prog = progress.value
    val primaryColor   = MaterialTheme.colorScheme.primary
    val dimColor       = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
        val n = values.size
        val slotW = size.width / n
        val barW = slotW * 0.52f

        values.forEachIndexed { i, v ->
            val rawH = (v / maxVal) * size.height * prog
            val barH = rawH.coerceAtLeast(if (v > 0) 4f else 0f)
            val x = i * slotW + (slotW - barW) / 2f

            if (v == 0) {
                drawRoundRect(
                    color = surfaceVariant,
                    topLeft = Offset(x, size.height - 3f),
                    size = Size(barW, 3f),
                    cornerRadius = CornerRadius(2f)
                )
            } else {
                drawRoundRect(
                    color = if (i == highlightIndex) primaryColor else dimColor,
                    topLeft = Offset(x, size.height - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, label ->
            Text(
                label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.5.sp,
                fontWeight = if (i == highlightIndex) FontWeight.Bold else FontWeight.Normal,
                color = if (i == highlightIndex) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

// ── Top games ──────────────────────────────────────────────────────────────────

@Composable
private fun TopGamesSection(
    games: List<GameStat>,
    rangeLabel: String = "All time",
    onGameTapped: (gameId: Int, gameName: String) -> Unit = { _, _ -> }
) {
    SectionCard {
        StatsCardHeader(title = "Top Games", subtitle = rangeLabel)
        Spacer(Modifier.height(12.dp))
        val maxPlays = games.firstOrNull()?.plays ?: 1
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            games.forEachIndexed { i, game ->
                TopGameRow(game = game, rank = i + 1, maxPlays = maxPlays,
                    onClick = { onGameTapped(game.gameId, game.name) })
            }
        }
    }
}

@Composable
private fun TopGameRow(game: GameStat, rank: Int, maxPlays: Int, onClick: () -> Unit = {}) {
    val fraction by animateFloatAsState(
        targetValue = if (maxPlays > 0) game.plays.toFloat() / maxPlays else 0f,
        animationSpec = boardFlowTween(BoardFlowMotion.ChartRowDuration + rank * BoardFlowMotion.ChartRowStagger),
        label = "bar_$rank"
    )
    val rankBadgeColor = when (rank) {
        1 -> Color(0xFFE6A817); 2 -> Color(0xFF9E9E9E); 3 -> Color(0xFFBF7D3A)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val rankTextColor = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val barColor = when (rank) {
        1 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = maxOf(0.38f, 1f - rank * 0.07f))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(22.dp).background(rankBadgeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$rank", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = rankTextColor, fontSize = 9.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                game.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (rank <= 3) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().background(barColor, RoundedCornerShape(2.dp)))
            }
        }
        Text(
            "${game.plays}",
            style = MaterialTheme.typography.labelLarge.withTabularNumbers(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End
        )
    }
}

// ── Rivalry pairs ─────────────────────────────────────────────────────────────

@Composable
private fun RivalryPairsSection(pairs: List<RivalryPair>) {
    SectionCard {
        StatsCardHeader(title = "Great Rivalries", subtitle = "At your table")
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            pairs.forEach { pair -> RivalryPairRow(pair) }
        }
    }
}

@Composable
private fun RivalryPairRow(pair: RivalryPair) {
    val totalDecisive = pair.aWins + pair.bWins
    val aFraction = if (totalDecisive > 0) pair.aWins.toFloat() / totalDecisive else 0.5f
    val aFractionAnim by animateFloatAsState(
        targetValue = aFraction,
        animationSpec = boardFlowTween(700),
        label = "rivalry_${pair.playerA}"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            pair.narrativeLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )

        // Head-to-head bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                pair.playerA.take(10),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(60.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
            Box(
                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                    .background(surfaceVariant)
            ) {
                // Player A wins (left side)
                Box(
                    modifier = Modifier.fillMaxWidth(aFractionAnim).fillMaxHeight()
                        .background(primaryColor.copy(alpha = 0.9f), RoundedCornerShape(3.dp))
                )
                // Center divider dot
                Box(
                    modifier = Modifier.align(Alignment.Center).size(width = 2.dp, height = 6.dp)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                )
            }
            Text(
                pair.playerB.take(10),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(60.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Win counts + most played game
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${pair.aWins}W · ${pair.bWins}W",
                style = MaterialTheme.typography.labelSmall.withTabularNumbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            pair.mostPlayedGame?.let { game ->
                Text(
                    "Often: $game",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// ── Day of week ────────────────────────────────────────────────────────────────

@Composable
private fun DayOfWeekSection(dist: List<Pair<String, Int>>, rangeLabel: String = "All time") {
    val peak = dist.maxByOrNull { it.second }
    val subtitle = peak?.takeIf { it.second > 0 }?.let { "${it.first} · $rangeLabel" }
    SectionCard {
        StatsCardHeader(title = "Favourite Day", subtitle = subtitle)
        Spacer(Modifier.height(12.dp))
        BucketBarChart(
            values = dist.map { it.second },
            labels = dist.map { it.first },
            highlightIndex = dist.indexOfFirst { it == peak && it.second > 0 }
        )
    }
}

// ── Top players ────────────────────────────────────────────────────────────────

@Composable
private fun TopPlayersSection(
    topPlayers: List<PlayerStat>,
    rangeLabel: String = "All time",
    onPlayerTapped: (String) -> Unit = {}
) {
    SectionCard {
        StatsCardHeader(title = "Top Players", subtitle = rangeLabel)
        Spacer(Modifier.height(12.dp))
        val maxPlays = topPlayers.firstOrNull()?.plays ?: 1
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            topPlayers.forEachIndexed { i, player ->
                TopPlayerRow(player = player, rank = i + 1, maxPlays = maxPlays,
                    onClick = { onPlayerTapped(player.displayName) })
            }
        }
    }
}

@Composable
private fun TopPlayerRow(player: PlayerStat, rank: Int, maxPlays: Int, onClick: () -> Unit = {}) {
    val fraction by animateFloatAsState(
        targetValue = if (maxPlays > 0) player.plays.toFloat() / maxPlays else 0f,
        animationSpec = boardFlowTween(BoardFlowMotion.PlayerChartRowDuration + rank * BoardFlowMotion.PlayerChartRowStagger),
        label = "player_$rank"
    )
    val winRate = if (player.plays > 0) player.wins.toFloat() / player.plays else 0f
    val hasWinData = player.wins > 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                player.displayName.take(1).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 11.sp
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    player.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (rank == 1) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (hasWinData) {
                    Text(
                        "${(winRate * 100).roundToInt()}% wins",
                        style = MaterialTheme.typography.labelSmall.withTabularNumbers(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(alpha = maxOf(0.4f, 1f - rank * 0.08f)),
                            RoundedCornerShape(2.dp)
                        )
                )
                if (hasWinData) {
                    Box(
                        modifier = Modifier.fillMaxWidth(fraction * winRate).fillMaxHeight()
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
        Text(
            "${player.plays}",
            style = MaterialTheme.typography.labelLarge.withTabularNumbers(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End
        )
    }
}

// ── On This Day ────────────────────────────────────────────────────────────────

@Composable
private fun OnThisDaySection(
    entries: List<OnThisDayEntry>,
    onGameTapped: (gameId: Int, gameName: String) -> Unit = { _, _ -> }
) {
    SectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Text(
                "On This Day",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            entries.forEach { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val yearsLabel = if (entry.yearsAgo == 1) "1 year ago" else "${entry.yearsAgo} years ago"
                    Text(
                        "$yearsLabel · ${entry.year}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    entry.plays.forEach { play ->
                        val winners = play.players.filter { it.isWinner }.map { it.name }
                            .filter { it.isNotBlank() }
                        val winnerText = if (winners.isNotEmpty()) " · ${winners.joinToString()} won" else ""
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onGameTapped(play.gameId, play.gameName)
                            },
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(4.dp).background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape
                                )
                            )
                            Text(
                                play.gameName + winnerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Gamer archetype ────────────────────────────────────────────────────────────

@Composable
private fun ArchetypeCard(archetype: GamerArchetype) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Your gaming profile",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    archetype.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    archetype.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                when (archetype) {
                    GamerArchetype.DEDICANT  -> Icons.Default.Star
                    GamerArchetype.LOYALIST  -> Icons.Default.EmojiEvents
                    GamerArchetype.MARATHONER -> Icons.Default.Schedule
                    GamerArchetype.SOCIALITE -> Icons.Default.Group
                    GamerArchetype.EXPLORER  -> Icons.AutoMirrored.Filled.TrendingUp
                    GamerArchetype.CURATOR   -> Icons.Default.History
                },
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            )
        }
    }
}

// ── Insights ──────────────────────────────────────────────────────────────────

@Composable
private fun InsightsSection(
    insights: List<StatsInsight>,
    rangeLabel: String = "All time",
    onGameTapped: (gameId: Int, gameName: String) -> Unit = { _, _ -> },
    onPlayerTapped: (String) -> Unit = {}
) {
    SectionCard {
        StatsCardHeader(title = "Insights", subtitle = rangeLabel)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            insights.chunked(3).forEach { rowInsights ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowInsights.forEach { insight ->
                        val onClick: (() -> Unit)? = when {
                            insight.gameFilter != null -> { { onGameTapped(insight.gameFilter.first, insight.gameFilter.second) } }
                            insight.playerFilter != null -> { { onPlayerTapped(insight.playerFilter) } }
                            else -> null
                        }
                        InsightChip(
                            icon = insight.icon,
                            value = insight.value,
                            label = insight.label,
                            detail = insight.detail,
                            onClick = onClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowInsights.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun InsightChip(
    icon: ImageVector,
    value: String,
    label: String,
    detail: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val border = if (onClick != null)
        androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    else null
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = border
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                icon, contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = if (onClick != null) 0.9f else 0.7f)
            )
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.withTabularNumbers(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun StatsCardHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}
