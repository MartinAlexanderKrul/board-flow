package cz.nicolsburg.boardflow.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.model.BggGame
import cz.nicolsburg.boardflow.model.ExpansionPlays
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.Spacing

/**
 * "Add expansion" in Log Play and Edit play: the expansions BGG lists for the game, owned ones
 * first, with a filter. Tapping one adds or removes it and keeps the sheet open, so several can
 * be picked. [load] returns null when the list is not available (offline and never fetched).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpansionPickerSheet(
    gameName: String,
    load: suspend () -> List<BggGame>?,
    ownedIds: Set<Int>,
    selectedIds: Set<Int>,
    onToggle: (BggGame) -> Unit,
    onDismiss: () -> Unit
) {
    var expansions by remember { mutableStateOf<List<BggGame>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        expansions = load()
        loading = false
    }
    val shown = expansions.orEmpty().filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }

    BoardFlowModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Text(
            "Add expansion",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.lg)
        )
        Text(
            "Played with $gameName, same players and scores",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)
        )
        when {
            loading -> Box(Modifier.fillMaxWidth().padding(Spacing.xl), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            expansions == null -> Text(
                "Could not get the expansions from BoardGameGeek. Try again when you're online.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.lg)
            )
            expansions!!.isEmpty() -> Text(
                "BoardGameGeek lists no expansions for $gameName.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.lg)
            )
            else -> {
                if (expansions!!.size > 8) {
                    BoardFlowTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Filter expansions") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    contentPadding = PaddingValues(bottom = Spacing.lg)
                ) {
                    items(shown, key = { it.id }) { expansion ->
                        val selected = expansion.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(expansion) }
                                .heightIn(min = Dimens.MinTouchTarget)
                                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Text(
                                ExpansionPlays.shortName(expansion.name, gameName),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (expansion.id in ownedIds) {
                                Text("Owned", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = "Added", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Dimens.Icon))
                            }
                        }
                    }
                }
            }
        }
    }
}
