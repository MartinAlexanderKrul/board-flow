@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package cz.nicolsburg.boardflow.ui.history

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.AppViewModel
import cz.nicolsburg.boardflow.ui.common.AnimatedDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationKind
import cz.nicolsburg.boardflow.ui.common.BoardFlowDestructiveButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowIconButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowIcons
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.model.PlayerResult
import cz.nicolsburg.boardflow.ui.common.withTabularNumbers
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.snapshotFlow
import cz.nicolsburg.boardflow.ui.common.BoardFlowFilterChip
import cz.nicolsburg.boardflow.ui.common.BoardFlowFilterSection
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowMotion
import cz.nicolsburg.boardflow.ui.common.BoardFlowPullRefreshContainer
import cz.nicolsburg.boardflow.ui.common.BoardFlowAnimatedVisibility
import cz.nicolsburg.boardflow.ui.common.BoardFlowModalBottomSheet
import cz.nicolsburg.boardflow.ui.common.BoardFlowSurfaceTokens
import cz.nicolsburg.boardflow.ui.common.GameSearchField
import cz.nicolsburg.boardflow.ui.common.SearchFieldActionButton
import cz.nicolsburg.boardflow.ui.common.boardFlowTween
import cz.nicolsburg.boardflow.ui.common.rememberBoardFlowPressScale
import cz.nicolsburg.boardflow.ui.common.rememberBoardFlowShimmerAlpha
import kotlinx.coroutines.flow.collect
import androidx.compose.material.icons.filled.Add
import cz.nicolsburg.boardflow.ui.common.ScreenTabRow
import cz.nicolsburg.boardflow.ui.common.swipeToNavigateTabs
import cz.nicolsburg.boardflow.ui.players.AddPlayerDialog
import cz.nicolsburg.boardflow.ui.players.EditPlayerDialog
import cz.nicolsburg.boardflow.ui.players.PlayersTabContent

private enum class HistorySortMode(val label: String) {
    DATE_DESC("Newest first"),
    DATE_ASC("Oldest first"),
    GAME_NAME("Game name"),
    DURATION("Duration")
}

private enum class HistoryDateRange(val label: String) {
    ALL("All time"),
    THIS_WEEK("This week"),
    THIS_MONTH("This month"),
    THIS_YEAR("This year")
}

private enum class HistoryTab(val label: String) {
    PLAYS("Plays"),
    STATS("Stats"),
    PLAYERS("Players")
}

