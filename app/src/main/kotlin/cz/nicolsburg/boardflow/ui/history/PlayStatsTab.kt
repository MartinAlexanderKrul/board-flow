package cz.nicolsburg.boardflow.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.ui.common.SectionCard
import cz.nicolsburg.boardflow.ui.common.withTabularNumbers
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// ── Data holders ─────────────────────────────────────────────────────────────

private data class MonthBucket(val label: String, val yearMonth: Int, val count: Int)
private data class GameStat(val name: String, val plays: Int)
private data class PlayerStat(val displayName: String, val plays: Int, val wins: Int)

// ── Pure computations ─────────────────────────────────────────────────────────

private fun buildMonthBuckets(plays: List<LoggedPlay>): List<MonthBucket> {
    val byMonth = plays
        .mapNotNull { play ->
            runCatching { LocalDate.parse(play.date) }.getOrNull()
                ?.let { d -> (d.year * 100 + d.monthValue) to play.quantity.coerceAtLeast(1) }
        }
        .groupBy({ it.first }) { it.second }
        .mapValues { it.value.sum() }
    val now = LocalDate.now()
    return (11 downTo 0).map { back ->
        val d = now.minusMonths(back.toLong())
        val ym = d.year * 100 + d.monthValue
        MonthBucket(
            label = d.format(DateTimeFormatter.ofPattern("MMM")),
            yearMonth = ym,
            count = byMonth[ym] ?: 0
        )
    }
}

private fun buildTopGames(plays: List<LoggedPlay>, limit: Int = 10): List<GameStat> =
    plays
        .groupBy { it.gameName }
        .mapValues { (_, g) -> g.sumOf { it.quantity.coerceAtLeast(1) } }
        .entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { GameStat(it.key, it.value) }

private fun buildTopPlayers(
    plays: List<LoggedPlay>,
    roster: List<Player>,
    limit: Int = 8
): List<PlayerStat> {
    fun resolveDisplayName(raw: String): String {
        val lower = raw.lowercase().trim()
        return roster.firstOrNull { p ->
            p.displayName.lowercase().trim() == lower ||
                p.aliases.any { a -> a.lowercase().trim() == lower }
        }?.displayName ?: raw
    }
    val map = mutableMapOf<String, Pair<Int, Int>>() // displayName → (plays, wins)
    plays.forEach { play ->
        play.players.filter { it.name.isNotBlank() }.forEach { pr ->
            val name = resolveDisplayName(pr.name)
            val (p, w) = map[name] ?: (0 to 0)
            map[name] = (p + 1) to (if (pr.isWinner) w + 1 else w)
        }
    }
    return map.entries
        .sortedByDescending { it.value.first }
        .take(limit)
        .map { (name, pw) -> PlayerStat(name, pw.first, pw.second) }
}

private fun computeHIndex(plays: List<LoggedPlay>): Int {
    val counts = plays
        .groupBy { it.gameName }
        .mapValues { (_, g) -> g.sumOf { it.quantity.coerceAtLeast(1) } }
        .values
        .sortedDescending()
    var h = 0
    counts.forEachIndexed { i, c -> if (c >= i + 1) h = i + 1 }
    return h
}

