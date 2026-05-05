package cz.nicolsburg.boardflow.ui.collection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.nicolsburg.boardflow.model.GameItem
import cz.nicolsburg.boardflow.ui.common.BoardFlowIcons
import cz.nicolsburg.boardflow.ui.common.SectionCard
import cz.nicolsburg.boardflow.ui.common.withTabularNumbers

private data class GameSleevesEntry(val name: String, val count: Int)

private data class SleeveSizeGroup(
    val size: String,
    val totalCount: Int,
    val games: List<GameSleevesEntry>,
    val isUnknown: Boolean = false
)

private fun computeSleeveSummary(
    games: List<GameItem>,
    excludedGameIds: Set<String>
): List<SleeveSizeGroup> {
    val toSleeve = games.filter { game ->
        game.isOwned &&
            sheetSleeveStatus(game) == SheetSleeveStatus.TO_SLEEVE &&
            game.objectId !in excludedGameIds
    }

    val sizeGroups = LinkedHashMap<String, Pair<Int, LinkedHashMap<String, Int>>>()
    val noDataGames = LinkedHashSet<String>()

    for (game in toSleeve) {
        val cardSets = game.sleeveCardSets
        if (cardSets.isEmpty()) {
            noDataGames += game.name
            continue
        }

        val countsForThisGame = mutableMapOf<String, Int>()
        for (cs in cardSets) {
            val size = cs.size?.takeIf { it.isNotBlank() } ?: continue
            countsForThisGame[size] = (countsForThisGame[size] ?: 0) + (cs.count ?: 0)
        }

        if (countsForThisGame.isEmpty()) {
            noDataGames += game.name
            continue
        }

        for ((size, gameCount) in countsForThisGame) {
            val existing = sizeGroups[size]
            if (existing == null) {
                sizeGroups[size] = gameCount to linkedMapOf(game.name to gameCount)
            } else {
                sizeGroups[size] = (existing.first + gameCount) to existing.second.also {
                    it[game.name] = (it[game.name] ?: 0) + gameCount
                }
            }
        }
    }

    val knownGroups = sizeGroups.entries
        .map { (size, pair) ->
            SleeveSizeGroup(
                size = size,
                totalCount = pair.first,
                games = pair.second.map { (name, count) -> GameSleevesEntry(name, count) }
            )
        }
        .sortedByDescending { it.totalCount }

    return if (noDataGames.isNotEmpty()) {
        knownGroups + SleeveSizeGroup(
            size = "No size data",
            totalCount = 0,
            games = noDataGames.map { GameSleevesEntry(it, 0) },
            isUnknown = true
        )
    } else {
        knownGroups
    }
}

@Composable
internal fun SleevesContent(
    allGames: List<GameItem>,
    listState: LazyListState = rememberLazyListState(),
    excludedGameIds: Set<String> = emptySet(),
    onToggleExclusion: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val allGamesToSleeve = remember(allGames) {
        allGames
            .filter { game -> game.isOwned && sheetSleeveStatus(game) == SheetSleeveStatus.TO_SLEEVE }
            .sortedBy { it.name.lowercase() }
    }

    val groups = remember(allGames, excludedGameIds) { computeSleeveSummary(allGames, excludedGameIds) }
    val includedCount = remember(allGamesToSleeve, excludedGameIds) {
        allGamesToSleeve.count { it.objectId !in excludedGameIds }
    }

    var showGameSelector by remember { mutableStateOf(false) }

    if (allGamesToSleeve.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    BoardFlowIcons.Sleeves,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
                Text(
                    "All games are sleeved",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "header") {
            SleeveSummaryHeader(
                includedCount = includedCount,
                totalCount = allGamesToSleeve.size,
                sizesCount = groups.count { !it.isUnknown },
                expanded = showGameSelector,
                onToggleExpand = { showGameSelector = !showGameSelector }
            )
        }

        item(key = "game_selector") {
            AnimatedVisibility(visible = showGameSelector) {
                SectionCard {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Included in sleeve count",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        allGamesToSleeve.forEach { game ->
                            val excluded = game.objectId in excludedGameIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleExclusion(game.objectId) }
                                    .padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    game.name,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 2.dp),
                                    color = if (excluded)
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CompactSleeveCheckmark(checked = !excluded)
                            }
                        }
                    }
                }
            }
        }

        items(groups, key = { it.size }) { group ->
            SleeveSizeGroupCard(group = group)
        }
    }
}

@Composable
private fun CompactSleeveCheckmark(checked: Boolean) {
    Surface(
        modifier = Modifier.size(22.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (checked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (checked) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            }
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (checked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SleeveSummaryHeader(
    includedCount: Int,
    totalCount: Int,
    sizesCount: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit
) {
    SectionCard(accented = true, onClick = onToggleExpand) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    BoardFlowIcons.Sleeves,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "$includedCount ${if (includedCount == 1) "game" else "games"} to sleeve",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val subtitle = buildList {
                        if (sizesCount > 0) add("$sizesCount ${if (sizesCount == 1) "size" else "sizes"} needed")
                        if (includedCount < totalCount) add("${totalCount - includedCount} excluded")
                    }.joinToString("  ·  ")
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand game list",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SleeveSizeGroupCard(group: SleeveSizeGroup) {
    var expanded by remember { mutableStateOf(false) }

    SectionCard(accented = false, onClick = { expanded = !expanded }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    group.size,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (group.isUnknown) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    "${group.games.size} ${if (group.games.size == 1) "game" else "games"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!group.isUnknown && group.totalCount > 0) {
                    Text(
                        "x${group.totalCount}",
                        style = MaterialTheme.typography.titleLarge.withTabularNumbers(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                group.games.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "· ${entry.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!group.isUnknown && entry.count > 0) {
                            Text(
                                "×${entry.count}",
                                style = MaterialTheme.typography.bodySmall.withTabularNumbers(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