@Composable
fun HistoryScreen(
    viewModel: AppViewModel,
    onActiveTabChange: (String?) -> Unit = {}
) {
    val historyPlays by viewModel.historyPlays.collectAsState()
    val bggLoading by viewModel.bggPlaysLoading.collectAsState()
    val bggError by viewModel.bggPlaysError.collectAsState()
    val players by viewModel.players.collectAsState()
    val deletingPlayId by viewModel.deletingBggPlayId.collectAsState()
    val editPlayLoading by viewModel.editPlayLoading.collectAsState()
    var playToDelete by remember { mutableStateOf<LoggedPlay?>(null) }
    var selectedPlay by remember { mutableStateOf<LoggedPlay?>(null) }
    var editingPlay by remember { mutableStateOf<LoggedPlay?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var editError by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(HistorySortMode.DATE_DESC) }
    var filterDateRange by remember { mutableStateOf(HistoryDateRange.ALL) }
    var filterPlayer by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    val hasActiveFilters = sortMode != HistorySortMode.DATE_DESC ||
        filterDateRange != HistoryDateRange.ALL ||
        filterPlayer != null

    val filteredPlays = remember(historyPlays, searchQuery, sortMode, filterDateRange, filterPlayer, players) {
        var result = historyPlays

        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            result = result.filter {
                it.gameName.lowercase().contains(query) ||
                    it.players.any { p -> p.name.lowercase().contains(query) }
            }
        }

        val today = LocalDate.now()
        result = when (filterDateRange) {
            HistoryDateRange.ALL -> result
            HistoryDateRange.THIS_WEEK -> {
                val cutoff = today.minusWeeks(1)
                result.filter { runCatching { !LocalDate.parse(it.date).isBefore(cutoff) }.getOrDefault(true) }
            }
            HistoryDateRange.THIS_MONTH -> result.filter {
                runCatching {
                    LocalDate.parse(it.date).let { d -> d.year == today.year && d.monthValue == today.monthValue }
                }.getOrDefault(true)
            }
            HistoryDateRange.THIS_YEAR -> result.filter {
                runCatching { LocalDate.parse(it.date).year == today.year }.getOrDefault(true)
            }
        }

        filterPlayer?.let { playerDisplayName ->
            val player = players.find { it.displayName == playerDisplayName }
            val names = if (player != null) {
                (listOf(player.displayName) + player.aliases).map { it.lowercase().trim() }
            } else {
                listOf(playerDisplayName.lowercase().trim())
            }
            result = result.filter { play -> play.players.any { it.name.lowercase().trim() in names } }
        }

        when (sortMode) {
            HistorySortMode.DATE_DESC -> result
            HistorySortMode.DATE_ASC -> result.sortedBy { it.date }
            HistorySortMode.GAME_NAME -> result.sortedBy { it.gameName.lowercase() }
            HistorySortMode.DURATION -> result.sortedByDescending { it.durationMinutes }
        }
    }

    var activeTab by remember { mutableStateOf(HistoryTab.PLAYS) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<cz.nicolsburg.boardflow.model.Player?>(null) }

    LaunchedEffect(activeTab) {
        controlsVisible = true
        if (activeTab == HistoryTab.PLAYS) listState.scrollToItem(0)
    }

    LaunchedEffect(listState) {
        var lastIndex = 0
        var lastOffset = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val scrollingDown = index > lastIndex || (index == lastIndex && offset > lastOffset)
                val atTop = index == 0 && offset < 8
                controlsVisible = atTop || !scrollingDown
                lastIndex = index
                lastOffset = offset
            }
    }

    LaunchedEffect(searchQuery, sortMode, filterDateRange, filterPlayer) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(Unit) {
        viewModel.loadPlayers()
        viewModel.loadPlayHistory()
        viewModel.loadCachedBggPlays()
    }

    LaunchedEffect(controlsVisible, activeTab) {
        onActiveTabChange(if (controlsVisible) null else activeTab.label)
    }

    DisposableEffect(Unit) {
        onDispose { onActiveTabChange(null) }
    }

    playToDelete?.let { play ->
        BoardFlowConfirmationDialog(
            title = "Delete play?",
            message = "Delete this play from BGG? This also removes it from the local cached history.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = {
                viewModel.deleteBggPlay(
                    playId = play.id,
                    onSuccess = {
                        playToDelete = null
                        deleteError = null
                    },
                    onError = { message ->
                        deleteError = message
                        playToDelete = null
                    }
                )
            },
            onDismiss = { playToDelete = null }
        )
    }

    selectedPlay?.let { play ->
        PlayDetailsDialog(
            play = play,
            players = players,
            isDeleting = deletingPlayId == play.id,
            onDismiss = { selectedPlay = null },
            onEdit = { editingPlay = play; selectedPlay = null },
            onDeletePlay = {
                selectedPlay = null
                playToDelete = play
            }
        )
    }

    editingPlay?.let { play ->
        EditPlayDialog(
            play = play,
            rosterPlayers = players,
            isLoading = editPlayLoading,
            onDismiss = { editingPlay = null; editError = null },
            onSave = { date, durationMinutes, location, comments, players ->
                editingPlay = null
                editError = null
                viewModel.editPlay(
                    play = play,
                    date = date,
                    durationMinutes = durationMinutes,
                    location = location,
                    comments = comments,
                    players = players,
                    onSuccess = { editError = null },
                    onError = { editError = it }
                )
            }
        )
    }

    if (showAddPlayerDialog) {
        AddPlayerDialog(
            onDismiss = { showAddPlayerDialog = false },
            onAdd = { name -> viewModel.addNewPlayer(name); showAddPlayerDialog = false }
        )
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
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            if (activeTab == HistoryTab.PLAYERS) {
                FloatingActionButton(onClick = { showAddPlayerDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add player")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .swipeToNavigateTabs(
                    tabCount = HistoryTab.entries.size,
                    selectedIndex = activeTab.ordinal,
                    onNavigate = { activeTab = HistoryTab.entries[it] }
                )
        ) {
            BoardFlowAnimatedVisibility(visible = controlsVisible) {
                ScreenTabRow(
                    tabs = HistoryTab.entries.map { it.label },
                    selectedIndex = activeTab.ordinal,
                    onTabSelected = { activeTab = HistoryTab.entries[it] }
                )
            }

            when (activeTab) {
                HistoryTab.PLAYS -> Column(modifier = Modifier.fillMaxSize()) {
                    deleteError?.let { message ->
                        Surface(color = MaterialTheme.colorScheme.errorContainer) {
                            Text(
                        message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            editError?.let { message ->
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            BoardFlowAnimatedVisibility(visible = controlsVisible) {
                GameSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    trailingAction = {
                        Box {
                            SearchFieldActionButton(onClick = { showFilters = true }) {
                                Icon(BoardFlowIcons.Filter, contentDescription = "Sort & filter")
                            }
                            if (hasActiveFilters) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(7.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (showFilters) {
                BoardFlowModalBottomSheet(
                    onDismissRequest = { showFilters = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    HistoryFilterSheetContent(
                        sortMode = sortMode,
                        onSortMode = { sortMode = it },
                        filterDateRange = filterDateRange,
                        onFilterDateRange = { filterDateRange = it },
                        filterPlayer = filterPlayer,
                        onFilterPlayer = { filterPlayer = it },
                        players = players,
                        hasActiveFilters = hasActiveFilters,
                        onReset = {
                            sortMode = HistorySortMode.DATE_DESC
                            filterDateRange = HistoryDateRange.ALL
                            filterPlayer = null
                        }
                    )
                }
            }

                    PlaysContent(
                        plays = filteredPlays,
                        players = players,
                        loading = bggLoading,
                        error = bggError,
                        hasBggUsername = viewModel.prefs.bggUsername.isNotBlank(),
                        onOpenPlay = { selectedPlay = it },
                        onRefresh = viewModel::fetchBggPlays,
                        listState = listState,
                        hasActiveFilters = hasActiveFilters,
                        onResetFilters = {
                            sortMode = HistorySortMode.DATE_DESC
                            filterDateRange = HistoryDateRange.ALL
                            filterPlayer = null
                            searchQuery = ""
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                HistoryTab.STATS -> StatsContent(
                    plays = historyPlays,
                    players = players,
                    modifier = Modifier.fillMaxSize()
                )
                HistoryTab.PLAYERS -> PlayersTabContent(
                    players = players,
                    sourcePlays = historyPlays,
                    onEditPlayer = { editingPlayer = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun PlaysContent(
    plays: List<LoggedPlay>,
    players: List<Player>,
    loading: Boolean,
    error: String?,
    hasBggUsername: Boolean,
    onOpenPlay: (LoggedPlay) -> Unit,
    onRefresh: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    hasActiveFilters: Boolean = false,
    onResetFilters: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val emptyState = rememberScrollState()
    val listAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    val isAtTop = if (plays.isEmpty() && !loading) emptyState.value == 0 else listAtTop

    BoardFlowPullRefreshContainer(
        isRefreshing = loading,
        isAtTop = isAtTop,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        when {
            loading && plays.isEmpty() -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(5) { ShimmerPlayCard() }
            }

            error != null && plays.isEmpty() -> Box(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(emptyState),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
                    )
                    Text(
                        "Couldn't load history",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            plays.isEmpty() && hasActiveFilters -> Box(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(emptyState),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        BoardFlowIcons.Filter,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                    Text(
                        "No plays match your filters",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BoardFlowSecondaryButton(onClick = onResetFilters) {
                        Text("Reset filters")
                    }
                }
            }

            plays.isEmpty() -> Box(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(emptyState),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                    Text(
                        "No play history",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (!hasBggUsername)
                            "Set your BGG username in Settings to start tracking your play history."
                        else
                            "Use Sync to refresh your play history from BGG.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(plays, key = { it.id }) { play ->
                    PlayHistoryCard(
                        play = play,
                        players = players,
                        onClick = { onOpenPlay(play) }
                    )
                }
            }
        }
    }
}


@Composable
private fun PlayHistoryCard(
    play: LoggedPlay,
    players: List<Player>,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = rememberBoardFlowPressScale(isPressed = isPressed, label = "cardScale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(BoardFlowSurfaceTokens.Shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        shape = BoardFlowSurfaceTokens.Shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    play.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (play.durationMinutes > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                "${play.durationMinutes} min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    if (play.location.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                play.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    play.gameName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (play.quantity > 1) {
                    PlayBadge("×${play.quantity}", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }
                if (play.incomplete) {
                    PlayBadge("partial", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                play.players.forEach { player ->
                    PlayerRow(player, resolveDisplayName(player.name, players))
                }
            }
        }
    }
}

@Composable
private fun ShimmerPlayCard() {
    val alpha = rememberBoardFlowShimmerAlpha(label = "historyShimmerAlpha")
    val shimmer = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = BoardFlowSurfaceTokens.Shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.width(72.dp).height(10.dp).background(shimmer, RoundedCornerShape(4.dp)))
            Box(Modifier.fillMaxWidth(0.55f).height(14.dp).background(shimmer, RoundedCornerShape(4.dp)))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(Modifier.fillMaxWidth(0.4f).height(10.dp).background(shimmer, RoundedCornerShape(4.dp)))
                        Box(Modifier.width(32.dp).height(10.dp).background(shimmer, RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayBadge(label: String, containerColor: Color, contentColor: Color) {
    Surface(color = containerColor, shape = RoundedCornerShape(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun PlayerColorDot(colorName: String) {
    val knownColors = mapOf(
        "red" to Color(0xFFE53935), "blue" to Color(0xFF1E88E5), "green" to Color(0xFF43A047),
        "yellow" to Color(0xFFFDD835), "orange" to Color(0xFFFB8C00), "purple" to Color(0xFF8E24AA),
        "white" to Color(0xFFF5F5F5), "black" to Color(0xFF212121), "pink" to Color(0xFFE91E63),
        "brown" to Color(0xFF6D4C41), "gray" to Color(0xFF757575), "grey" to Color(0xFF757575),
        "cyan" to Color(0xFF00ACC1), "teal" to Color(0xFF00897B), "lime" to Color(0xFF7CB342)
    )
    val parsed = knownColors[colorName.lowercase().trim()]
        ?: runCatching { Color(android.graphics.Color.parseColor(colorName)) }.getOrNull()
    if (parsed != null) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(parsed, CircleShape)
        )
    }
}

@Composable
private fun PlayerRow(player: PlayerResult, displayName: String) {
    val scoreText = player.score.takeUnless {
        val normalized = it.trim()
        normalized.isEmpty() || normalized == "0" || normalized == "0.0"
    } ?: "—"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (player.color.isNotBlank()) {
            PlayerColorDot(player.color)
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (player.isWinner) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (player.isNew) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "New player",
                    tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Row(
            modifier = Modifier.width(56.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                scoreText,
                style = MaterialTheme.typography.bodyMedium.withTabularNumbers(),
                color = if (player.isWinner) MaterialTheme.colorScheme.tertiary
                else if (scoreText == "—") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (player.isWinner) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "Winner",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Spacer(Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PlayDetailsDialog(
    play: LoggedPlay,
    players: List<Player>,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDeletePlay: () -> Unit
) {
    AnimatedDialog(onDismissRequest = onDismiss) {
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
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                play.gameName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                play.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SuggestionChip(onClick = {}, label = { Text("${play.players.size} players", style = MaterialTheme.typography.labelSmall) })
                                if (play.durationMinutes > 0) {
                                    SuggestionChip(onClick = {}, label = { Text("${play.durationMinutes} min", style = MaterialTheme.typography.labelSmall) })
                                }
                                if (play.quantity > 1) {
                                    SuggestionChip(onClick = {}, label = { Text("×${play.quantity}", style = MaterialTheme.typography.labelSmall) })
                                }
                                if (play.incomplete) {
                                    SuggestionChip(onClick = {}, label = { Text("partial", style = MaterialTheme.typography.labelSmall) })
                                }
                            }
                        }
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                }

                item {
                    DetailSection(
                        rows = buildList {
                            add("Date" to play.date)
                            if (play.durationMinutes > 0) add("Duration" to "${play.durationMinutes} min")
                            if (play.location.isNotBlank()) add("Location" to play.location)
                            if (play.quantity > 1) add("Quantity" to "×${play.quantity}")
                            if (play.incomplete) add("Status" to "Incomplete play")
                            if (!play.nowInStats) add("Stats" to "Excluded from stats")
                            if (play.comments.isNotBlank()) add("Comment" to play.comments)
                        }
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Players", style = MaterialTheme.typography.titleSmall)
                        play.players.forEach { player ->
                            Column {
                                PlayerRow(player, resolveDisplayName(player.name, players))
                                val meta = buildList {
                                    if (player.color.isNotBlank()) add(player.color)
                                    if (player.rating.isNotBlank() && player.rating != "N/A") add("rated ${player.rating}")
                                    if (player.isNew) add("first play")
                                }
                                if (meta.isNotEmpty()) {
                                    Text(
                                        meta.joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BoardFlowSecondaryButton(
                            onClick = onEdit,
                            enabled = !isDeleting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit")
                        }
                        BoardFlowDestructiveButton(
                            onClick = onDeletePlay,
                            enabled = !isDeleting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(BoardFlowIcons.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

@Composable
private fun EditPlayDialog(
    play: LoggedPlay,
    rosterPlayers: List<Player>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (date: String, durationMinutes: Int, location: String, comments: String, players: List<PlayerResult>) -> Unit
) {
    var date by remember(play.id) { mutableStateOf(play.date) }
    var duration by remember(play.id) { mutableStateOf(if (play.durationMinutes > 0) play.durationMinutes.toString() else "") }
    var location by remember(play.id) { mutableStateOf(play.location) }
    var comments by remember(play.id) { mutableStateOf(play.comments) }
    val editPlayers = remember(play.id, rosterPlayers) { play.players.collapsedByName(rosterPlayers).toMutableStateList() }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = runCatching {
            LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AnimatedDialog(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Edit Play", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(play.gameName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = {},
                            label = { Text("Date") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date", modifier = Modifier.size(18.dp))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it.filter { c -> c.isDigit() } },
                            label = { Text("Duration (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = comments,
                            onValueChange = { comments = it },
                            label = { Text("Comments") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Players", style = MaterialTheme.typography.titleSmall)
                        editPlayers.forEachIndexed { index, player ->
                            EditPlayerRow(
                                player = player,
                                onNameChange = { editPlayers[index] = player.copy(name = it) },
                                onScoreChange = { editPlayers[index] = player.copy(score = it) },
                                onColorChange = { editPlayers[index] = player.copy(color = it) },
                                onToggleWinner = { editPlayers[index] = player.copy(isWinner = !player.isWinner) },
                                onFirstPlayChange = { editPlayers[index] = player.copy(isNew = it) }
                            )
                        }
                    }
                }

                item {
                    BoardFlowButton(
                        onClick = {
                            onSave(
                                date,
                                duration.toIntOrNull() ?: 0,
                                location,
                                comments,
                                editPlayers.toList().collapsedByName(rosterPlayers)
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Save")
                        }
                    }
                }
        }
    }
}


private fun List<PlayerResult>.collapsedByName(rosterPlayers: List<Player>): List<PlayerResult> {
    val collapsed = linkedMapOf<String, PlayerResult>()
    val blankNamePlayers = mutableListOf<PlayerResult>()

    forEach { player ->
        val trimmedName = player.name.trim()
        if (trimmedName.isBlank()) {
            blankNamePlayers += player.copy(name = trimmedName)
            return@forEach
        }

        val displayName = resolveDisplayName(trimmedName, rosterPlayers).trim()
        val key = displayName.lowercase()
        val normalized = player.copy(name = displayName)
        collapsed[key] = collapsed[key]?.mergeSameNamePlayer(normalized) ?: normalized
    }

    return (collapsed.values + blankNamePlayers).sortedWith(
        compareBy<PlayerResult> { it.name.trim().lowercase() }
            .thenBy { it.color.trim().lowercase() }
            .thenBy { it.score.trim() }
    )
}

private fun PlayerResult.mergeSameNamePlayer(other: PlayerResult): PlayerResult {
    return copy(
        score = score.meaningfulPlayerValueOr(other.score, treatZeroAsEmpty = true),
        isWinner = isWinner || other.isWinner,
        color = color.meaningfulPlayerValueOr(other.color),
        rating = rating.meaningfulPlayerValueOr(other.rating),
        isNew = isNew || other.isNew
    )
}

private fun String.meaningfulPlayerValueOr(fallback: String, treatZeroAsEmpty: Boolean = false): String {
    val value = trim()
    val fallbackValue = fallback.trim()
    val emptyValue = value.isBlank() ||
        value.equals("N/A", ignoreCase = true) ||
        (treatZeroAsEmpty && (value == "0" || value == "0.0"))
    return if (emptyValue) fallbackValue else value
}

@Composable
private fun EditPlayerRow(
    player: PlayerResult,
    onNameChange: (String) -> Unit,
    onScoreChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onToggleWinner: () -> Unit,
    onFirstPlayChange: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = player.name.ifBlank { "Unnamed player" }
    val scoreText = player.score.takeUnless {
        val normalized = it.trim()
        normalized.isEmpty() || normalized == "0" || normalized == "0.0"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BoardFlowSurfaceTokens.Shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (player.color.isNotBlank()) {
                PlayerColorDot(player.color)
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (scoreText != null) {
                Text(
                    scoreText,
                    style = MaterialTheme.typography.bodySmall.withTabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (player.isWinner) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "Winner",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse player" else "Expand player",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (player.color.isNotBlank()) {
                    PlayerColorDot(player.color)
                }
                OutlinedTextField(
                    value = player.name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1.6f)
                )
                OutlinedTextField(
                    value = player.score,
                    onValueChange = onScoreChange,
                    label = { Text("Score") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(0.8f)
                )
                BoardFlowIconButton(onClick = onToggleWinner) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Toggle winner",
                        tint = if (player.isWinner) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = player.color,
                    onValueChange = onColorChange,
                    label = { Text("Team / color") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = player.isNew,
                        onCheckedChange = onFirstPlayChange
                    )
                    Text(
                        "First play",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
    }
}

}

@Composable
private fun DetailSection(rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(96.dp)
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium.withTabularNumbers(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun resolveDisplayName(name: String, players: List<Player>): String {
    if (name.isBlank()) return name
    val lower = name.lowercase().trim()
    return players.firstOrNull { player ->
        (listOf(player.displayName) + player.aliases).any { it.lowercase().trim() == lower }
    }?.displayName ?: name
}


@Composable
private fun HistoryFilterSheetContent(
    sortMode: HistorySortMode,
    onSortMode: (HistorySortMode) -> Unit,
    filterDateRange: HistoryDateRange,
    onFilterDateRange: (HistoryDateRange) -> Unit,
    filterPlayer: String?,
    onFilterPlayer: (String?) -> Unit,
    players: List<Player>,
    hasActiveFilters: Boolean,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FilterAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Sort & Filter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Play history",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (hasActiveFilters) {
                BoardFlowInlineAction(onClick = onReset) { Text("Reset") }
            }
        }

        BoardFlowFilterSection(
            label = "Sort by",
            detail = "Choose how the history list is ordered."
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistorySortMode.entries.forEach { mode ->
                    BoardFlowFilterChip(
                        selected = sortMode == mode,
                        onClick = { onSortMode(mode) },
                        leadingIcon = if (sortMode == mode) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(BoardFlowSurfaceTokens.FilterIconSize)
                                )
                            }
                        } else null,
                        label = { Text(mode.label) }
                    )
                }
            }
        }

        BoardFlowFilterSection(
            label = "Date range",
            detail = "Show plays from a specific time period."
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryDateRange.entries.forEach { range ->
                    BoardFlowFilterChip(
                        selected = filterDateRange == range,
                        onClick = { onFilterDateRange(range) },
                        leadingIcon = if (filterDateRange == range) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(BoardFlowSurfaceTokens.FilterIconSize)
                                )
                            }
                        } else null,
                        label = { Text(range.label) }
                    )
                }
            }
        }

        if (players.isNotEmpty()) {
            BoardFlowFilterSection(
                label = "Player",
                detail = "Show only plays with this player."
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoardFlowFilterChip(
                        selected = filterPlayer == null,
                        onClick = { onFilterPlayer(null) },
                        label = { Text("Anyone") }
                    )
                    players.forEach { player ->
                        val isSelected = filterPlayer == player.displayName
                        BoardFlowFilterChip(
                            selected = isSelected,
                            onClick = { onFilterPlayer(if (isSelected) null else player.displayName) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(BoardFlowSurfaceTokens.FilterIconSize)
                                    )
                                }
                            } else null,
                            label = { Text(player.displayName) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
