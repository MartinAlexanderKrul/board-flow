package cz.nicolsburg.boardflow.ui.players

import cz.nicolsburg.boardflow.ui.theme.PlayerColors
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import cz.nicolsburg.boardflow.ui.common.playerInitialColor
import cz.nicolsburg.boardflow.ui.common.parsePlayerColor
import cz.nicolsburg.boardflow.ui.common.PlayerColorChoices
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.BorderStroke
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.theme.Spacing
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import cz.nicolsburg.boardflow.ui.common.BoardFlowSectionTitle
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineField
import cz.nicolsburg.boardflow.ui.common.BoardFlowIconButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormRow
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormGroup
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.nicolsburg.boardflow.AppViewModel
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.ui.common.AnimatedDialog
import cz.nicolsburg.boardflow.ui.history.recentPlaysForPlayer
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationKind
import cz.nicolsburg.boardflow.ui.common.BoardFlowCloseGlyph
import cz.nicolsburg.boardflow.ui.common.BoardFlowDestructiveButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowIcons
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import cz.nicolsburg.boardflow.ui.common.PlayerAvatar
import cz.nicolsburg.boardflow.ui.common.SectionCard
import cz.nicolsburg.boardflow.ui.common.withTabularNumbers
import cz.nicolsburg.boardflow.ui.history.RivalryStat
import cz.nicolsburg.boardflow.ui.history.playerCurrentWinStreak
import cz.nicolsburg.boardflow.ui.history.rivalriesForPlayer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal data class PlayerStats(
    val totalPlays: Int,
    val wins: Int,
    val lastPlayedDate: String?,
    val favoriteGame: String?,
    val favoriteGameId: Int? = null,
    val currentWinStreak: Int = 0
)

internal val PlayerStats.winRate: Int
    get() = if (totalPlays > 0) kotlin.math.round(wins * 100f / totalPlays).toInt() else 0

internal fun List<LoggedPlay>.statsForPlayer(player: Player): PlayerStats {
    val names = (listOf(player.displayName) + player.aliases).map { it.lowercase().trim() }
    val myPlays = filter { play -> play.players.any { it.name.lowercase().trim() in names } }
    val wins = myPlays.count { play -> play.players.any { it.name.lowercase().trim() in names && it.isWinner } }
    val lastDate = myPlays.maxOfOrNull { it.date }?.let { formatPlayDate(it) }
    val favEntry = myPlays
        .groupBy { it.gameId }
        .mapValues { (_, plays) ->
            val name = plays.maxByOrNull { it.date }?.gameName ?: plays.first().gameName
            name to plays.sumOf { it.quantity.coerceAtLeast(1) }
        }
        .maxByOrNull { it.value.second }
    val streak = playerCurrentWinStreak(names)
    return PlayerStats(
        totalPlays = myPlays.size,
        wins = wins,
        lastPlayedDate = lastDate,
        favoriteGame = favEntry?.value?.first,
        favoriteGameId = favEntry?.key,
        currentWinStreak = streak
    )
}

private fun List<LoggedPlay>.lastPlayedDateForPlayer(player: Player): LocalDate? {
    val names = (listOf(player.displayName) + player.aliases).map { it.lowercase().trim() }
    return filter { play -> play.players.any { it.name.lowercase().trim() in names } }
        .mapNotNull { play -> runCatching { LocalDate.parse(play.date) }.getOrNull() }
        .maxOrNull()
}

private fun List<Player>.sortedByRecentActivity(sourcePlays: List<LoggedPlay>): List<Player> =
    sortedWith(
        compareByDescending<Player> { sourcePlays.lastPlayedDateForPlayer(it) ?: LocalDate.MIN }
            .thenBy { it.displayName.lowercase() }
    )

