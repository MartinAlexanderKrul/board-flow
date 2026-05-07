package cz.nicolsburg.boardflow.ui.collection

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import cz.nicolsburg.boardflow.data.SecurePreferences
import cz.nicolsburg.boardflow.model.GameItem
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.ui.common.AnimatedDialog
import cz.nicolsburg.boardflow.ui.common.GameBackdrop
import cz.nicolsburg.boardflow.ui.common.withTabularNumbers
import cz.nicolsburg.boardflow.ui.history.ContextualInsightStrip
import cz.nicolsburg.boardflow.ui.history.GameHistoryStats
import cz.nicolsburg.boardflow.ui.history.gameContextualInsight
import cz.nicolsburg.boardflow.ui.history.gameHistoryStats

private data class InfoSection(
    val title: String,
    val stats: List<SectionStat>,
    val secondaryLabels: Set<String> = emptySet(),
    val columns: Int = 2
)

private object GameDetailTokens {
    val CardPadding = 14.dp
    val CardCorner = RoundedCornerShape(18.dp)
    const val CardBorderAlpha = 0.12f
    const val NeutralCardAlpha = 0.04f
    const val FeaturedCardAlpha = 0.045f
}

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
    val securePreferences = remember(context) { SecurePreferences(context.applicationContext) }
    val bggUrl = bggSleevesUrl(game)
    val driveUrl = game.shareUrl?.takeIf { it.isNotBlank() }
    val hasExternalButtons = bggUrl != null || driveUrl != null
    val gameObjectId = remember(game) { game.objectId.toIntOrNull()?.takeIf { it > 0 } }
    val overviewStats = remember(game) { overviewStats(game) }
    val ratingStats = remember(game) { ratingStats(game) }
    val playerStats = remember(game) { playerPreferenceStats(game) }
    val customRows = remember(game) { customDetailRows(game) }
    val myStats = remember(game, historyPlays, players) {
        if (gameObjectId != null) historyPlays.gameHistoryStats(gameObjectId, players)
        else historyPlays.gameHistoryStats(game.identity.name, players)
    }
    val contextualInsight = remember(game, historyPlays, players) {
        gameObjectId?.let { historyPlays.gameContextualInsight(it, players, prefs = securePreferences) }
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val headerChips = remember(game, primaryColor, secondaryColor) {
        headerStatusChips(game, primaryColor, secondaryColor)
    }
    val compactChips = headerChips.size > 2 || LocalConfiguration.current.screenWidthDp < 380
    val infoSections = remember(playerStats, overviewStats, ratingStats) {
        buildList {
            if (playerStats.isNotEmpty()) add(InfoSection("Players", playerStats.map(::polishGameDetailStat)))
            if (overviewStats.isNotEmpty()) add(InfoSection("Overview", overviewStats.map(::polishGameDetailStat)))
            if (ratingStats.isNotEmpty()) {
                add(InfoSection("Ratings", ratingStats.map(::polishGameDetailStat), setOf("Rank"), columns = 3))
            }
        }
    }
    val hasSleeves = game.sleeveStatus != GameItem.SleeveStatus.UNKNOWN || game.sleeveCardSets.isNotEmpty()
    val listState = rememberLazyListState()
    val headerCollapse by remember {
        derivedStateOf {
            when {
                listState.firstVisibleItemIndex > 0 -> 1f
                else -> (listState.firstVisibleItemScrollOffset / 200f).coerceIn(0f, 1f)
            }
        }
    }
    val compactHeaderAlpha by remember {
        derivedStateOf { ((headerCollapse - 0.12f) / 0.88f).coerceIn(0f, 1f) }
    }

    fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    AnimatedDialog(
        onDismissRequest = onDismiss,
        backdrop = {
            GameBackdrop(
                imageUrl = game.thumbnailUrl,
                height = 200.dp,
                titleFadeAlpha = 0.34f,
                contentFadeAlpha = 0.82f,
                bottomSurfaceBlendStart = 0.78f,
                collapseFraction = headerCollapse
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    HeaderSection(game, headerChips, compactChips, headerCollapse)
                }

                if (gameObjectId != null) {
                    item {
                        val hasHistory = myStats != null
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (hasHistory) {
                                DialogPrimaryActionButton(
                                    onClick = onLogPlay,
                                    modifier = Modifier.weight(1.08f)
                                ) {
                                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Log Play")
                                }
                                DialogSecondaryActionButton(
                                    onClick = { onViewHistory(gameObjectId) },
                                    modifier = Modifier.weight(0.92f)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("History")
                                }
                            } else {
                                DialogPrimaryActionButton(
                                    onClick = onLogPlay,
                                    modifier = if (hasExternalButtons) Modifier.weight(0.62f) else Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Log Play")
                                }
                                if (hasExternalButtons) Spacer(Modifier.weight(0.38f))
                            }
                        }
                    }
                }

                if (myStats != null && contextualInsight != null) {
                    item {
                        ContextualInsightStrip(
                            insight = contextualInsight,
                            ambient = true,
                            modifier = Modifier.alpha(1f - 0.18f * headerCollapse)
                        )
                    }
                }

                if (myStats != null) {
                    item { YourStatsCard(stats = myStats) }
                }

                if (infoSections.isNotEmpty()) {
                    item { InfoGroupBlock(infoSections) }
                }

                if (hasSleeves) {
                    item { SleevesBlock(game) }
                }

                if (customRows.isNotEmpty()) {
                    item { InfoGroupBlock(listOf(InfoSection("More", customRows))) }
                }

                if (bggUrl != null || driveUrl != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.025f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (bggUrl != null) {
                                    DialogUtilityActionButton(
                                        onClick = { open(bggUrl) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Open BGG")
                                    }
                                }
                                if (driveUrl != null) {
                                    DialogUtilityActionButton(
                                        onClick = { open(driveUrl) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Drive")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CompactStickyHeader(
                game = game,
                alpha = compactHeaderAlpha,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun HeaderSection(
    game: GameItem,
    headerChips: List<HeaderChip>,
    compactChips: Boolean,
    collapse: Float = 0f
) {
    val hasBackdrop = !game.thumbnailUrl.isNullOrBlank()
    val expandedThumb = 84.dp
    val collapsedThumb = 46.dp
    val thumbSize = expandedThumb - (expandedThumb - collapsedThumb) * collapse
    val chipAlpha = (1f - collapse * 1.15f).coerceIn(0f, 1f)

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
                    .size(thumbSize)
                    .clip(MaterialTheme.shapes.medium)
                    .alpha(1f - 0.22f * collapse)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(thumbSize)
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
            modifier = Modifier
                .weight(1f)
                .alpha(1f - 0.08f * collapse),
            verticalArrangement = Arrangement.spacedBy(5.dp)
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
                modifier = Modifier.alpha(chipAlpha),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                game.rating?.let {
                    InlineStat(
                        icon = Icons.Default.Star,
                        label = formatDecimal(it),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
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

// â”€â”€ Your Stats card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YourStatsCard(
    stats: GameHistoryStats
) {
    val primary          = MaterialTheme.colorScheme.primary
    val onSurface        = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant   = MaterialTheme.colorScheme.outlineVariant

    Surface(
        color = primary.copy(alpha = GameDetailTokens.FeaturedCardAlpha),
        shape = GameDetailTokens.CardCorner,
        border = BorderStroke(0.5.dp, outlineVariant.copy(alpha = GameDetailTokens.CardBorderAlpha)),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = GameDetailTokens.CardPadding, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Section label with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.QueryStats,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = primary.copy(alpha = 0.72f)
                )
                Text(
                    text = "Your Stats",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = primary.copy(alpha = 0.72f)
                )
            }

            // Primary â€” plays count (brightest) + secondary â€” last played date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "${stats.plays}",
                        style = MaterialTheme.typography.displaySmall,
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
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Last played",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant.copy(alpha = 0.55f)
                        )
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = onSurface.copy(alpha = 0.84f)
                        )
                    }
                }
            }

            // Tertiary stats grid
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

            // Frequent partners â€” pill chips
            if (stats.commonPlayers.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Frequent partners",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant.copy(alpha = 0.55f)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stats.commonPlayers.forEach { (name, count) ->
                            Surface(
                                shape = CircleShape,
                                color = onSurface.copy(alpha = 0.045f),
                                border = BorderStroke(0.5.dp, outlineVariant.copy(alpha = 0.14f))
                            ) {
                                Text(
                                    text = "${compactPartnerName(name)}  $count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onSurface.copy(alpha = 0.82f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// â”€â”€ Grouped info block â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun InfoGroupBlock(sections: List<InfoSection>) {
    Surface(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
        shape = GameDetailTokens.CardCorner,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            sections.forEachIndexed { i, section ->
                if (i > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                        modifier = Modifier.padding(horizontal = GameDetailTokens.CardPadding)
                    )
                }
                Column(
                    modifier = Modifier.padding(horizontal = GameDetailTokens.CardPadding, vertical = 9.dp),
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

// â”€â”€ Sleeves block â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun SleevesBlock(game: GameItem) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = GameDetailTokens.NeutralCardAlpha),
        shape = GameDetailTokens.CardCorner,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = GameDetailTokens.CardBorderAlpha)),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = GameDetailTokens.CardPadding, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Style,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                        Text(
                            text = "Sleeves",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
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
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                        modifier = Modifier.padding(horizontal = GameDetailTokens.CardPadding)
                    )
                    Box(
                        modifier = Modifier
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                            .padding(horizontal = GameDetailTokens.CardPadding, vertical = 9.dp)
                    ) {
                        SleevesSection(game)
                    }
                }
            }
        }
    }
}

// â”€â”€ Status chip â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun StatusChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    iconOnly: Boolean = false
) {
    Surface(color = tint.copy(alpha = 0.05f), shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = tint.copy(alpha = 0.84f)
            )
            if (!iconOnly) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint.copy(alpha = 0.82f)
                )
            }
        }
    }
}

