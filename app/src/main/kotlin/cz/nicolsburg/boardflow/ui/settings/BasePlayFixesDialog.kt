package cz.nicolsburg.boardflow.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.AppViewModel
import cz.nicolsburg.boardflow.model.BasePlayFix
import cz.nicolsburg.boardflow.model.BggGame
import cz.nicolsburg.boardflow.ui.common.AnimatedDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowCard
import cz.nicolsburg.boardflow.ui.common.BoardFlowEmptyState
import cz.nicolsburg.boardflow.ui.common.BoardFlowErrorBanner
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowPickerField
import cz.nicolsburg.boardflow.ui.common.BoardFlowPickerSheet
import cz.nicolsburg.boardflow.ui.common.formatDisplayDate
import cz.nicolsburg.boardflow.ui.theme.Spacing

/**
 * Settings > Data > Add missing base games: lists expansion plays logged without their base
 * game, lets the user tick them and pick the base where BGG names several, then adds the plays.
 */
@Composable
fun BasePlayFixesDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onDone: (Int) -> Unit
) {
    var fixes by remember { mutableStateOf<List<BasePlayFix>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val selected = remember { mutableStateListOf<Boolean>() }
    val bases = remember { mutableStateListOf<BggGame?>() }
    var pickerIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.findBasePlayFixes()
            .onSuccess { found ->
                fixes = found
                selected.addAll(found.map { it.selectedByDefault })
                bases.addAll(found.map { it.suggestedBase })
            }
            .onFailure { error = it.message ?: "Could not reach BoardGameGeek" }
    }

    val chosen = fixes.orEmpty().indices.filter { selected.getOrElse(it) { false } }
    val ready = chosen.isNotEmpty() && chosen.all { fixes!![it].existingBasePlay != null || bases[it] != null }

    AnimatedDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.lg)) {
            Text("Add missing base games", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Expansion plays logged without their base game. Each one gets a base-game play with the same date, players and scores.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.md)
            )
            val list = fixes
            when {
                error != null -> BoardFlowErrorBanner(message = error!!)
                list == null -> Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text("Checking your plays on BoardGameGeek...", style = MaterialTheme.typography.bodyMedium)
                }
                list.isEmpty() -> BoardFlowEmptyState(icon = Icons.Default.Extension, title = "Nothing to add", message = "Every expansion play has its base game.")
                else -> LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(bottom = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    itemsIndexed(list, key = { _, fix -> fix.expansionPlay.id }) { index, fix ->
                        FixRow(
                            fix = fix,
                            checked = selected[index],
                            base = bases[index],
                            onCheckedChange = { selected[index] = it },
                            onPickBase = { pickerIndex = index }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoardFlowInlineAction(onClick = onDismiss, neutral = true, large = true) { Text("Cancel") }
                BoardFlowButton(
                    enabled = ready,
                    onClick = {
                        val picked = chosen.map { fixes!![it] to bases[it] }
                        viewModel.applyBasePlayFixes(picked, onDone)
                        onDismiss()
                    }
                ) { Text(if (chosen.isEmpty()) "Add" else "Add ${chosen.size}") }
            }
        }

        // The sheet sits inside the dialog's content, or its window opens behind the dialog.
        pickerIndex?.let { index ->
            val fix = fixes?.getOrNull(index)
            if (fix != null) {
                BoardFlowPickerSheet(
                    title = "Base game for ${fix.expansionPlay.gameName}",
                    options = fix.baseOptions,
                    selectedOption = bases[index] ?: fix.baseOptions.first(),
                    optionLabel = { it.name },
                    onSelect = { bases[index] = it; selected[index] = true; pickerIndex = null },
                    onDismiss = { pickerIndex = null }
                )
            }
        }
    }
}

@Composable
private fun FixRow(
    fix: BasePlayFix,
    checked: Boolean,
    base: BggGame?,
    onCheckedChange: (Boolean) -> Unit,
    onPickBase: () -> Unit
) {
    val play = fix.expansionPlay
    BoardFlowCard(contentPadding = PaddingValues(Spacing.sm), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Column(modifier = Modifier.weight(1f).padding(top = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(play.gameName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(formatDisplayDate(play.date), play.players.joinToString { it.name.trim().substringBefore(' ') }.ifBlank { "No players" })
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val existing = fix.existingBasePlay
                if (existing != null) {
                    Text(
                        "${existing.gameName} was logged that day with no result: it gets this play's length, scores and winner",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Box(modifier = Modifier.padding(top = Spacing.xs, end = Spacing.sm)) {
                        BoardFlowPickerField(label = "Base game", value = base?.name ?: "Choose", expanded = false, onClick = onPickBase)
                    }
                }
            }
        }
    }
}
