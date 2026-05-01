package cz.nicolsburg.boardflow.ui.collection

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

private fun computeSleeveSummary(games: List<GameItem>): List<SleeveSizeGroup> {
    val toSleeve = games.filter { game ->
        game.isOwned && sheetSleeveStatus(game) == SheetSleeveStatus.TO_SLEEVE
    }

    // size → (total count across all games, game name → count for that game)
    val sizeGroups = LinkedHashMap<String, Pair<Int, LinkedHashMap<String, Int>>>()
    val noDataGames = LinkedHashSet<String>()

    for (game in toSleeve) {
        val cardSets = game.sleeveCardSets
        if (cardSets.isEmpty()) {
            noDataGames += game.name
            continue
        }

        // Aggregate counts per size for this game first (multiple card sets may share a size)
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
    modifier: Modifier = Modifier
) {
    val groups = remember(allGames) { computeSleeveSummary(allGames) }
    val gamesCount = remember(allGames) {
        allGames.count { game ->
            game.isOwned && sheetSleeveStatus(game) == SheetSleeveStatus.TO_SLEEVE
        }
    }

    if (groups.isEmpty()) {
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
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SleeveSummaryHeader(
                gamesCount = gamesCount,
                sizesCount = groups.count { !it.isUnknown }
            )
        }

        items(groups, key = { it.size }) { group ->
            SleeveSizeGroupCard(group = group)
        }
    }
}

@Composable
private fun SleeveSummaryHeader(gamesCount: Int, sizesCount: Int) {
    SectionCard(accented = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                BoardFlowIcons.Sleeves,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "$gamesCount ${if (gamesCount == 1) "game" else "games"} to sleeve",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (sizesCount > 0) {
                    Text(
                        "$sizesCount sleeve ${if (sizesCount == 1) "size" else "sizes"} needed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SleeveSizeGroupCard(group: SleeveSizeGroup) {
    var expanded by remember { mutableStateOf(false) }

    SectionCard(accented = false) {
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
                        "×${group.totalCount}",
                        style = MaterialTheme.typography.titleLarge.withTabularNumbers(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