internal fun formatPlayDate(yyyyMMdd: String): String = try {
    LocalDate.parse(yyyyMMdd).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
} catch (_: Exception) { yyyyMMdd }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PlayerListItem(player: Player, stats: PlayerStats, onTap: () -> Unit = {}) {
    Surface(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        shape = BoardFlowShape.Card,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.xs, top = Spacing.md, bottom = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(player.displayName, size = 44.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        player.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        // Player names stay white everywhere; the chevron shows the row opens.
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (player.isHidden) {
                        Icon(
                            Icons.Default.VisibilityOff,
                            contentDescription = "Hidden from stats",
                            modifier = Modifier.size(Dimens.IconSmall),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    if (stats.totalPlays > 0) {
                        listOfNotNull(
                            if (stats.totalPlays == 1) "1 play" else "${stats.totalPlays} plays",
                            "${stats.winRate}% wins",
                            stats.lastPlayedDate?.let { "last $it" }
                        ).joinToString(" · ")
                    } else {
                        "No plays yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                stats.favoriteGame?.let { game ->
                    Text(
                        "Most played: $game",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(Modifier.size(width = 32.dp, height = Dimens.MinTouchTarget), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Avatar, name and a line of context: the header of every player dialog. */
@Composable
private fun PlayerDialogHeader(name: String, supporting: String, color: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerAvatar(name.ifBlank { "?" }, size = 56.dp, color = color)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            if (supporting.isNotBlank()) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EditPlayerDialog(
    player: Player,
    onDismiss: () -> Unit,
    onRenameDisplayName: (String) -> Unit,
    onUpdateBggUsername: (String) -> Unit,
    onAddAlias: (String) -> Unit,
    onRemoveAlias: (String) -> Unit,
    onToggleHidden: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onUpdateColor: (String) -> Unit = {},
    onSaved: () -> Unit = {}
) {
    var color        by remember { mutableStateOf(player.color) }
    var displayName  by remember { mutableStateOf(player.displayName) }
    var bggUsername  by remember { mutableStateOf(player.bggUsername) }
    var isHidden     by remember { mutableStateOf(player.isHidden) }
    var localAliases by remember { mutableStateOf(player.aliases) }
    var newAlias     by remember { mutableStateOf("") }

    LaunchedEffect(player.displayName) { displayName = player.displayName }
    LaunchedEffect(player.bggUsername) { bggUsername = player.bggUsername }
    LaunchedEffect(player.isHidden)    { isHidden    = player.isHidden }
    LaunchedEffect(player.color)       { color       = player.color }


    val identityChanged = (displayName.isNotBlank() && displayName != player.displayName)
            || bggUsername.trim() != player.bggUsername
            || isHidden != player.isHidden
            || color != player.color
            || localAliases != player.aliases
    val canAdd = newAlias.isNotBlank() && newAlias.trim() !in localAliases
    val doAdd = { if (canAdd) { localAliases = localAliases + newAlias.trim(); newAlias = "" } }

    AnimatedDialog(onDismissRequest = onDismiss) {
        Column {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                item {
                    // The header avatar previews the colour before it is saved.
                    PlayerDialogHeader(
                        player.displayName,
                        "Edit player",
                        color = parsePlayerColor(color) ?: playerInitialColor(player.displayName)
                    )
                }

                item {
                    BoardFlowFormGroup(raised = true) {
                        BoardFlowFormRow(label = "Name", icon = Icons.Default.Person) {
                            BoardFlowInlineField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                placeholder = "Player name",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        BoardFlowFormDivider()
                        BoardFlowFormRow(label = "BGG user", icon = BoardFlowIcons.OpenWeb) {
                            BoardFlowInlineField(
                                value = bggUsername,
                                onValueChange = { bggUsername = it },
                                placeholder = "Optional",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        BoardFlowFormDivider()
                        BoardFlowFormRow(label = "Hide from stats", icon = Icons.Default.VisibilityOff, labelWidth = null) {
                            Switch(checked = isHidden, onCheckedChange = { isHidden = it })
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        BoardFlowSectionTitle(
                            title = "Colour",
                            supporting = "Used for this player's circle across the app"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            ColorSwatch(
                                fill = playerInitialColor(player.displayName),
                                label = "Automatic",
                                selected = color.isBlank(),
                                automatic = true,
                                onClick = { color = "" }
                            )
                            PlayerColorChoices.forEach { (label, hex) ->
                                ColorSwatch(
                                    fill = parsePlayerColor(hex) ?: Color.Gray,
                                    label = label,
                                    selected = color.equals(hex, ignoreCase = true),
                                    onClick = { color = hex }
                                )
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        BoardFlowSectionTitle(
                            title = "Aliases",
                            supporting = "Other names this player goes by in your plays"
                        )
                        if (localAliases.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                localAliases.forEach { alias ->
                                    InputChip(
                                        selected = false,
                                        onClick = { localAliases = localAliases.filter { it != alias } },
                                        label = { Text(alias, style = MaterialTheme.typography.labelLarge) },
                                        shape = BoardFlowShape.Pill,
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove $alias",
                                                modifier = Modifier.size(Dimens.IconSmall),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        BoardFlowFormGroup(raised = true) {
                            BoardFlowFormRow(label = "Add alias", labelWidth = 96.dp) {
                                BoardFlowInlineField(
                                    value = newAlias,
                                    onValueChange = { newAlias = it },
                                    placeholder = "Another name",
                                    modifier = Modifier.weight(1f)
                                )
                                BoardFlowIconButton(onClick = doAdd, enabled = canAdd) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add alias",
                                        tint = if (canAdd) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(thickness = Dimens.Hairline, color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.sm, end = Spacing.lg, top = Spacing.md, bottom = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoardFlowIconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(BoardFlowIcons.Delete, contentDescription = "Delete player", modifier = Modifier.size(Dimens.Icon))
                }
                Spacer(Modifier.weight(1f))
                BoardFlowInlineAction(onClick = onDismiss, neutral = true, large = true) { Text("Cancel") }
                BoardFlowButton(
                    onClick = {
                        if (displayName.isNotBlank() && displayName != player.displayName) onRenameDisplayName(displayName)
                        if (bggUsername.trim() != player.bggUsername) onUpdateBggUsername(bggUsername)
                        if (isHidden != player.isHidden) onToggleHidden(isHidden)
                        if (color != player.color) onUpdateColor(color)
                        val toAdd = localAliases - player.aliases.toSet()
                        val toRemove = player.aliases - localAliases.toSet()
                        toAdd.forEach { onAddAlias(it) }
                        toRemove.forEach { onRemoveAlias(it) }
                        onSaved()
                        onDismiss()
                    },
                    enabled = identityChanged
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(Dimens.Icon))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
fun PlayersTabContent(
    players: List<Player>,
    sourcePlays: List<LoggedPlay>,
    onEditPlayer: (Player) -> Unit,
    currentPlayerName: String? = null,
    listState: LazyListState = rememberLazyListState(),
    openPlayerId: String? = null,
    onOpenPlayerConsumed: () -> Unit = {},
    onViewPlayerPlays: (playerName: String, sourcePlayerId: String) -> Unit = { _, _ -> },
    onViewPlayerGame: (gameId: Int, gameName: String, sourcePlayerId: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var viewingPlayer by remember { mutableStateOf<Player?>(null) }
    var viewingRival by remember { mutableStateOf<Player?>(null) }
    var showHiddenSection by remember { mutableStateOf(false) }
    val sortedAll = remember(players, sourcePlays) { players.sortedByRecentActivity(sourcePlays) }
    val sortedPlayers = remember(sortedAll) { sortedAll.filter { !it.isHidden } }
    val hiddenPlayers = remember(sortedAll) { sortedAll.filter { it.isHidden } }

    LaunchedEffect(openPlayerId, players) {
        if (openPlayerId != null && viewingPlayer == null && viewingRival == null) {
            viewingPlayer = players.find { it.id == openPlayerId }
            onOpenPlayerConsumed()
        }
    }

    viewingPlayer?.let { vp ->
        val livePlayer = players.find { it.id == vp.id }
        if (livePlayer != null) {
            val stats = remember(sourcePlays, livePlayer) { sourcePlays.statsForPlayer(livePlayer) }
            val rivalries = remember(sourcePlays, livePlayer) { sourcePlays.rivalriesForPlayer(livePlayer) }
            PlayerDetailDialog(
                player = livePlayer,
                stats = stats,
                rivalries = rivalries,
                sourcePlays = sourcePlays,
                allPlayers = players,
                currentPlayerName = currentPlayerName,
                onDismiss = { viewingPlayer = null },
                onEdit = { onEditPlayer(livePlayer); viewingPlayer = null },
                onViewPlays = { val id = vp.id; viewingPlayer = null; onViewPlayerPlays(livePlayer.displayName, id) },
                onViewGame = { gameId, gameName -> val id = vp.id; viewingPlayer = null; onViewPlayerGame(gameId, gameName, id) },
                onViewRival = { rival -> viewingRival = rival }
            )
        } else { viewingPlayer = null }
    }

    viewingRival?.let { rv ->
        val liveRival = players.find { it.id == rv.id }
        if (liveRival != null) {
            val stats = remember(sourcePlays, liveRival) { sourcePlays.statsForPlayer(liveRival) }
            val rivalries = remember(sourcePlays, liveRival) { sourcePlays.rivalriesForPlayer(liveRival) }
            PlayerDetailDialog(
                player = liveRival,
                stats = stats,
                rivalries = rivalries,
                sourcePlays = sourcePlays,
                allPlayers = players,
                currentPlayerName = currentPlayerName,
                onDismiss = { viewingRival = null },
                onEdit = { onEditPlayer(liveRival); viewingRival = null },
                onViewPlays = { val id = rv.id; viewingRival = null; onViewPlayerPlays(liveRival.displayName, id) },
                onViewGame = { gameId, gameName -> val id = rv.id; viewingRival = null; onViewPlayerGame(gameId, gameName, id) },
                onViewRival = { rival -> viewingRival = rival }
            )
        } else { viewingRival = null }
    }

    if (players.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Person, contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
                Text(
                    "No players yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Players are added automatically when you log plays.\nTap + to add your first player manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(sortedPlayers, key = { it.id }) { player ->
                val stats = remember(sourcePlays, player) { sourcePlays.statsForPlayer(player) }
                PlayerListItem(player = player, stats = stats,
                    onTap = { viewingPlayer = player })
            }

            if (hiddenPlayers.isNotEmpty()) {
                item(key = "hidden-section-header") {
                    HiddenPlayersSectionHeader(
                        count = hiddenPlayers.size,
                        expanded = showHiddenSection,
                        onToggle = { showHiddenSection = !showHiddenSection }
                    )
                }
                if (showHiddenSection) {
                    items(hiddenPlayers, key = { "h-${it.id}" }) { player ->
                        val stats = remember(sourcePlays, player) { sourcePlays.statsForPlayer(player) }
                        PlayerListItem(player = player, stats = stats,
                            onTap = { viewingPlayer = player })
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenPlayersSectionHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    BoardFlowSectionTitle(
        title = "Hidden players ($count)",
        modifier = Modifier.clip(BoardFlowShape.Control).clickable(onClick = onToggle)
    ) {
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse hidden" else "Expand hidden",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
internal fun PlayerDetailDialog(
    player: Player,
    stats: PlayerStats,
    rivalries: List<RivalryStat>,
    sourcePlays: List<LoggedPlay> = emptyList(),
    allPlayers: List<Player> = emptyList(),
    currentPlayerName: String? = null,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onViewPlays: (() -> Unit)? = null,
    onViewGame: ((gameId: Int, gameName: String) -> Unit)? = null,
    onViewRival: ((Player) -> Unit)? = null
) {
    // True when the dialog is showing the app user's own profile — enables "You" framing in rivalries
    val isCurrentPlayer = currentPlayerName != null &&
        (listOf(player.displayName) + player.aliases).any { it.equals(currentPlayerName, ignoreCase = true) }
    val recentPlays = remember(sourcePlays, player) { sourcePlays.recentPlaysForPlayer(player, 5) }
    val names = remember(player) { (listOf(player.displayName) + player.aliases).map { it.lowercase().trim() } }

    AnimatedDialog(onDismissRequest = onDismiss) {
        Column {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                item {
                    PlayerDialogHeader(
                        name = player.displayName,
                        supporting = listOfNotNull(
                            player.bggUsername.takeIf { it.isNotBlank() }?.let { "BGG: $it" },
                            player.aliases.takeIf { it.isNotEmpty() }?.let { "also ${it.joinToString(", ")}" }
                        ).joinToString(" · ")
                    )
                }

                if (stats.totalPlays > 0) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            PlayerStatCell("plays", "${stats.totalPlays}", Modifier.weight(1f), onClick = onViewPlays)
                            PlayerStatCell("wins", "${stats.wins}", Modifier.weight(1f))
                            PlayerStatCell("win rate", "${stats.winRate}%", Modifier.weight(1f))
                        }
                    }
                    item {
                        BoardFlowFormGroup(raised = true) {
                            var first = true
                            if (stats.currentWinStreak >= 2) {
                                DetailRow("Streak", "${stats.currentWinStreak} wins in a row")
                                first = false
                            }
                            stats.lastPlayedDate?.let {
                                if (!first) BoardFlowFormDivider()
                                DetailRow("Last played", it)
                                first = false
                            }
                            stats.favoriteGame?.let { gameName ->
                                if (!first) BoardFlowFormDivider()
                                val gameId = stats.favoriteGameId
                                DetailRow(
                                    "Most played", gameName,
                                    onClick = if (onViewGame != null && gameId != null) {
                                        { onViewGame(gameId, gameName) }
                                    } else null
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            "No plays recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (recentPlays.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            BoardFlowSectionTitle(title = "Recent plays")
                            BoardFlowFormGroup(raised = true) {
                                recentPlays.forEachIndexed { index, play ->
                                    if (index > 0) BoardFlowFormDivider()
                                    val won = play.players.any { it.name.lowercase().trim() in names && it.isWinner }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = Dimens.MinTouchTarget)
                                            .padding(horizontal = Spacing.lg),
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            play.gameName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            formatPlayDate(play.date),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (won) {
                                            Icon(
                                                Icons.Default.EmojiEvents,
                                                contentDescription = "Won",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(Dimens.Icon)
                                            )
                                        } else {
                                            Spacer(Modifier.size(Dimens.Icon))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (rivalries.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            BoardFlowSectionTitle(title = "Rivalries")
                            BoardFlowFormGroup(raised = true) {
                                rivalries.forEachIndexed { index, rivalry ->
                                    if (index > 0) BoardFlowFormDivider()
                                    val rivalPlayer = allPlayers.firstOrNull { p ->
                                        (listOf(p.displayName) + p.aliases).any {
                                            it.equals(rivalry.opponentName, ignoreCase = true)
                                        }
                                    }
                                    RivalryRow(
                                        rivalry = rivalry,
                                        isCurrentPlayer = isCurrentPlayer,
                                        onClick = if (onViewRival != null && rivalPlayer != null) {
                                            { onViewRival(rivalPlayer) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(thickness = Dimens.Hairline, color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoardFlowIconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit player",
                        modifier = Modifier.size(Dimens.Icon),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.weight(1f))
                if (onViewPlays != null) {
                    BoardFlowButton(onClick = onViewPlays) { Text("All plays") }
                }
            }
        }
    }
}

@Composable
private fun PlayerStatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.withTabularNumbers(),
                // Amber only on the cell that opens something.
                color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = BoardFlowShape.Card,
            color = MaterialTheme.colorScheme.surfaceContainerHigh, content = content)
    } else {
        Surface(modifier = modifier, shape = BoardFlowShape.Card,
            color = MaterialTheme.colorScheme.surfaceContainerHigh, content = content)
    }
}

@Composable
private fun DetailRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    BoardFlowFormRow(label = label, labelWidth = 104.dp, onClick = onClick) {
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RivalryRow(
    rivalry: RivalryStat,
    isCurrentPlayer: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val winFraction = if (rivalry.playsTogetherCount > 0)
        rivalry.myWins.toFloat() / rivalry.playsTogetherCount else 0f

    // Personalised narrative: only when viewing the app user's own rivalries
    val narrative: String? = if (isCurrentPlayer) when {
        rivalry.myWins > rivalry.theirWins ->
            "You lead ${rivalry.myWins}-${rivalry.theirWins}"
        rivalry.theirWins > rivalry.myWins ->
            "${rivalry.opponentName} leads ${rivalry.theirWins}-${rivalry.myWins}"
        rivalry.myWins > 0 ->
            "Level at ${rivalry.myWins} each"
        else -> null
    } else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = Spacing.lg, end = Spacing.md, top = Spacing.md, bottom = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        PlayerAvatar(rivalry.opponentName, size = 32.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    rivalry.opponentName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${rivalry.myWins}-${rivalry.theirWins}",
                    style = MaterialTheme.typography.titleSmall.withTabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            LinearProgressIndicator(
                progress = { winFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(BoardFlowShape.Pill),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Text(
                listOfNotNull(
                    narrative,
                    if (rivalry.playsTogetherCount == 1) "1 play together" else "${rivalry.playsTogetherCount} plays together"
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AnimatedDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("New player", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Players are also added automatically when you log a play with a new name.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BoardFlowFormGroup(raised = true) {
                BoardFlowFormRow(label = "Name", icon = Icons.Default.Person) {
                    BoardFlowInlineField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = "Player name",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End)
            ) {
                BoardFlowInlineAction(onClick = onDismiss, neutral = true, large = true) { Text("Cancel") }
                BoardFlowButton(onClick = { onAdd(newName) }, enabled = newName.isNotBlank()) { Text("Add player") }
            }
        }
    }
}

/** One choice in the colour picker: a 40dp circle, ringed and ticked when selected. */
@Composable
private fun ColorSwatch(
    fill: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    automatic: Boolean = false
) {
    val mark = if (fill.luminance() > 0.55f) PlayerColors.DarkInk else Color.White
    Box(
        modifier = Modifier
            .size(Dimens.MinTouchTarget)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.RadioButton
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = fill,
            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    selected -> Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = mark,
                        modifier = Modifier.size(Dimens.Icon)
                    )
                    automatic -> Text("A", color = mark, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
