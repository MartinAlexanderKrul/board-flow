package cz.nicolsburg.boardflow.ui.collection

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import cz.nicolsburg.boardflow.model.GameItem
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.ui.common.AnimatedDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.GameBackdrop
import cz.nicolsburg.boardflow.ui.common.BoardFlowOutlinedButton
import cz.nicolsburg.boardflow.ui.common.withTabularNumbers
import cz.nicolsburg.boardflow.ui.history.ContextualInsight
import cz.nicolsburg.boardflow.ui.history.GameHistoryStats
import cz.nicolsburg.boardflow.ui.history.gameContextualInsight
import cz.nicolsburg.boardflow.ui.history.gameHistoryStats

private data class InfoSection(
    val title: String,
    val stats: List<SectionStat>,
    val secondaryLabels: Set<String> = emptySet(),
    val columns: Int = 2
)

@Composable
fun GameDetailsDialog(
    game: GameItem,
    onDismiss: () -> Unit,
    historyPlays: List<LoggedPlay> = emptyList(),
    players: List<Player> = emptyList(),
    onLogPlay: () -> Unit = {},
    onViewHistory: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val bggUrl = bggSleevesUrl(game)
    val driveUrl = game.shareUrl?.takeIf { it.isNotBlank() }

    val gameObjectId = remember(game) { game.objectId.toIntOrNull()?.takeIf { it > 0 } }

    val overviewStats  = remember(game) { overviewStats(game) }
    val ratingStats    = remember(game) { ratingStats(game) }
    val playerStats    = remember(game) { playerPreferenceStats(game) }
    val customRows     = remember(game) { customDetailRows(game) }

    val myStats = remember(game, historyPlays, players) {
        if (gameObjectId != null) historyPlays.gameHistoryStats(gameObjectId, players)
        else historyPlays.gameHistoryStats(game.identity.name, players)
    }
    val contextualInsight = remember(game, historyPlays, players) {
        gameObjectId?.let { historyPlays.gameContextualInsight(it, players) }
    }

    val primaryColor   = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val headerChips    = remember(game, primaryColor, secondaryColor) {
        headerStatusChips(game, primaryColor, secondaryColor)
    }
    val compactChips = headerChips.size > 2 || LocalConfiguration.current.screenWidthDp < 380

    val infoSections = remember(playerStats, overviewStats, ratingStats) {
        buildList {
            if (playerStats.isNotEmpty())   add(InfoSection("Players", playerStats))
            if (overviewStats.isNotEmpty()) add(InfoSection("Overview", overviewStats))
            if (ratingStats.isNotEmpty())   add(InfoSection("Ratings", ratingStats, setOf("Rank"), columns = 3))
        }
    }
    val hasSleeves = game.sleeveStatus != GameItem.SleeveStatus.UNKNOWN ||
            game.sleeveCardSets.isNotEmpty()

    fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    AnimatedDialog(
        onDismissRequest = onDismiss,
        backdrop = { GameBackdrop(imageUrl = game.thumbnailUrl, height = 200.dp) }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Header with atmospheric backdrop ──────────────────────────────
            item {
                HeaderSection(game, headerChips, compactChips)
            }

            // ── Action row: Log Play (always) + History (only with plays) ─────
            if (gameObjectId != null) {
                item {
                    val hasHistory = myStats != null
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BoardFlowButton(
                            onClick = onLogPlay,
                            modifier = if (hasHistory) Modifier.weight(1f) else Modifier.fillMaxWidth()
                        ) { Text("Log Play") }

                        if (hasHistory) {
                            BoardFlowOutlinedButton(
                                onClick = { onViewHistory(gameObjectId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("History")
                            }
                        }
                    }
                }
            }

            // ── Your Stats hero card ──────────────────────────────────────────
            if (myStats != null) {
                item {
                    YourStatsCard(stats = myStats, insight = contextualInsight)
                }
            } else if (contextualInsight != null) {
                item {
                    Text(
                        text = contextualInsight.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }

            // ── Grouped info block (Players · Overview · Ratings) ─────────────
            if (infoSections.isNotEmpty()) {
                item { InfoGroupBlock(infoSections) }
            }

            // ── Sleeves ───────────────────────────────────────────────────────
            if (hasSleeves) {
                item { SleevesBlock(game) }
            }

            // ── More (custom spreadsheet rows) ────────────────────────────────
            if (customRows.isNotEmpty()) {
                item { InfoGroupBlock(listOf(InfoSection("More", customRows))) }
            }

            // ── External links ────────────────────────────────────────────────
            if (bggUrl != null || driveUrl != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (bggUrl != null) {
                            BoardFlowButton(
                                onClick = { open(bggUrl) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("  Open BGG")
                            }
                        }
                        if (driveUrl != null) {
                            BoardFlowOutlinedButton(
                                onClick = { open(driveUrl) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("  Drive")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Header content (GameBackdrop behind dialog handles the artwork) ───────────

@Composable
private fun HeaderSection(
    game: GameItem,
    headerChips: List<HeaderChip>,
    compactChips: Boolean
) {
    val hasBackdrop = !game.thumbnailUrl.isNullOrBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (hasBackdrop) {
            AsyncImage(
                model = game.thumbnailUrl,
                contentDescription = game.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(84.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.GridView,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = game.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (hasBackdrop) Color.White.copy(alpha = 0.95f)
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 20.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                game.rating?.let {
                    InlineStat(
                        icon = Icons.Default.Star,
                        label = formatDecimal(it),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                headerChips.forEach { chip ->
                    StatusChip(
                        label = chip.label,
                        icon = chip.icon,
                        tint = chip.tint,
                        iconOnly = compactChips
                    )
                }
            }
        }
    }
}

// ── Your Stats hero card ──────────────────────────────────────────────────────

@Composable
private fun YourStatsCard(
    stats: GameHistoryStats,
    insight: ContextualInsight?
) {
    val primary          = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = primary.copy(alpha = 0.04f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your Stats",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = primary.copy(alpha = 0.75f)
            )

            // Hero row — plays count + last played date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        text = "${stats.plays}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = primary
                    )
                    Text(
                        text = "plays",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                stats.lastPlayedDate?.let { date ->
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "last played",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Secondary stats grid
            val secondary = buildList {
                stats.avgDurationMinutes?.let { avg ->
                    add(SectionStat("Avg duration", if (avg >= 60) "${avg / 60}h ${avg % 60}m" else "${avg}m"))
                }
                stats.bestScore?.let { (name, score) -> add(SectionStat("Best score", "$score by $name")) }
                stats.mostWins?.let { (name, wins) -> add(SectionStat("Most wins", "$name ($wins)")) }
            }
            if (secondary.isNotEmpty()) {
                DetailGrid(stats = secondary, emphasizeSurface = false)
            }

            // Frequent partners
            if (stats.commonPlayers.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Frequent partners",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant.copy(alpha = 0.55f)
                    )
                    Text(
                        text = stats.commonPlayers.joinToString(" · ") { "${it.first} (${it.second})" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }

            // Contextual insight — single rotating line
            insight?.let {
                Text(
                    text = it.text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (it.positive) primary.copy(alpha = 0.85f)
                            else onSurfaceVariant.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ── Grouped info block ────────────────────────────────────────────────────────

@Composable
private fun InfoGroupBlock(sections: List<InfoSection>) {
    Surface(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            sections.forEachIndexed { i, section ->
                if (i > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                    DetailGrid(
                        stats = section.stats,
                        emphasizeSurface = false,
                        secondaryLabels = section.secondaryLabels,
                        columns = section.columns
                    )
                }
            }
        }
    }
}

// ── Sleeves block ─────────────────────────────────────────────────────────────

@Composable
private fun SleevesBlock(game: GameItem) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Sleeves",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                    if (!expanded) {
                        val groups = game.sleeveCardSets
                            .filter { it.size != null || it.count != null }
                            .groupBy { it.size?.trim().orEmpty() }
                        val totalCards = game.sleeveCardSets.mapNotNull { it.count }.sum()
                        when {
                            groups.isNotEmpty() && totalCards > 0 -> Text(
                                text = "${groups.size} ${if (groups.size == 1) "size" else "sizes"} · $totalCards cards",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            game.sleeveStatus == GameItem.SleeveStatus.MISSING -> Text(
                                text = "No data on BGG yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        SleevesSection(game)
                    }
                }
            }
        }
    }
}

// ── Status chip ───────────────────────────────────────────────────────────────

@Composable
private fun StatusChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    iconOnly: Boolean = false
) {
    Surface(color = tint.copy(alpha = 0.08f), shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(11.dp), tint = tint)
            if (!iconOnly) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint.copy(alpha = 0.9f)
                )
            }
        }
    }
}

// ── Detail grid ───────────────────────────────────────────────────────────────

@Composable
private fun DetailGrid(
    stats: List<SectionStat>,
    emphasizeSurface: Boolean = true,
    secondaryLabels: Set<String> = emptySet(),
    columns: Int = 2
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { stat ->
                    DetailCell(
                        label = stat.label,
                        value = stat.value,
                        emphasizeSurface = emphasizeSurface,
                        secondary = stat.label in secondaryLabels,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DetailCell(
    label: String,
    value: String,
    emphasizeSurface: Boolean,
    secondary: Boolean,
    modifier: Modifier = Modifier
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (emphasizeSurface) 10.dp else 2.dp,
                    vertical = if (emphasizeSurface) 9.dp else 3.dp
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (secondary) 0.5f else 0.58f
                )
            )
            Text(
                text = value,
                style = if (secondary) {
                    MaterialTheme.typography.labelMedium.withTabularNumbers()
                } else {
                    MaterialTheme.typography.bodyMedium.withTabularNumbers()
                },
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (secondary) 0.72f else 1f
                )
            )
        }
    }

    if (emphasizeSurface) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = MaterialTheme.shapes.medium
        ) { content() }
    } else {
        Box(modifier = modifier) { content() }
    }
}

// ── Sleeves expanded content ──────────────────────────────────────────────────

@Composable
private fun SleevesSection(game: GameItem) {
    val grouped = remember(game) {
        game.sleeveCardSets
            .filter { it.size != null || it.count != null }
            .groupBy { it.size?.trim().orEmpty() }
            .entries.sortedBy { it.key }
    }

    when {
        game.sleeveStatus == GameItem.SleeveStatus.MISSING ->
            Text(
                text = "No sleeve data on BGG yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

        game.sleeveStatus == GameItem.SleeveStatus.ERROR ->
            Text(
                text = game.sleeveNote ?: "Could not load sleeve data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
            )

        grouped.isEmpty() -> Unit

        else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            grouped.forEach { (size, sets) ->
                val total = sets.mapNotNull { it.count }.sum()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = size.ifBlank { "Unknown size" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f)
                    )
                    if (total > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "$total",
                                style = MaterialTheme.typography.labelSmall.withTabularNumbers(),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
