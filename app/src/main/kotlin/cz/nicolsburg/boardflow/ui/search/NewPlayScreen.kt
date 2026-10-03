package cz.nicolsburg.boardflow.ui.search

import cz.nicolsburg.boardflow.ui.common.swipeToNavigateTabs
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.runtime.snapshotFlow
import cz.nicolsburg.boardflow.ui.common.BoardFlowAnimatedVisibility
import androidx.compose.runtime.LaunchedEffect
import cz.nicolsburg.boardflow.ui.common.withTabularNumbers
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowTextField
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import cz.nicolsburg.boardflow.ui.common.PlayerAvatar
import cz.nicolsburg.boardflow.ui.common.GameCover
import cz.nicolsburg.boardflow.ui.common.GameBackdrop
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import androidx.compose.ui.text.style.TextOverflow
import cz.nicolsburg.boardflow.ui.theme.Spacing
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import cz.nicolsburg.boardflow.ui.common.LocalBoardFlowMessenger
import cz.nicolsburg.boardflow.ui.common.GameListRow
import cz.nicolsburg.boardflow.ui.common.BoardFlowSectionTitle
import cz.nicolsburg.boardflow.ui.common.BoardFlowErrorBanner
import cz.nicolsburg.boardflow.ui.common.BoardFlowEmptyState
import cz.nicolsburg.boardflow.ui.common.BoardFlowCard
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import androidx.compose.material.icons.filled.Search
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.nicolsburg.boardflow.AppViewModel
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideAvailability
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideSummary
import cz.nicolsburg.boardflow.ui.common.BoardFlowFilterChip
import cz.nicolsburg.boardflow.ui.common.ScreenTabRow
import cz.nicolsburg.boardflow.model.BggGame
import cz.nicolsburg.boardflow.model.RecommendationLane
import cz.nicolsburg.boardflow.model.RecommendationPick
import cz.nicolsburg.boardflow.model.PlayTimer
import cz.nicolsburg.boardflow.model.SessionContext
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Icon
import androidx.compose.foundation.BorderStroke
import cz.nicolsburg.boardflow.ui.common.BoardFlowCloseGlyph
import cz.nicolsburg.boardflow.ui.common.BoardFlowIconButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowSurfaceTokens
import cz.nicolsburg.boardflow.ui.common.GameSearchField
import cz.nicolsburg.boardflow.ui.common.SearchFieldActionButton
import cz.nicolsburg.boardflow.ui.common.rememberBoardFlowShimmerAlpha
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ScrollDragState(val letter: Char, val dragFraction: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPlayScreen(
    viewModel: AppViewModel,
    onGameSelected: (BggGame) -> Unit,
    onPlayAgain: () -> Unit = {},
    onScanQuick: () -> Unit = {},
    setupGuideAvailability: Map<Int, SetupGuideAvailability> = emptyMap(),
    allSetupGuides: List<SetupGuideSummary> = emptyList(),
    onOpenQuickSetup: (gameId: Int) -> Unit = {},
    onActiveTabChange: (String?) -> Unit = {},
    onLogTimedPlay: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(NewPlayTab.LOG_PLAY) }
    // Quick Setup tab: every guide, or only the games on my shelf (with unavailable ones dimmed).
    // Quick Guides opens on the user's own games; All guides is one tap away.
    var showAllGuides by rememberSaveable { mutableStateOf(false) }
    val correctionMode by viewModel.quickScanCorrectionMode.collectAsState()
    // Correcting a scanned game is a Log Play flow; never show it on the Quick Setup tab.
    val setupTab = selectedTab == NewPlayTab.QUICK_SETUP && !correctionMode
    val results by viewModel.logPlaySearchResults.collectAsState()
    val loading by viewModel.searchLoading.collectAsState()
    val error   by viewModel.searchError.collectAsState()
    val collectionLoaded by viewModel.collectionLoaded.collectAsState()
    val sessionBannerVisible by viewModel.sessionBannerVisible.collectAsState()
    val sessionContext by viewModel.sessionContext.collectAsState()
    val changeGameActive by viewModel.changeGameSessionActive.collectAsState()
    val collectionItems by viewModel.collectionItems.collectAsState()
    val historyPlays by viewModel.historyPlays.collectAsState()
    val pendingPlayers by viewModel.pendingPlayers.collectAsState()
    val rosterPlayers by viewModel.players.collectAsState()
    val recommendationsEnabled by viewModel.recommendationsEnabled.collectAsState()
    val recommendationLanes = remember(query, sessionContext, collectionItems, historyPlays, pendingPlayers, recommendationsEnabled) {
        if (recommendationsEnabled && query.isBlank()) viewModel.getLogPlayRecommendations() else emptyList()
    }
    val activeTimer by viewModel.activeTimer.collectAsState()
    val messenger = LocalBoardFlowMessenger.current
    // Guides only carry a game id; borrow the cover from the collection when we have it.
    val thumbnailsById = remember(collectionItems) {
        collectionItems.mapNotNull { item -> item.objectId.toIntOrNull()?.let { it to item.thumbnailUrl } }.toMap()
    }

    LaunchedEffect(Unit) { viewModel.loadLogPlayGames() }

    // Tabs and search slide away while scrolling down and come back on the way up (as in Collection).
    val listState = rememberLazyListState()
    var controlsVisible by remember { mutableStateOf(true) }
    // Follows the finger, not the list position: hiding the controls makes the list taller, and a
    // position check would read that as scrolling back up and bring them straight back.
    val hideOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -4f) controlsVisible = false
                else if (available.y > 4f) controlsVisible = true
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(selectedTab, showAllGuides) {
        controlsVisible = true
        listState.scrollToItem(0)
    }

    LaunchedEffect(setupTab) { onActiveTabChange(if (setupTab) NewPlayTab.QUICK_SETUP.label else null) }

    LaunchedEffect(query) {
        delay(800)
        viewModel.filterLogPlayGames(query)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BoardFlowAnimatedVisibility(visible = controlsVisible && !correctionMode) {
            ScreenTabRow(
                tabs = NewPlayTab.entries.map { it.label },
                selectedIndex = selectedTab.ordinal,
                onTabSelected = {
                    selectedTab = NewPlayTab.entries[it]
                    // Each tab searches its own list; do not carry the text across.
                    query = ""
                }
            )
        }

        // A game is being played (timer running): the way back to logging it, above everything else.
        val playingTimer = activeTimer
        // On both tabs: Quick Setup's Start game lands on the guides tab.
        AnimatedVisibility(visible = playingTimer != null) {
            playingTimer?.let { timer ->
                PlayingNowBanner(
                    timer = timer,
                    thumbnailUrl = timer.gameId?.let { thumbnailsById[it] },
                    onLogResult = onLogTimedPlay
                )
            }
        }

        // Continue last session banner
        AnimatedVisibility(visible = sessionBannerVisible && !changeGameActive && !setupTab) {
            sessionContext?.let { ctx ->
                SessionContinueBanner(
                    context   = ctx,
                    thumbnailUrl = thumbnailsById[ctx.gameId],
                    onPlayAgain = {
                        viewModel.setupPlayAgain(ctx)
                        onPlayAgain()
                    },
                    onContinueWithAnotherGame = { viewModel.setupChangeGameSession(ctx) },
                    onStartNew = { viewModel.clearSession() },
                    onDismiss = { viewModel.dismissSessionBannerForSession() }
                )
            }
        }

        // Change game notice — same slot and size as the session banner
        AnimatedVisibility(visible = changeGameActive && !setupTab) {
            BoardFlowCard(
                emphasized = true,
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.Icon),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Pick the next game. Players from the last one are kept.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(hideOnScroll)
                // Swipe between Log Play and Quick Guides, like the other tabbed screens.
                .then(
                    if (correctionMode) Modifier
                    else Modifier.swipeToNavigateTabs(
                        tabCount = NewPlayTab.entries.size,
                        selectedIndex = selectedTab.ordinal,
                        onNavigate = {
                            selectedTab = NewPlayTab.entries[it]
                            query = ""
                        }
                    )
                )
                .padding(horizontal = 16.dp)
        ) {
            BoardFlowAnimatedVisibility(visible = controlsVisible) {
            Column {
            Spacer(Modifier.height(8.dp))

            GameSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search games...",
                modifier = Modifier.fillMaxWidth(),
                trailingAction = if (setupTab) null else {
                    {
                        SearchFieldActionButton(onClick = onScanQuick) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Scan score",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )

            if (setupTab) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BoardFlowFilterChip(
                        selected = !showAllGuides,
                        onClick = { showAllGuides = false },
                        label = { Text("My games") }
                    )
                    BoardFlowFilterChip(
                        selected = showAllGuides,
                        onClick = { showAllGuides = true },
                        label = { Text("All guides") }
                    )
                }
            }

            if (query.isBlank() && !changeGameActive && !setupTab) {
                Spacer(Modifier.height(4.dp))
                PlayingWithRow(
                    pendingPlayers = pendingPlayers,
                    rosterPlayers = rosterPlayers,
                    onAdd = viewModel::addPendingPlayer,
                    onRemove = viewModel::removePendingPlayer
                )
            }
            }
            }

            Spacer(Modifier.height(8.dp))

            when {
                setupTab && showAllGuides -> {
                    val matches = remember(allSetupGuides, query) {
                        allSetupGuides.filter { query.isBlank() || it.gameName.contains(query.trim(), ignoreCase = true) }
                    }
                    if (matches.isEmpty()) {
                        BoardFlowEmptyState(
                            icon = Icons.Default.Checklist,
                            title = if (query.isBlank()) "No setup guides yet" else "No guide for \"$query\"",
                            message = "Guides are added over time. Check the spelling or try another game."
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(matches, key = { it.gameId }) { guide ->
                                SetupGameRow(
                                    game = BggGame(
                                        id = guide.gameId,
                                        name = guide.gameName,
                                        yearPublished = null,
                                        thumbnailUrl = thumbnailsById[guide.gameId]
                                    ),
                                    available = true,
                                    onClick = { onOpenQuickSetup(guide.gameId) }
                                )
                            }
                        }
                    }
                }

                loading -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(10) { ShimmerGameRow() }
                }

                error != null -> Column(
                    modifier = Modifier.padding(vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    val message = error.orEmpty()
                    BoardFlowErrorBanner(
                        message = if (message.contains("private") || message.contains("401")) {
                            "$message\n\nMake your BGG profile public in its account settings, or search by name."
                        } else {
                            message
                        }
                    )
                    BoardFlowSecondaryButton(onClick = { viewModel.loadLogPlayGames() }) {
                        Text("Show recent games")
                    }
                }

                results.isEmpty() && query.isNotBlank() -> BoardFlowEmptyState(
                    icon = Icons.Default.Search,
                    title = "No games found for \"$query\"",
                    message = "Check the spelling, or try a shorter part of the name."
                )

                results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BoardFlowEmptyState(
                        icon = if (setupTab) Icons.Default.Checklist else Icons.AutoMirrored.Filled.NoteAdd,
                        title = if (setupTab) "No games to show" else "Log a play",
                        message = if (collectionLoaded) {
                            "Search for a game above."
                        } else {
                            "Search for a game above, or load your BGG collection in Settings, Sync."
                        }
                    )
                }

                else -> {
                    val scope = rememberCoroutineScope()
                    val showScrollBar = results.size > 20
                    var dragState by remember { mutableStateOf<ScrollDragState?>(null) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(
                                bottom = 8.dp,
                                end = if (showScrollBar) 20.dp else 0.dp
                            )
                        ) {
                            if (!setupTab && recommendationsEnabled && query.isBlank() && recommendationLanes.isNotEmpty()) {
                                item {
                                    RecommendationsSection(
                                        lanes = recommendationLanes,
                                        onSelect = { game ->
                                            viewModel.selectGame(game)
                                            onGameSelected(game)
                                        }
                                    )
                                }
                            }
                            if (setupTab) items(results) { game ->
                                SetupGameRow(
                                    game = game,
                                    available = game.id in setupGuideAvailability,
                                    onClick = { onOpenQuickSetup(game.id) }
                                )
                            } else items(results) { game ->
                                GameRow(
                                    game = game,
                                    timerActive = activeTimer?.gameId == game.id,
                                    onClick = {
                                        viewModel.selectGame(game)
                                        onGameSelected(game)
                                    },
                                    onTimerToggle = {
                                        if (activeTimer?.gameId == game.id) {
                                            viewModel.stopPlayTimer()
                                            messenger.show("Timer stopped")
                                        } else {
                                            viewModel.startPlayTimer(game.id, game.name)
                                            messenger.show("Timer started for ${game.name}")
                                        }
                                    },
                                )
                            }
                        }

                        if (showScrollBar) {
                            // Floating letter bubble — follows finger position instantly
                            dragState?.let { state ->
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val bubbleSize = 52.dp
                                    val inset = 8.dp
                                    val usable = maxHeight - inset * 2
                                    val yOffset = (inset + usable * state.dragFraction - bubbleSize / 2)
                                        .coerceIn(inset, maxHeight - inset - bubbleSize)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-24).dp, y = yOffset)
                                            .shadow(elevation = 12.dp, shape = CircleShape)
                                            .size(bubbleSize)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = state.letter.toString(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }

                            FastScrollBar(
                                listState = listState,
                                results = results,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp)
                                    .width(20.dp),
                                onScrollRequested = { idx -> scope.launch { listState.scrollToItem(idx) } },
                                onDragStateChange = { dragState = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationsSection(
    lanes: List<RecommendationLane>,
    onSelect: (BggGame) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Column(modifier = Modifier.padding(bottom = Spacing.sm)) {
        BoardFlowSectionTitle(
            title = "Good picks right now",
            modifier = Modifier
                .clip(BoardFlowShape.Control)
                .clickable { expanded = !expanded }
        ) {
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                lanes.forEach { lane ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Column {
                            Text(
                                lane.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                lane.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        lane.picks.forEach { pick ->
                            GameListRow(
                                name = pick.game.name,
                                thumbnailUrl = pick.game.thumbnailUrl,
                                supporting = pick.reason,
                                onClick = { onSelect(pick.game) }
                            ) { RowChevron() }
                        }
                    }
                }
                BoardFlowSectionTitle(title = "All games")
            }
        }
    }
}

/** Marks a row that opens something. Decorative: the whole row is the tap target. */
@Composable
private fun RowChevron() {
    Box(Modifier.size(width = 32.dp, height = Dimens.MinTouchTarget), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlayingWithRow(
    pendingPlayers: List<String>,
    rosterPlayers: List<cz.nicolsburg.boardflow.model.Player>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var pickerExpanded by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Playing:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp)
            )
            pendingPlayers.forEach { name ->
                InputChip(
                    selected = false,
                    onClick = { onRemove(name) },
                    label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove $name",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
            SuggestionChip(
                onClick = { pickerExpanded = !pickerExpanded },
                label = {
                    Text(
                        if (pendingPlayers.isEmpty()) "Who's playing?" else "+ Add",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                icon = {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        AnimatedVisibility(visible = pickerExpanded) {
            Surface(
                shape = BoardFlowShape.Control,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val available = remember(rosterPlayers, pendingPlayers) {
                        rosterPlayers
                            .filter { p -> pendingPlayers.none { it.equals(p.displayName, ignoreCase = true) } }
                            .sortedByDescending { it.lastPlayedAt ?: 0L }
                    }
                    if (available.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            available.forEach { player ->
                                SuggestionChip(
                                    onClick = { onAdd(player.displayName) },
                                    label = {
                                        Text(
                                            player.displayName,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            placeholder = {
                                Text("Type a name…", style = MaterialTheme.typography.bodySmall)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Done
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = {
                                    if (customName.isNotBlank()) {
                                        onAdd(customName.trim())
                                        customName = ""
                                    }
                                }
                            )
                        )
                        IconButton(
                            onClick = {
                                if (customName.isNotBlank()) {
                                    onAdd(customName.trim())
                                    customName = ""
                                }
                            },
                            enabled = customName.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add player",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FastScrollBar(
    listState: LazyListState,
    results: List<BggGame>,
    modifier: Modifier = Modifier,
    onScrollRequested: (Int) -> Unit,
    onDragStateChange: (ScrollDragState?) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val isScrollInProgress = listState.isScrollInProgress
    var isDragging by remember { mutableStateOf(false) }
    var lastLetter by remember { mutableStateOf<Char?>(null) }

    fun letterAt(idx: Int): Char? {
        val first = results.getOrNull(idx)?.name?.trimStart()?.firstOrNull()?.uppercaseChar()
        return if (first?.isLetter() == true) first else null
    }

    // Thumb position derived from list scroll state — avoids unnecessary recompositions
    val thumbFraction by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val total = layout.totalItemsCount.takeIf { it > 0 } ?: return@derivedStateOf 0f
            val visibleCount = layout.visibleItemsInfo.size
            val scrollable = (total - visibleCount).takeIf { it > 0 } ?: return@derivedStateOf 0f
            val avgH = layout.visibleItemsInfo
                .takeIf { it.isNotEmpty() }
                ?.let { it.sumOf { item -> item.size }.toFloat() / it.size }
                ?: 60f
            ((listState.firstVisibleItemIndex + listState.firstVisibleItemScrollOffset / avgH) / scrollable)
                .coerceIn(0f, 1f)
        }
    }

    val thumbSizeFraction by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val total = layout.totalItemsCount.takeIf { it > 0 } ?: return@derivedStateOf 1f
            (layout.visibleItemsInfo.size.toFloat() / total).coerceIn(0.04f, 1f)
        }
    }

    // Near-invisible at rest, brightens while scrolling, semi-transparent while dragging
    val thumbAlpha by animateFloatAsState(
        targetValue = when {
            isDragging         -> 0.80f
            isScrollInProgress -> 0.65f
            else               -> 0.20f
        },
        animationSpec = tween(durationMillis = if (isDragging || isScrollInProgress) 80 else 600),
        label = "thumbAlpha"
    )

    // Thumb fattens slightly when active
    val thumbWidthDp by animateDpAsState(
        targetValue = when {
            isDragging         -> 5.dp
            isScrollInProgress -> 4.dp
            else               -> 3.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "thumbWidth"
    )

    val primary   = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    var trackHeightPx by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .onSizeChanged { trackHeightPx = it.height }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val total = listState.layoutInfo.totalItemsCount
                        if (total == 0) return@detectVerticalDragGestures
                        val fraction = (offset.y / trackHeightPx).coerceIn(0f, 1f)
                        val targetIdx = (fraction * total).toInt().coerceIn(0, total - 1)
                        val letter = letterAt(targetIdx)
                        lastLetter = letter
                        if (letter != null) onDragStateChange(ScrollDragState(letter, fraction))
                        onScrollRequested(targetIdx)
                    },
                    onDragEnd = {
                        isDragging = false
                        lastLetter = null
                        onDragStateChange(null)
                    },
                    onDragCancel = {
                        isDragging = false
                        lastLetter = null
                        onDragStateChange(null)
                    },
                    onVerticalDrag = { change, _ ->
                        val total = listState.layoutInfo.totalItemsCount
                        if (total == 0) return@detectVerticalDragGestures
                        val fraction = (change.position.y / trackHeightPx).coerceIn(0f, 1f)
                        val targetIdx = (fraction * total).toInt().coerceIn(0, total - 1)
                        val letter = letterAt(targetIdx) ?: lastLetter
                        if (letter != null && letter != lastLetter) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        lastLetter = letter
                        if (letter != null) onDragStateChange(ScrollDragState(letter, fraction))
                        onScrollRequested(targetIdx)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tw = thumbWidthDp.toPx()
            val trackX = size.width - tw - 1.dp.toPx()
            val minThumbPx = 28.dp.toPx()
            val thumbH = (size.height * thumbSizeFraction).coerceAtLeast(minThumbPx)
            val thumbTop = (thumbFraction * (size.height - thumbH)).coerceIn(0f, size.height - thumbH)

            // Subtle track groove
            drawRoundRect(
                color = onSurface.copy(alpha = (thumbAlpha * 0.25f).coerceAtMost(0.10f)),
                topLeft = Offset(trackX, 0f),
                size = Size(tw, size.height),
                cornerRadius = CornerRadius(tw / 2)
            )
            // Amber thumb pill
            drawRoundRect(
                color = primary.copy(alpha = thumbAlpha),
                topLeft = Offset(trackX, thumbTop),
                size = Size(tw, thumbH),
                cornerRadius = CornerRadius(tw / 2)
            )
        }
    }
}

/** Shown while a play timer runs: the game, the running time, and a way to log the result. */
@Composable
private fun PlayingNowBanner(timer: PlayTimer, thumbnailUrl: String?, onLogResult: () -> Unit) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer.startedAt) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000)
        }
    }
    val elapsed = ((now - timer.startedAt) / 1000).coerceAtLeast(0)
    val clock = if (elapsed >= 3600) "%d:%02d:%02d".format(elapsed / 3600, elapsed % 3600 / 60, elapsed % 60)
                else "%d:%02d".format(elapsed / 60, elapsed % 60)
    Surface(
        onClick = onLogResult,
        shape = BoardFlowShape.Card,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            GameCover(name = timer.gameName, thumbnailUrl = thumbnailUrl, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Playing now · $clock",
                    style = MaterialTheme.typography.bodySmall.withTabularNumbers(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    timer.gameName.ifBlank { "Your game" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BoardFlowButton(onClick = onLogResult) { Text("Log result") }
        }
    }
}

@Composable
private fun SessionContinueBanner(
    context: SessionContext,
    thumbnailUrl: String? = null,
    onPlayAgain: () -> Unit,
    onContinueWithAnotherGame: () -> Unit,
    onStartNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val elapsedMs = System.currentTimeMillis() - context.lastPlayTimestamp
    val elapsedLabel = when {
        elapsedMs < 60_000L    -> "just now"
        elapsedMs < 3_600_000L -> "${elapsedMs / 60_000} min ago"
        else                   -> "${elapsedMs / 3_600_000} h ago"
    }
    val names = context.players.map { it.name.trim().substringBefore(' ') }.filter { it.isNotBlank() }
    val who = names.take(3).joinToString(", ") + if (names.size > 3) " +${names.size - 3}" else ""

    // A quiet shortcut, not the main thing on the screen: one line of context, small actions.
    Surface(
        shape = BoardFlowShape.Control,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    ) {
        Column(modifier = Modifier.padding(start = Spacing.md, bottom = Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOf("Session", context.gameName, who, elapsedLabel).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                BoardFlowIconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(Dimens.IconSmall))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SessionAction("Play again", onPlayAgain)
                SessionAction("Another game", onContinueWithAnotherGame)
                SessionAction("End session", onStartNew)
            }
        }
    }
}

@Composable
private fun SessionAction(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = BoardFlowShape.Pill, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
        )
    }
}

@Composable
private fun ShimmerGameRow() {
    val alpha = rememberBoardFlowShimmerAlpha(label = "searchShimmerAlpha")
    val shimmer = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.weight(1f).height(14.dp).background(shimmer, BoardFlowShape.Small))
        Box(Modifier.size(16.dp).background(shimmer, BoardFlowShape.Pill))
    }
}

@Composable
private fun GameRow(
    game: BggGame,
    timerActive: Boolean = false,
    onClick: () -> Unit,
    onTimerToggle: () -> Unit = {},
) {
    GameListRow(name = game.name, thumbnailUrl = game.thumbnailUrl, onClick = onClick) {
        BoardFlowIconButton(onClick = onTimerToggle) {
            Icon(
                if (timerActive) Icons.Default.TimerOff else Icons.Default.Timer,
                contentDescription = if (timerActive) "Stop timer" else "Start timer",
                tint = if (timerActive) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.Icon),
            )
        }
        RowChevron()
    }
}

private enum class NewPlayTab(val label: String) {
    LOG_PLAY("Log Play"),
    QUICK_SETUP("Quick Guides")
}

@Composable
private fun SetupGameRow(
    game: BggGame,
    available: Boolean,
    onClick: () -> Unit
) {
    // Games without a guide stay listed so search finds them, but cannot be opened.
    GameListRow(
        name = game.name,
        thumbnailUrl = game.thumbnailUrl,
        supporting = if (available) null else "No setup guide yet",
        enabled = available,
        onClick = onClick
    ) {
        if (available) RowChevron()
    }
}