private fun computeStreaks(plays: List<LoggedPlay>): Pair<Int, Int> {
    val dates = plays
        .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
        .toSortedSet()
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

// ── Main composable ───────────────────────────────────────────────────────────

@Composable
internal fun StatsContent(
    plays: List<LoggedPlay>,
    players: List<Player>,
    modifier: Modifier = Modifier
) {
    val statPlays = remember(plays) { plays.filter { it.nowInStats } }

    if (statPlays.isEmpty()) {
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
                    "Log some plays to see your statistics here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // ── Derived stats ────────────────────────────────────────────────────────
    val totalPlays    = remember(statPlays) { statPlays.sumOf { it.quantity.coerceAtLeast(1) } }
    val uniqueGames   = remember(statPlays) { statPlays.map { it.gameName }.toSet().size }
    val totalMinutes  = remember(statPlays) { statPlays.filter { it.durationMinutes > 0 }.sumOf { it.durationMinutes } }
    val playsThisYear = remember(statPlays) {
        val y = LocalDate.now().year
        statPlays.filter { runCatching { LocalDate.parse(it.date).year == y }.getOrDefault(false) }
            .sumOf { it.quantity.coerceAtLeast(1) }
    }
    val months       = remember(statPlays) { buildMonthBuckets(statPlays) }
    val topGames     = remember(statPlays) { buildTopGames(statPlays) }
    val topPlayers   = remember(statPlays, players) { buildTopPlayers(statPlays, players) }
    val hIndex       = remember(statPlays) { computeHIndex(statPlays) }
    val (curStreak, bestStreak) = remember(statPlays) { computeStreaks(statPlays) }
    val dayDist      = remember(statPlays) { buildDayOfWeekDist(statPlays) }
    val avgDuration  = remember(statPlays) {
        val d = statPlays.filter { it.durationMinutes > 0 }
        if (d.isEmpty()) null else d.sumOf { it.durationMinutes } / d.size
    }
    val longestSession = remember(statPlays) {
        statPlays.filter { it.durationMinutes > 0 }.maxByOrNull { it.durationMinutes }
            ?.let { it.gameName to it.durationMinutes }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SummarySection(totalPlays, uniqueGames, totalMinutes, players.size, playsThisYear) }

        if (months.any { it.count > 0 }) {
            item { ActivitySection(months) }
        }

        if (topGames.isNotEmpty()) {
            item { TopGamesSection(topGames) }
        }

        if (dayDist.any { it.second > 0 }) {
            item { DayOfWeekSection(dayDist) }
        }

        if (topPlayers.isNotEmpty()) {
            item { TopPlayersSection(topPlayers) }
        }

        item {
            InsightsSection(
                hIndex = hIndex,
                currentStreak = curStreak,
                bestStreak = bestStreak,
                avgDuration = avgDuration,
                longestSession = longestSession
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
    playerCount: Int,
    playsThisYear: Int
) {
    val totalHours = totalMinutes / 60
    val remainingMinutes = totalMinutes % 60

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BigStatCard(
                value = "$totalPlays",
                label = "Total Plays",
                icon = Icons.Default.History,
                modifier = Modifier.weight(1f)
            )
            BigStatCard(
                value = "$uniqueGames",
                label = "Unique Games",
                icon = Icons.Default.Star,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BigStatCard(
                value = if (totalMinutes > 0) "${totalHours}h ${remainingMinutes}m" else "—",
                label = "Time Played",
                icon = Icons.Default.Schedule,
                modifier = Modifier.weight(1f)
            )
            BigStatCard(
                value = "$playerCount",
                label = "Players",
                icon = Icons.Default.EmojiEvents,
                modifier = Modifier.weight(1f)
            )
        }
        if (playsThisYear > 0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "This year",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "$playsThisYear ${if (playsThisYear == 1) "play" else "plays"}",
                        style = MaterialTheme.typography.labelLarge.withTabularNumbers(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun BigStatCard(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.withTabularNumbers(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Activity bar chart ────────────────────────────────────────────────────────

@Composable
private fun ActivitySection(months: List<MonthBucket>) {
    val totalInWindow = months.sumOf { it.count }
    val peakMonth = months.maxByOrNull { it.count }

    SectionCard {
        StatsCardHeader(
            title = "Play Activity",
            subtitle = "Last 12 months  ·  $totalInWindow ${if (totalInWindow == 1) "play" else "plays"}"
        )
        Spacer(Modifier.height(12.dp))

        val currentYM = LocalDate.now().let { it.year * 100 + it.monthValue }
        BucketBarChart(
            values = months.map { it.count },
            labels = months.map { it.label },
            highlightIndex = months.indexOfFirst { it.yearMonth == currentYM }
        )

        peakMonth?.takeIf { it.count > 0 }?.let { peak ->
            Spacer(Modifier.height(8.dp))
            Text(
                "Peak: ${peak.label} — ${peak.count} ${if (peak.count == 1) "play" else "plays"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun BucketBarChart(
    values: List<Int>,
    labels: List<String>,
    highlightIndex: Int = -1
) {
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    val maxVal = (values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
    val prog = progress.value

    val primaryColor    = MaterialTheme.colorScheme.primary
    val dimColor        = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    val surfaceVariant  = MaterialTheme.colorScheme.surfaceVariant

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val n = values.size
        val slotW = size.width / n
        val barW = slotW * 0.52f

        values.forEachIndexed { i, v ->
            val rawH = (v / maxVal) * size.height * prog
            val barH = rawH.coerceAtLeast(if (v > 0) 4f else 0f)
            val x = i * slotW + (slotW - barW) / 2f

            // Track (empty bar outline)
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
                color = if (i == highlightIndex)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

// ── Top games ─────────────────────────────────────────────────────────────────

@Composable
private fun TopGamesSection(games: List<GameStat>) {
    SectionCard {
        StatsCardHeader(
            title = "Top Games",
            subtitle = "Most played of all time"
        )
        Spacer(Modifier.height(12.dp))
        val maxPlays = games.firstOrNull()?.plays ?: 1
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            games.forEachIndexed { i, game ->
                TopGameRow(game = game, rank = i + 1, maxPlays = maxPlays)
            }
        }
    }
}

@Composable
private fun TopGameRow(game: GameStat, rank: Int, maxPlays: Int) {
    val fraction by animateFloatAsState(
        targetValue = if (maxPlays > 0) game.plays.toFloat() / maxPlays else 0f,
        animationSpec = tween(600 + rank * 35, easing = FastOutSlowInEasing),
        label = "bar_$rank"
    )

    val rankBadgeColor = when (rank) {
        1 -> Color(0xFFE6A817)
        2 -> Color(0xFF9E9E9E)
        3 -> Color(0xFFBF7D3A)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val rankTextColor = when {
        rank <= 3 -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val barColor = when (rank) {
        1 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = maxOf(0.38f, 1f - rank * 0.07f))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(rankBadgeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$rank",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = rankTextColor,
                fontSize = 9.sp
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                game.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (rank <= 3) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(barColor, RoundedCornerShape(2.dp))
                )
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

// ── Day of week ───────────────────────────────────────────────────────────────

@Composable
private fun DayOfWeekSection(dist: List<Pair<String, Int>>) {
    val peak = dist.maxByOrNull { it.second }
    SectionCard {
        StatsCardHeader(
            title = "Favourite Day",
            subtitle = peak?.takeIf { it.second > 0 }?.let { "Most plays on ${it.first}" }
        )
        Spacer(Modifier.height(12.dp))
        BucketBarChart(
            values = dist.map { it.second },
            labels = dist.map { it.first },
            highlightIndex = dist.indexOfFirst { it == peak && it.second > 0 }
        )
    }
}

// ── Top players ───────────────────────────────────────────────────────────────

@Composable
private fun TopPlayersSection(topPlayers: List<PlayerStat>) {
    SectionCard {
        StatsCardHeader(
            title = "Top Players",
            subtitle = "By number of recorded plays"
        )
        Spacer(Modifier.height(12.dp))
        val maxPlays = topPlayers.firstOrNull()?.plays ?: 1
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            topPlayers.forEachIndexed { i, player ->
                TopPlayerRow(player = player, rank = i + 1, maxPlays = maxPlays)
            }
        }
    }
}

@Composable
private fun TopPlayerRow(player: PlayerStat, rank: Int, maxPlays: Int) {
    val fraction by animateFloatAsState(
        targetValue = if (maxPlays > 0) player.plays.toFloat() / maxPlays else 0f,
        animationSpec = tween(550 + rank * 30, easing = FastOutSlowInEasing),
        label = "player_$rank"
    )
    val winRate = if (player.plays > 0) player.wins.toFloat() / player.plays else 0f
    val hasWinData = player.wins > 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Avatar circle with initials
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
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

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Plays bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(
                            MaterialTheme.colorScheme.secondary.copy(
                                alpha = maxOf(0.4f, 1f - rank * 0.08f)
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )
                // Win rate overlay
                if (hasWinData) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction * winRate)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.secondary,
                                RoundedCornerShape(2.dp)
                            )
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

// ── Insights ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightsSection(
    hIndex: Int,
    currentStreak: Int,
    bestStreak: Int,
    avgDuration: Int?,
    longestSession: Pair<String, Int>?
) {
    SectionCard {
        StatsCardHeader(title = "Insights")
        Spacer(Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InsightChip(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                value = "H-$hIndex",
                label = "H-index",
                detail = if (hIndex > 0)
                    "Played $hIndex games\nat least $hIndex times"
                else
                    "Play the same game\nmultiple times to grow this",
                modifier = Modifier.weight(1f)
            )
            if (bestStreak > 1) {
                InsightChip(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${bestStreak}d",
                    label = "Best streak",
                    detail = if (currentStreak > 1) "Current: ${currentStreak}d 🔥" else null,
                    modifier = Modifier.weight(1f)
                )
            }
            avgDuration?.let { avg ->
                InsightChip(
                    icon = Icons.Default.Schedule,
                    value = if (avg >= 60) "${avg / 60}h ${avg % 60}m" else "${avg}m",
                    label = "Avg duration",
                    modifier = Modifier.weight(1f)
                )
            }
            longestSession?.let { (name, minutes) ->
                InsightChip(
                    icon = Icons.Default.EmojiEvents,
                    value = if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m",
                    label = "Longest session",
                    detail = name,
                    modifier = Modifier.weight(1f)
                )
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.withTabularNumbers(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    }
}
