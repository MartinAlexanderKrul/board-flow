package cz.nicolsburg.boardflow.ui.common

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.model.PlayerResult
import kotlinx.coroutines.delay

private val PlayerCardShape = RoundedCornerShape(22.dp)
private val CompactFieldShape = RoundedCornerShape(14.dp)

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
    onFocusDone: () -> Unit = {}
) {
    val scoreFocusRequester = remember { FocusRequester() }
    val exactMatch = remember(rosterPlayers, player.name) { rosterPlayers.exactPlayerMatch(player.name) }
    val suggestedMatches = remember(rosterPlayers, player.name) {
        rosterPlayers.playerMatchSuggestions(player.name).filter { it.id != exactMatch?.id }
    }
    val winnerTint = MaterialTheme.colorScheme.primary
    val winnerContainer = winnerTint.copy(alpha = 0.12f)

    LaunchedEffect(requestScoreFocus) {
        if (requestScoreFocus) {
            delay(150)
            runCatching { scoreFocusRequester.requestFocus() }
            onFocusDone()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (collapsed && onToggleCollapsed != null) Modifier.clickable(onClick = onToggleCollapsed)
                else Modifier
            ),
        shape = PlayerCardShape,
        color = if (player.isWinner) winnerTint.copy(alpha = 0.06f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        border = BorderStroke(
            1.dp,
            if (player.isWinner) winnerTint.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (collapsed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = player.name.ifBlank { "Unnamed player" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (onToggleCollapsed != null) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = "Expand player",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 8.dp)
                            .size(18.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    CompactTonalTextField(
                        value = player.name,
                        onValueChange = { onUpdate(player.copy(name = it)) },
                        label = "Name",
                        modifier = Modifier.weight(1f)
                    )

                    if (onToggleCollapsed != null) {
                        BoardFlowIconButton(
                            onClick = onToggleCollapsed,
                            modifier = Modifier.padding(top = 4.dp),
                            colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                            )
                        ) {
                            Icon(
                                Icons.Default.ExpandLess,
                                contentDescription = "Collapse player",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    BoardFlowIconButton(
                        onClick = onRemove,
                        modifier = Modifier.padding(top = 4.dp),
                        colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
                        )
                    ) {
                        Icon(
                            BoardFlowIcons.Delete,
                            contentDescription = "Remove player",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                exactMatch?.let { match ->
                    MatchedPlayerChip(name = match.displayName)
                }

                if (suggestedMatches.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedMatches.forEach { match ->
                            SuggestionChip(
                                onClick = { onUpdate(player.copy(name = match.displayName)) },
                                label = { Text(match.displayName, style = MaterialTheme.typography.labelMedium) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f))
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SmallFieldLabel("Score")
                        CompactTonalTextField(
                            value = player.score,
                            onValueChange = { onUpdate(player.copy(score = it)) },
                            label = "Score",
                            modifier = Modifier.focusRequester(scoreFocusRequester),
                            keyboardType = KeyboardType.Number,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }
                    WinnerChip(
                        selected = player.isWinner,
                        onClick = { onUpdate(player.copy(isWinner = !player.isWinner)) }
                    )
                    FirstPlayChip(
                        checked = player.isNew,
                        onCheckedChange = { onUpdate(player.copy(isNew = it)) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SmallFieldLabel("Team")
                        CompactTonalTextField(
                            value = player.color,
                            onValueChange = { onUpdate(player.copy(color = it)) },
                            label = "Team",
                            placeholder = "Color / faction",
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(0.52f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SmallFieldLabel("Rating")
                        CompactTonalTextField(
                            value = player.rating,
                            onValueChange = { onUpdate(player.copy(rating = it)) },
                            label = "Rating",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardType = KeyboardType.Number,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WinnerChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = MaterialTheme.colorScheme.primary
    TogglePill(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        selectedTint = tint
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = "Winner",
            modifier = Modifier.size(16.dp),
            tint = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        )
        Text(
            "Winner",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MatchedPlayerChip(name: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Matched $name",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }
    }
}

@Composable
private fun FirstPlayChip(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = MaterialTheme.colorScheme.primary
    TogglePill(
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        selectedTint = tint
    ) {
        Icon(
            Icons.Default.FiberNew,
            contentDescription = "First play",
            modifier = Modifier.size(16.dp),
            tint = if (checked) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        )
        Text(
            "First play",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = if (checked) tint else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TogglePill(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTint: Color,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) selectedTint.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
        border = BorderStroke(
            1.dp,
            if (selected) selectedTint.copy(alpha = 0.32f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)
        )
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

@Composable
private fun CompactTonalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
    showLabel: Boolean = false,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    trailingContent: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = CompactFieldShape,
        textStyle = textStyle,
        label = if (showLabel) {
            {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                )
            }
        } else null,
        placeholder = (placeholder ?: if (showLabel) null else label)?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = if (showLabel) 12.sp else 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
        },
        trailingIcon = trailingContent,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f),
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f),
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        ),
    modifier = modifier.height(if (singleLine) 52.dp else 92.dp)
    )
}

@Composable
private fun SmallFieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
    )
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
