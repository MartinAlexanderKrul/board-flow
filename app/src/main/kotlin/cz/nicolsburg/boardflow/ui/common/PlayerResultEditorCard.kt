package cz.nicolsburg.boardflow.ui.common

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    requestScoreFocus: Boolean = false,
    onFocusDone: () -> Unit = {}
) {
    val scoreFocusRequester = remember { FocusRequester() }
    val exactMatch = remember(rosterPlayers, player.name) { rosterPlayers.exactPlayerMatch(player.name) }
    val suggestedMatches = remember(rosterPlayers, player.name) {
        rosterPlayers.playerMatchSuggestions(player.name)
            .filter { it.id != exactMatch?.id }
    }

    LaunchedEffect(requestScoreFocus) {
        if (requestScoreFocus) {
            delay(150)
            runCatching { scoreFocusRequester.requestFocus() }
            onFocusDone()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = BoardFlowSurfaceTokens.Shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                PlayerField(label = "Name", modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = player.name,
                        onValueChange = { onUpdate(player.copy(name = it)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                BoardFlowIconButton(onClick = { onUpdate(player.copy(isWinner = !player.isWinner)) }) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Toggle winner",
                        tint = if (player.isWinner) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    )
                }
            }

            if (exactMatch != null || suggestedMatches.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    exactMatch?.let { match ->
                        SuggestionChip(
                            onClick = { onUpdate(player.copy(name = match.displayName)) },
                            label = { Text("Matched ${match.displayName}", style = MaterialTheme.typography.labelMedium) },
                            icon = {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                    suggestedMatches.forEach { match ->
                        SuggestionChip(
                            onClick = { onUpdate(player.copy(name = match.displayName)) },
                            label = { Text(match.displayName, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                PlayerField(label = "Score", modifier = Modifier.width(108.dp)) {
                    OutlinedTextField(
                        value = player.score,
                        onValueChange = { onUpdate(player.copy(score = it)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(scoreFocusRequester)
                    )
                }
                PlayerField(label = "Team / color", modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = player.color,
                        onValueChange = { onUpdate(player.copy(color = it)) },
                        singleLine = true,
                        leadingIcon = {
                            if (player.color.isNotBlank()) {
                                PlayerColorDot(colorName = player.color, modifier = Modifier.size(12.dp))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            PlayerField(label = "Rating") {
                OutlinedTextField(
                    value = player.rating,
                    onValueChange = { onUpdate(player.copy(rating = it)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = player.isNew,
                        onCheckedChange = { onUpdate(player.copy(isNew = it)) }
                    )
                    Text(
                        "First play",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BoardFlowIconButton(onClick = onRemove) {
                    Icon(
                        BoardFlowIcons.Delete,
                        contentDescription = "Remove player",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        content()
    }
}

@Composable
private fun PlayerColorDot(colorName: String, modifier: Modifier = Modifier) {
    val knownColors = mapOf(
        "red" to Color(0xFFE53935),
        "blue" to Color(0xFF1E88E5),
        "green" to Color(0xFF43A047),
        "yellow" to Color(0xFFFDD835),
        "orange" to Color(0xFFFB8C00),
        "purple" to Color(0xFF8E24AA),
        "white" to Color(0xFFF5F5F5),
        "black" to Color(0xFF212121),
        "pink" to Color(0xFFE91E63),
        "brown" to Color(0xFF6D4C41),
        "gray" to Color(0xFF757575),
        "grey" to Color(0xFF757575),
        "cyan" to Color(0xFF00ACC1),
        "teal" to Color(0xFF00897B),
        "lime" to Color(0xFF7CB342)
    )
    val parsed = knownColors[colorName.lowercase().trim()]
        ?: runCatching { Color(android.graphics.Color.parseColor(colorName)) }.getOrNull()
    if (parsed != null) {
        Box(
            modifier = modifier.background(parsed, CircleShape)
        )
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
