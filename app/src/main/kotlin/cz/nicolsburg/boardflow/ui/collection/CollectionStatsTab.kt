package cz.nicolsburg.boardflow.ui.collection

import cz.nicolsburg.boardflow.ui.theme.Spacing
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import cz.nicolsburg.boardflow.ui.common.GameCover
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.model.GameItem
import cz.nicolsburg.boardflow.model.SleeveTrackingState
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowModalBottomSheet
import cz.nicolsburg.boardflow.ui.common.BoardFlowSurfaceTokens
import cz.nicolsburg.boardflow.ui.common.SectionCard
import cz.nicolsburg.boardflow.ui.common.withTabularNumbers

// ── Data ──────────────────────────────────────────────────────────────────────

private data class CollectionStats(
    val totalOwned: Int,
    val wishlistCount: Int,
    val unplayedCount: Int,
    val avgRating: Double?,
    val totalBggPlays: Int,
    val playDepth: List<Pair<String, Int>>,
    val weightTiers: List<Pair<String, Int>>,
    val sleeved: Int,
    val toSleeve: Int,
    val notTracked: Int,
    val topPlayed: List<GameItem>,
    val neverPlayedGames: List<GameItem>,
)

private fun computeStats(games: List<GameItem>): CollectionStats {
    val owned = games.filter { it.isOwned }

    val unplayed = owned.filter { (it.numPlays ?: 0) == 0 }
    val ratingsWithValue = owned.mapNotNull { it.rating }.filter { it > 0 }
    val avgRating = if (ratingsWithValue.isNotEmpty()) ratingsWithValue.average() else null
    val totalBggPlays = owned.sumOf { it.numPlays ?: 0 }

    val playDepth = listOf(
        "Unplayed" to owned.count { (it.numPlays ?: 0) == 0 },
        "Tried (1-4)" to owned.count { (it.numPlays ?: 0) in 1..4 },
        "Familiar (5-14)" to owned.count { (it.numPlays ?: 0) in 5..14 },
        "Deep (15+)" to owned.count { (it.numPlays ?: 0) >= 15 },
    ).filter { it.second > 0 }

    val weightOrder = listOf("Light", "Casual", "Medium-Light", "Medium", "Medium-Heavy", "Heavy", "Expert")
    val weightTiers = owned.mapNotNull { it.weight?.let { w -> gameWeightLabel(w) } }
        .groupingBy { it }.eachCount()
        .entries.sortedBy { weightOrder.indexOf(it.key).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
        .map { it.key to it.value }

    val sleeveCounts = owned.groupBy { sheetSleeveStatus(it) }
    val sleeved = sleeveCounts[SleeveTrackingState.SLEEVED]?.size ?: 0
    val toSleeve = (sleeveCounts[SleeveTrackingState.TO_SLEEVE]?.size ?: 0) +
            (sleeveCounts[SleeveTrackingState.POSSIBLE]?.size ?: 0)
    val notTracked = (sleeveCounts[SleeveTrackingState.NOT_SLEEVING]?.size ?: 0) +
            (sleeveCounts[SleeveTrackingState.UNKNOWN]?.size ?: 0)

    val topPlayed = owned.filter { (it.numPlays ?: 0) > 0 }
        .sortedByDescending { it.numPlays ?: 0 }
        .take(5)

    return CollectionStats(
        totalOwned = owned.size,
        wishlistCount = games.count { it.isWishlisted },
        unplayedCount = unplayed.size,
        avgRating = avgRating,
        totalBggPlays = totalBggPlays,
        playDepth = playDepth,
        weightTiers = weightTiers,
        sleeved = sleeved,
        toSleeve = toSleeve,
        notTracked = notTracked,
        topPlayed = topPlayed,
        neverPlayedGames = unplayed.sortedBy { it.name.lowercase() },
    )
}

// ── Root composable ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionStatsTab(
    games: List<GameItem>,
    onMarkAsPlayed: (gameId: Int, gameName: String) -> Unit = { _, _ -> },
    historyPlayCounts: Map<Int, Int> = emptyMap(),
    onGameTapped: (GameItem) -> Unit = {},
) {
    var markedObjectIds by remember(games) { mutableStateOf(emptySet<String>()) }
    val stats = remember(games, markedObjectIds, historyPlayCounts) {
        // BGG's play count lags behind plays logged here until the next sync, so use
        // whichever is higher. Display only: the canonical snapshot is not touched.
        val withHistory = games.map { game ->
            val logged = game.objectId.toIntOrNull()?.let { historyPlayCounts[it] } ?: 0
            if (logged > (game.numPlays ?: 0)) {
                game.copy(ownership = game.ownership.copy(bggPlayCount = logged))
            } else {
                game
            }
        }
        val base = computeStats(withHistory)
        if (markedObjectIds.isEmpty()) base
        else base.copy(neverPlayedGames = base.neverPlayedGames.filter { it.objectId !in markedObjectIds })
    }

    if (stats.totalOwned == 0) {
        Box(Modifier.fillMaxSize().padding(Spacing.xxl), contentAlignment = Alignment.Center) {
            Text(
                "No owned games yet. Refresh your collection in Settings, Sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { OverviewCard(stats) }
        if (stats.playDepth.isNotEmpty()) {
            item { PlayDepthCard(stats) }
        }
        if (stats.weightTiers.isNotEmpty()) {
            item { ComplexityCard(stats) }
        }
        if (stats.sleeved + stats.toSleeve + stats.notTracked > 0) {
            item { SleeveCard(stats) }
        }
        if (stats.topPlayed.isNotEmpty()) {
            item { TopPlayedCard(stats, onGameTapped) }
        }
        if (stats.neverPlayedGames.isNotEmpty()) {
            item {
                UnplayedShelfCard(stats, onGameTapped = onGameTapped) { game ->
                    markedObjectIds = markedObjectIds + game.objectId
                    game.objectId.toIntOrNull()?.let { id -> onMarkAsPlayed(id, game.name) }
                }
            }
        }
    }
}

// ── Cards ─────────────────────────────────────────────────────────────────────

@Composable
private fun OverviewCard(stats: CollectionStats) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BigStat(
                value = stats.totalOwned.toString(),
                label = "Owned",
                icon = { Icon(Icons.Default.Inventory2, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            )
            BigStat(
                value = stats.wishlistCount.toString(),
                label = "Wishlist",
                icon = { Icon(Icons.Default.Bookmark, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            )
            BigStat(
                value = stats.unplayedCount.toString(),
                label = "Unplayed",
            )
        }

        if (stats.avgRating != null || stats.totalBggPlays > 0) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                stats.avgRating?.let { rating ->
                    InlineStat(
                        icon = Icons.Default.Star,
                        label = "Avg rating  ${formatDecimal(rating)}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (stats.totalBggPlays > 0) {
                    Text(
                        "${stats.totalBggPlays} total BGG plays",
                        style = MaterialTheme.typography.labelMedium.withTabularNumbers(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayDepthCard(stats: CollectionStats) {
    val max = stats.playDepth.maxOf { it.second }
    SectionCard {
        CardTitle("Play depth")
        stats.playDepth.forEachIndexed { i, (label, count) ->
            if (i > 0) Spacer(Modifier.height(6.dp))
            StatBarRow(
                label = label,
                count = count,
                total = stats.totalOwned,
                fraction = if (max > 0) count.toFloat() / max else 0f,
                barColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ComplexityCard(stats: CollectionStats) {
    val max = stats.weightTiers.maxOf { it.second }
    SectionCard {
        CardTitle("Complexity")
        stats.weightTiers.forEachIndexed { i, (label, count) ->
            if (i > 0) Spacer(Modifier.height(6.dp))
            StatBarRow(
                label = label,
                count = count,
                total = stats.totalOwned,
                fraction = if (max > 0) count.toFloat() / max else 0f,
                barColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SleeveCard(stats: CollectionStats) {
    SectionCard {
        CardTitle("Sleeve coverage")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BigStat(stats.sleeved.toString(), "Sleeved")
            BigStat(stats.toSleeve.toString(), "To sleeve")
            BigStat(stats.notTracked.toString(), "Not tracking")
        }
        val trackedTotal = stats.sleeved + stats.toSleeve + stats.notTracked
        if (trackedTotal > 0 && stats.sleeved > 0) {
            val pct = kotlin.math.round(stats.sleeved * 100f / trackedTotal).toInt()
            Text(
                "$pct% of collection sleeved",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TopPlayedCard(stats: CollectionStats, onGameTapped: (GameItem) -> Unit) {
    SectionCard {
        CardTitle("Most played")
        stats.topPlayed.forEach { game ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(BoardFlowShape.Control)
                    .clickable { onGameTapped(game) }
                    .heightIn(min = Dimens.MinTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                GameCover(name = game.name, thumbnailUrl = game.thumbnailUrl, size = 40.dp)
                Text(
                    game.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    "${game.numPlays} ${if (game.numPlays == 1) "play" else "plays"}",
                    style = MaterialTheme.typography.bodyMedium.withTabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UnplayedShelfCard(
    stats: CollectionStats,
    onGameTapped: (GameItem) -> Unit,
    onMarkGame: (GameItem) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(BoardFlowShape.Control)
                .clickable { expanded = !expanded }
                .heightIn(min = Dimens.MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Unplayed shelf",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${stats.neverPlayedGames.size} game${if (stats.neverPlayedGames.size == 1) "" else "s"} never played",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                stats.neverPlayedGames.forEach { game ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BoardFlowShape.Control)
                            .clickable { onGameTapped(game) }
                            .heightIn(min = Dimens.MinTouchTarget),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        GameCover(name = game.name, thumbnailUrl = game.thumbnailUrl, size = 32.dp)
                        Text(
                            game.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        BoardFlowInlineAction(onClick = { onMarkGame(game) }) {
                            Text("Mark played")
                        }
                    }
                }
            }
        }
    }
}

// ── Shared primitives ─────────────────────────────────────────────────────────

@Composable
private fun CardTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun BigStat(
    value: String,
    label: String,
    icon: (@Composable () -> Unit)? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                icon()
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall.withTabularNumbers(),
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.withTabularNumbers(),
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatBarRow(
    label: String,
    count: Int,
    total: Int,
    fraction: Float,
    barColor: androidx.compose.ui.graphics.Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "$count · ${if (total > 0) kotlin.math.round(count * 100f / total).toInt() else 0}%",
                style = MaterialTheme.typography.bodySmall.withTabularNumbers(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(BoardFlowShape.Pill)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(BoardFlowShape.Pill)
                    .background(barColor),
            )
        }
    }
}
