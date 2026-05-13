package cz.nicolsburg.boardflow.ui.players

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.nicolsburg.boardflow.AppViewModel
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.ui.common.AnimatedDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationKind
import cz.nicolsburg.boardflow.ui.common.BoardFlowCloseGlyph
import cz.nicolsburg.boardflow.ui.common.BoardFlowDestructiveButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowIconButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
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
    val currentWinStreak: Int = 0
)

internal val PlayerStats.winRate: Int
    get() = if (totalPlays > 0) wins * 100 / totalPlays else 0

internal fun List<LoggedPlay>.statsForPlayer(player: Player): PlayerStats {
    val names = (listOf(player.displayName) + player.aliases).map { it.lowercase().trim() }
    val myPlays = filter { play -> play.players.any { it.name.lowercase().trim() in names } }
    val wins = myPlays.count { play -> play.players.any { it.name.lowercase().trim() in names && it.isWinner } }
    val lastDate = myPlays.maxOfOrNull { it.date }?.let { formatPlayDate(it) }
    val favGame = myPlays.groupingBy { it.gameName }.eachCount().maxByOrNull { it.value }?.key
    val streak = playerCurrentWinStreak(names)
    return PlayerStats(totalPlays = myPlays.size, wins = wins, lastPlayedDate = lastDate,
        favoriteGame = favGame, currentWinStreak = streak)
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
fun PlayersScreen(viewModel: AppViewModel) {
    val players    by viewModel.players.collectAsState()
    val sourcePlays by viewModel.historyPlays.collectAsState()
    val sortedPlayers = remember(players, sourcePlays) { players.sortedByRecentActivity(sourcePlays) }

    var showAddDialog  by remember { mutableStateOf(false) }
    var editingPlayer  by remember { mutableStateOf<Player?>(null) }
    var viewingPlayer  by remember { mutableStateOf<Player?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadPlayers(); viewModel.loadPlayHistory(); viewModel.loadCachedBggPlays()
    }

    if (showAddDialog) {
        AddPlayerDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name -> viewModel.addNewPlayer(name); showAddDialog = false }
        )
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
                onDismiss = { viewingPlayer = null },
                onEdit = { editingPlayer = livePlayer; viewingPlayer = null }
            )
        } else { viewingPlayer = null }
    }

    editingPlayer?.let { ep ->
        val livePlayer = players.find { it.id == ep.id }
        if (livePlayer != null) {
            EditPlayerDialog(
                player = livePlayer,
                onDismiss = { editingPlayer = null },
                onRenameDisplayName = { viewModel.updatePlayerDisplayName(livePlayer.id, it) },
                onUpdateBggUsername = { viewModel.updatePlayerBggUsername(livePlayer.id, it) },
                onAddAlias = { viewModel.addPlayerAlias(livePlayer.id, it) },
                onRemoveAlias = { viewModel.removePlayerAlias(livePlayer.id, it) },
                onDelete = { viewModel.deletePlayer(livePlayer.id); editingPlayer = null }
            )
        } else {
            editingPlayer = null
        }
    }

    Scaffold(
        topBar = {},
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add player")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (players.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
                        Text("No players yet", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Players are added automatically when you log plays.\nTap + to add your first player manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(sortedPlayers, key = { it.id }) { player ->
                        val stats = remember(sourcePlays, player) { sourcePlays.statsForPlayer(player) }
                        PlayerListItem(player = player, stats = stats,
                            onTap = { viewingPlayer = player },
                            onEdit = { editingPlayer = player })
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlayerListItem(player: Player, stats: PlayerStats, onTap: () -> Unit = {}, onEdit: () -> Unit) {
    SectionCard(onClick = onTap) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    player.displayName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                val infoParts = buildList {
                    if (player.bggUsername.isNotBlank()) add("BGG: ${player.bggUsername}")
                    if (player.aliases.isNotEmpty()) add("Aliases: ${player.aliases.joinToString(", ")}")
                }
                if (infoParts.isNotEmpty()) {
                    Text(
                        infoParts.joinToString("  -  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val statLine = if (stats.totalPlays > 0) {
                    buildList {
                        add("${stats.totalPlays} plays")
                        add("${stats.wins} wins (${stats.winRate}%)")
                        stats.lastPlayedDate?.let { add("Last played $it") }
                    }.joinToString("  ·  ")
                } else {
                    "No plays yet"
                }
                Text(
                    statLine,
                    style = MaterialTheme.typography.bodySmall.withTabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                stats.favoriteGame?.let {
                    Text(
                        "Favorite: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )
                }
            }
            BoardFlowIconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit player")
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
    onDelete: () -> Unit
) {
    var displayName by remember { mutableStateOf(player.displayName) }
    var bggUsername by remember { mutableStateOf(player.bggUsername) }
    var newAlias    by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var aliasToRemove by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(player.displayName) { displayName = player.displayName }
    LaunchedEffect(player.bggUsername) { bggUsername = player.bggUsername }

    if (showDeleteConfirm) {
        BoardFlowConfirmationDialog(
            title = "Delete player?",
            message = "Delete \"${player.displayName}\" and all aliases? This cannot be undone.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = onDelete,
            onDismiss = { showDeleteConfirm = false }
        )
        return
    }

    aliasToRemove?.let { alias ->
        BoardFlowConfirmationDialog(
            title = "Remove alias?",
            message = "Remove \"$alias\" from ${player.displayName}?",
            confirmLabel = "Remove",
            dismissLabel = "Cancel",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = {
                onRemoveAlias(alias)
                aliasToRemove = null
            },
            onDismiss = { aliasToRemove = null }
        )
    }

    val identityChanged = (displayName.isNotBlank() && displayName != player.displayName)
            || bggUsername.trim() != player.bggUsername

    PlayerDialog(
        onDismissRequest = onDismiss,
        title = "Edit Player",
        actions = {
            BoardFlowButton(
                onClick = {
                    if (displayName.isNotBlank() && displayName != player.displayName) onRenameDisplayName(displayName)
                    if (bggUsername.trim() != player.bggUsername) onUpdateBggUsername(bggUsername)
                    onDismiss()
                },
                enabled = identityChanged,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Display Name", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = displayName, onValueChange = { displayName = it },
                            placeholder = { Text("e.g. Alice") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth())
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("BGG Username", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = bggUsername, onValueChange = { bggUsername = it },
                            placeholder = { Text("e.g. boardgamer42") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            HorizontalDivider()
            Text("Aliases", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (player.aliases.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    player.aliases.forEach { alias ->
                        InputChip(selected = false, onClick = {}, label = { Text(alias) },
                            trailingIcon = {
                                IconButton(onClick = { aliasToRemove = alias }, modifier = Modifier.size(18.dp)) {
                                    BoardFlowCloseGlyph(
                                        contentDescription = "Remove $alias",
                                        modifier = Modifier.size(14.dp),
                                        iconSize = 14.dp
                                    )
                                }
                            })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("New Alias", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = newAlias, onValueChange = { newAlias = it },
                        placeholder = { Text("e.g. Al") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                }
                BoardFlowIconButton(onClick = { onAddAlias(newAlias); newAlias = "" },
                    enabled = newAlias.isNotBlank()) {
                    Icon(Icons.Default.Add, contentDescription = "Add alias")
                }
            }
            HorizontalDivider()
            BoardFlowDestructiveButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete Player", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
internal fun PlayerDialog(
    onDismissRequest: () -> Unit,
    title: String,
    actions: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedDialog(onDismissRequest = onDismissRequest) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Column(content = content)
                }
                item {
                    Column(content = actions)
                }
        }
    }
}

@Composable
fun PlayersTabContent(
    players: List<Player>,
    sourcePlays: List<LoggedPlay>,
    onEditPlayer: (Player) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    var viewingPlayer by remember { mutableStateOf<Player?>(null) }
    val sortedPlayers = remember(players, sourcePlays) { players.sortedByRecentActivity(sourcePlays) }

    viewingPlayer?.let { vp ->
        val livePlayer = players.find { it.id == vp.id }
        if (livePlayer != null) {
            val stats = remember(sourcePlays, livePlayer) { sourcePlays.statsForPlayer(livePlayer) }
            val rivalries = remember(sourcePlays, livePlayer) { sourcePlays.rivalriesForPlayer(livePlayer) }
            PlayerDetailDialog(
                player = livePlayer,
                stats = stats,
                rivalries = rivalries,
                onDismiss = { viewingPlayer = null },
                onEdit = { onEditPlayer(livePlayer); viewingPlayer = null }
            )
        } else { viewingPlayer = null }
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
                    onTap = { viewingPlayer = player },
                    onEdit = { onEditPlayer(player) })
            }
        }
    }
}

@Composable
internal fun PlayerDetailDialog(
    player: Player,
    stats: PlayerStats,
    rivalries: List<RivalryStat>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    PlayerDialog(
        onDismissRequest = onDismiss,
        title = player.displayName,
        actions = {
            BoardFlowSecondaryButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit Player")
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (player.bggUsername.isNotBlank() || player.aliases.isNotEmpty()) {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (player.bggUsername.isNotBlank()) {
                            DetailRow("BGG username", player.bggUsername)
                        }
                        if (player.aliases.isNotEmpty()) {
                            DetailRow("Aliases", player.aliases.joinToString(", "))
                        }
                    }
                }
            }

            if (stats.totalPlays > 0) {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Play Stats", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlayerStatCell("Plays", "${stats.totalPlays}", Modifier.weight(1f))
                            PlayerStatCell("Wins", "${stats.wins}", Modifier.weight(1f))
                            PlayerStatCell("Win rate", "${stats.winRate}%", Modifier.weight(1f))
                        }
                        if (stats.currentWinStreak >= 2) {
                            DetailRow("Current streak", "${stats.currentWinStreak} in a row 🔥",
                                valueColor = MaterialTheme.colorScheme.primary)
                        }
                        stats.lastPlayedDate?.let { DetailRow("Last played", it) }
                        stats.favoriteGame?.let {
                            DetailRow("Most played", it,
                                valueColor = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                Text(
                    "No plays recorded yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (rivalries.isNotEmpty()) {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Rivalries", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        rivalries.forEach { rivalry ->
                            RivalryRow(rivalry = rivalry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerStatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp))
        Text(value, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor)
    }
}

@Composable
private fun RivalryRow(rivalry: RivalryStat) {
    val initial = rivalry.opponentName.take(1).uppercase()
    val winFraction = if (rivalry.playsTogetherCount > 0)
        rivalry.myWins.toFloat() / rivalry.playsTogetherCount else 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(initial, style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 11.sp)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(rivalry.opponentName, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium)
                Text("${rivalry.playsTogetherCount} plays together",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { winFraction },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                Text("${rivalry.myWins}–${rivalry.theirWins}",
                    style = MaterialTheme.typography.labelSmall.withTabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    PlayerDialog(
        onDismissRequest = onDismiss,
        title = "New Player",
        actions = {
            BoardFlowButton(
                onClick = { onAdd(newName) },
                enabled = newName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Player")
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Add a player manually. They will also continue to be created automatically from logged plays.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Display Name",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it },
                    placeholder = { Text("e.g. Alice") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
