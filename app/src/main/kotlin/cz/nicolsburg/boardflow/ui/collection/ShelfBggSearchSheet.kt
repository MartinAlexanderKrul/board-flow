package cz.nicolsburg.boardflow.ui.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.SyncViewModel
import cz.nicolsburg.boardflow.model.BggGame
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowModalBottomSheet
import cz.nicolsburg.boardflow.ui.common.GameListRow
import cz.nicolsburg.boardflow.ui.theme.Spacing

/** The row under the My Shelf results that searches BoardGameGeek for the typed name. */
@Composable
internal fun SearchBggRow(query: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BoardFlowInlineAction(onClick = onClick, icon = Icons.Default.Search) {
            Text("Search BoardGameGeek for \"${query.trim()}\"")
        }
    }
}

/**
 * BoardGameGeek results for the shelf search. A game already on the shelf says so; tapping any
 * result opens its detail dialog (a new game opens on its collection status, to add it).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShelfBggSearchSheet(
    search: SyncViewModel.ShelfBggSearch,
    shelfIds: Set<String>,
    onOpen: (BggGame) -> Unit,
    onDismiss: () -> Unit
) {
    BoardFlowModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text("Add to your collection", style = MaterialTheme.typography.titleLarge)
                Text(
                    "BoardGameGeek results for \"${search.query}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                search.loading -> Box(Modifier.fillMaxWidth().heightIn(min = 160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                search.results.isEmpty() -> Box(Modifier.fillMaxWidth().padding(Spacing.xl), contentAlignment = Alignment.Center) {
                    Text(
                        search.error ?: "Nothing found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(search.results, key = { it.id }) { game ->
                        val onShelf = game.id.toString() in shelfIds
                        GameListRow(
                            name = game.name,
                            thumbnailUrl = game.thumbnailUrl,
                            supporting = listOfNotNull(
                                game.yearPublished?.takeIf { it.isNotBlank() },
                                "In your collection".takeIf { onShelf }
                            ).joinToString(" - ").ifBlank { null },
                            enabled = search.openingGameId == null,
                            onClick = { onOpen(game) }
                        ) {
                            if (search.openingGameId == game.id) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}