// â”€â”€ Detail grid â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
                    alpha = if (secondary) 0.76f else 0.84f
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

// â”€â”€ Sleeves expanded content â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
            grouped.forEachIndexed { index, (size, sets) ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f),
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
                val total = sets.mapNotNull { it.count }.sum()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
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
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "$total",
                                style = MaterialTheme.typography.labelSmall.withTabularNumbers(),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun compactPartnerName(name: String): String {
    val trimmed = name.trim()
    if (trimmed.length <= 16) return trimmed
    val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.size >= 2) {
        val shortened = "${parts.first()} ${parts.last().first()}."
        if (shortened.length <= 16) return shortened
        return parts.first()
    }
    return trimmed.take(15) + "..."
}

@Composable
private fun CompactStickyHeader(
    game: GameItem,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0f) return
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f * alpha),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f * alpha)),
        modifier = modifier.alpha(alpha)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!game.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = game.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }
            Text(
                text = game.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun polishGameDetailStat(stat: SectionStat): SectionStat = when (stat.label) {
    "Best for" -> SectionStat(stat.label, "Excellent at ${stat.value}")
    "Recommended for" -> SectionStat(stat.label, "Great with ${stat.value}")
    "Weight" -> SectionStat(stat.label, strategyWeightLabel(stat.value))
    else -> stat
}

private fun strategyWeightLabel(raw: String): String {
    val value = raw.toDoubleOrNull() ?: return raw
    return when {
        value < 1.8 -> "Light strategy"
        value < 2.5 -> "Medium-light strategy"
        value < 3.2 -> "Medium-heavy strategy"
        value < 4.0 -> "Heavy strategy"
        else -> "Very heavy strategy"
    }
}

@Composable
private fun DialogPrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun DialogSecondaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun DialogUtilityActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
            contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.84f)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

