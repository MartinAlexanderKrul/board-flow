package cz.nicolsburg.boardflow.ui.common

import cz.nicolsburg.boardflow.ui.theme.PlayerColors
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.Switch
import androidx.compose.animation.AnimatedVisibility
import cz.nicolsburg.boardflow.ui.theme.Spacing
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import cz.nicolsburg.boardflow.ui.theme.BoardFlowColors
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.model.PlayerResult
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerResultEditorCard(
    player: PlayerResult,
    rosterPlayers: List<Player>,
    onUpdate: (PlayerResult) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
    onToggleCollapsed: (() -> Unit)? = null,
    requestScoreFocus: Boolean = false,
    onFocusDone: () -> Unit = {},
    requestNameFocus: Boolean = false,
    onNameFocusDone: () -> Unit = {},
    // Inside a dialog the row sits on a raised surface and needs the next tone up.
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    val scoreFocusRequester = remember { FocusRequester() }
    val nameFocusRequester = remember { FocusRequester() }
    var nameFocused by remember { mutableStateOf(false) }
    val exactMatch = remember(rosterPlayers, player.name) { rosterPlayers.exactPlayerMatch(player.name) }
    val suggestedMatches = remember(rosterPlayers, player.name) {
        rosterPlayers.playerMatchSuggestions(player.name).filter { it.id != exactMatch?.id }
    }

    LaunchedEffect(requestNameFocus) {
        if (requestNameFocus) {
            delay(100)
            runCatching { nameFocusRequester.requestFocus() }
            onNameFocusDone()
        }
    }

    // Only rename the field when the user is not actively typing in it.
    LaunchedEffect(exactMatch?.id, nameFocused) {
        val match = exactMatch ?: return@LaunchedEffect
        if (!nameFocused && player.name.trim() != match.displayName) {
            onUpdate(player.copy(name = match.displayName))
        }
    }

    LaunchedEffect(requestScoreFocus) {
        if (requestScoreFocus) {
            delay(150)
            runCatching { scoreFocusRequester.requestFocus() }
            onFocusDone()
        }
    }

    // One line per player: who, did they win, what did they score. Everything else
    // (name, team, rating, first play, remove) is behind the chevron.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BoardFlowShape.Card,
        color = containerColor
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(start = Spacing.md, end = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(BoardFlowShape.Control)
                        .then(
                            if (onToggleCollapsed != null) Modifier.clickable(onClick = onToggleCollapsed)
                            else Modifier
                        )
                        .heightIn(min = Dimens.MinTouchTarget),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    PlayerAvatar(player.name.ifBlank { "?" }, size = 36.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = player.name.ifBlank { "New player" },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (player.name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val details = listOfNotNull(
                            player.color.trim().takeIf { it.isNotBlank() },
                            "First play".takeIf { player.isNew },
                            // BGG sends 0 or N/A for "not rated".
                            player.rating.trim().takeIf { (it.toDoubleOrNull() ?: 0.0) > 0.0 }?.let { "Rated $it" }
                        ).joinToString(" · ")
                        if (details.isNotBlank()) {
                            Text(
                                details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                BoardFlowIconButton(onClick = { onUpdate(player.copy(isWinner = !player.isWinner)) }) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Winner",
                        tint = if (player.isWinner) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.IconLarge)
                    )
                }
                Surface(
                    shape = BoardFlowShape.Control,
                    color = if (containerColor == MaterialTheme.colorScheme.surface) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    modifier = Modifier.size(width = 76.dp, height = 44.dp)
                ) {
                    // Saved players start at "0". Show that as the placeholder rather than as
                    // text, so typing 62 can never produce "062".
                    BoardFlowInlineField(
                        value = if (player.score == "0") "" else player.score,
                        // Scores are numbers; a hardware or pasted letter is dropped.
                        onValueChange = { typed ->
                            onUpdate(player.copy(score = typed.filter { it.isDigit() || it == '-' || it == '.' }))
                        },
                        placeholder = "0",
                        keyboardType = KeyboardType.Number,
                        textAlign = TextAlign.Center,
                        textStyle = MaterialTheme.typography.titleMedium.withTabularNumbers(),
                        textColor = if (player.isWinner) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.sm)
                            .semantics { contentDescription = "Score" }
                            .focusRequester(scoreFocusRequester)
                    )
                }
                if (onToggleCollapsed != null) {
                    BoardFlowIconButton(onClick = onToggleCollapsed) {
                        Icon(
                            if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (collapsed) "Expand player" else "Collapse player",
                            modifier = Modifier.size(Dimens.IconLarge)
                        )
                    }
                } else {
                    Spacer(Modifier.width(Spacing.sm))
                }
            }

            AnimatedVisibility(visible = !collapsed) {
                Column {
                    BoardFlowFormDivider()
                    BoardFlowFormRow(label = "Name") {
                        BoardFlowInlineField(
                            value = player.name,
                            onValueChange = { onUpdate(player.copy(name = it)) },
                            placeholder = "Player name",
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(nameFocusRequester)
                                .onFocusChanged { nameFocused = it.isFocused }
                        )
                        if (exactMatch != null) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Saved player",
                                modifier = Modifier.size(Dimens.Icon),
                                tint = BoardFlowColors.Success
                            )
                        }
                    }
                    if (suggestedMatches.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = Spacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            suggestedMatches.forEach { match ->
                                SuggestionChip(
                                    onClick = { onUpdate(player.copy(name = match.displayName)) },
                                    label = { Text(match.displayName, style = MaterialTheme.typography.labelLarge) },
                                    shape = BoardFlowShape.Pill
                                )
                            }
                        }
                    }
                    BoardFlowFormDivider()
                    BoardFlowFormRow(label = "Team") {
                        BoardFlowInlineField(
                            value = player.color,
                            onValueChange = { onUpdate(player.copy(color = it)) },
                            placeholder = "Colour or faction",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    BoardFlowFormDivider()
                    BoardFlowFormRow(label = "Rating") {
                        BoardFlowInlineField(
                            value = if ((player.rating.trim().toDoubleOrNull() ?: 1.0) == 0.0) "" else player.rating,
                            onValueChange = { onUpdate(player.copy(rating = it)) },
                            placeholder = "1 to 10",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    BoardFlowFormDivider()
                    BoardFlowFormRow(label = "First play", labelWidth = null) {
                        Switch(checked = player.isNew, onCheckedChange = { onUpdate(player.copy(isNew = it)) })
                    }
                    BoardFlowFormDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
                        horizontalArrangement = Arrangement.End
                    ) {
                        BoardFlowInlineAction(onClick = onRemove, icon = BoardFlowIcons.Delete, destructive = true) {
                            Text("Remove player", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerColorDot(colorName: String, modifier: Modifier = Modifier) {
    val parsed = PlayerColors.resolve(colorName)
    if (parsed != null) {
        Box(modifier = modifier.background(parsed, CircleShape))
    } else {
        Spacer(modifier = modifier)
    }
}

private fun List<Player>.exactPlayerMatch(input: String): Player? {
    val lower = input.trim().lowercase()
    if (lower.isBlank()) return null
    return firstOrNull { player ->
        (listOf(player.displayName) + player.aliases).any { it.trim().lowercase() == lower }
    }
}

private fun List<Player>.playerMatchSuggestions(input: String): List<Player> {
    val lower = input.trim().lowercase()
    if (lower.length < 2) return emptyList()
    val threshold = maxOf(2, lower.length / 3)
    return map { player ->
        val bestDistance = (listOf(player.displayName) + player.aliases)
            .minOf { levenshtein(lower, it.trim().lowercase()) }
        player to bestDistance
    }
        .filter { (_, distance) -> distance <= threshold }
        .sortedBy { (_, distance) -> distance }
        .map { (player, _) -> player }
        .take(5)
}

private fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            )
        }
    }
    return dp[a.length][b.length]
}
