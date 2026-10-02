package cz.nicolsburg.boardflow.ui.review

import cz.nicolsburg.boardflow.ui.common.formatDisplayDate
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineField
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormRow
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormGroup
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormDivider
import cz.nicolsburg.boardflow.ui.common.BoardFlowFilterChip
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Place
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Shadow
import cz.nicolsburg.boardflow.ui.common.GameBackdrop
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.Check
import cz.nicolsburg.boardflow.ui.theme.Spacing
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import cz.nicolsburg.boardflow.ui.common.GameCover
import cz.nicolsburg.boardflow.ui.common.BoardFlowSectionTitle
import cz.nicolsburg.boardflow.ui.common.BoardFlowErrorBanner
import cz.nicolsburg.boardflow.ui.common.BoardFlowCard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CameraAlt
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import cz.nicolsburg.boardflow.model.Challenge
import cz.nicolsburg.boardflow.model.ChallengeProgress
import cz.nicolsburg.boardflow.model.PlayerResult
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.AppViewModel
import cz.nicolsburg.boardflow.model.BggGame
import cz.nicolsburg.boardflow.model.GameCandidate
import cz.nicolsburg.boardflow.model.GameRelations
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.RecordMoment
import cz.nicolsburg.boardflow.model.RecommendationPick
import cz.nicolsburg.boardflow.model.ScanRecognitionResult
import cz.nicolsburg.boardflow.model.SessionContext
import cz.nicolsburg.boardflow.model.deriveSessionHub
import cz.nicolsburg.boardflow.ui.history.SessionHubDialog
import cz.nicolsburg.boardflow.model.Player as BggPlayer
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowCloseGlyph
import cz.nicolsburg.boardflow.ui.common.BoardFlowIconButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowIcons
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowSurfaceTokens
import cz.nicolsburg.boardflow.ui.common.PlayerAvatar
import cz.nicolsburg.boardflow.ui.common.PlayerResultEditorCard
import cz.nicolsburg.boardflow.util.toFlexibleLocalDateOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private data class ChallengeAdvance(
    val challenge: Challenge,
    val from: Int,
    val to: Int,
    val goal: Int
) {
    val isNewlyComplete: Boolean get() = to >= goal && from < goal
}

private data class PostSaveInfo(
    val sessionContext: SessionContext,
    val record: RecordMoment?,
    val anchorPlay: LoggedPlay,
    val challengeAdvances: List<ChallengeAdvance> = emptyList(),
    val stillActive: List<ChallengeProgress> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LogPlayScreen(
    viewModel: AppViewModel,
    onPosted: () -> Unit,
    onChangeGame: () -> Unit,
    onNavigateBack: () -> Unit,
    onDiscard: () -> Unit = onNavigateBack,
    onChooseGame: () -> Unit = {},
    onScan: () -> Unit = {},
    onPickRecommendation: (BggGame) -> Unit = {},
    onEditPlay: (LoggedPlay) -> Unit = {}
) {
    val players         by viewModel.editablePlayers.collectAsState()
    val posting         by viewModel.postLoading.collectAsState()
    val extractedPlay   by viewModel.extractedPlay.collectAsState()
    val gameRelations   by viewModel.gameRelations.collectAsState()
    val additionalGames by viewModel.additionalGames.collectAsState()
    val rosterPlayers   by viewModel.players.collectAsState()
    val historyPlays    by viewModel.historyPlays.collectAsState()
    val recommendationsEnabled by viewModel.recommendationsEnabled.collectAsState()
    val gameCandidates        by viewModel.gameCandidates.collectAsState()
    val scanRecognitionResult by viewModel.scanRecognitionResult.collectAsState()
    val scanStartedWithGame   by viewModel.scanStartedWithGame.collectAsState()
    val scanRetryResult       by viewModel.scanRetryResult.collectAsState()

    // Read prefill once on first composition (consumed from ViewModel).
    val prefill = remember { viewModel.takePrefill() }

    var date           by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var duration       by rememberSaveable { mutableStateOf(prefill?.durationSuggestion ?: "") }
    var location       by rememberSaveable { mutableStateOf(prefill?.location ?: "") }
    var comments       by rememberSaveable { mutableStateOf("") }
    var quantity       by rememberSaveable { mutableStateOf(1) }
    var incomplete     by rememberSaveable { mutableStateOf(false) }
    var nowInStats     by rememberSaveable { mutableStateOf(true) }
    var showAdvanced   by rememberSaveable { mutableStateOf(false) }
    var errorMsg          by remember { mutableStateOf<String?>(null) }
    var showAiOutput      by rememberSaveable { mutableStateOf(false) }
    var showDatePicker    by rememberSaveable { mutableStateOf(false) }
    var focusFirstScore   by remember { mutableStateOf(false) }
    var collapsedPlayers by rememberSaveable { mutableStateOf<List<Boolean>>(emptyList()) }
    var playerRowKeys by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    // Post-save card state. Non-null while post-save card is visible.
    var postSaveInfo     by remember { mutableStateOf<PostSaveInfo?>(null) }
    // Snapshot kept alive so the exit fade animation has content to render.
    var lastPostSaveInfo by remember { mutableStateOf<PostSaveInfo?>(null) }
    if (postSaveInfo != null) lastPostSaveInfo = postSaveInfo
    var sessionHubInfo by remember { mutableStateOf<PostSaveInfo?>(null) }
    var sessionHubTitle by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionHubInfo?.anchorPlay?.sessionId) {
        sessionHubTitle = viewModel.getSessionTitle(sessionHubInfo?.anchorPlay?.sessionId)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                date.toFlexibleLocalDateOrNull()?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            }.getOrDefault(System.currentTimeMillis())
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Quick scan + "Enter manually" reaches this screen without a game.
    val hasGame = (viewModel.selectedGame?.id ?: 0) != 0
    val gameName = viewModel.selectedGame?.name ?: "No game selected"
    val headerGameName = if (additionalGames.isEmpty()) gameName
        else (listOf(gameName) + additionalGames.map { it.name }).joinToString(" + ")

    LaunchedEffect(extractedPlay?.date) {
        extractedPlay?.date?.takeIf { it.isNotBlank() }?.let { date = it }
    }

    val initialDate = extractedPlay?.date?.takeIf { it.isNotBlank() } ?: LocalDate.now().toString()
    val hasUnsavedChanges by remember(
        date,
        duration,
        location,
        comments,
        quantity,
        incomplete,
        nowInStats,
        players,
        additionalGames,
        extractedPlay
    ) {
        derivedStateOf {
            date != initialDate ||
                duration.isNotBlank() ||
                location.isNotBlank() ||
                comments.isNotBlank() ||
                quantity != 1 ||
                incomplete ||
                !nowInStats ||
                players.isNotEmpty() ||
                additionalGames.isNotEmpty() ||
                extractedPlay != null
        }
    }

    LaunchedEffect(hasUnsavedChanges) {
        viewModel.setLogPlayHasUnsavedChanges(hasUnsavedChanges)
    }

    LaunchedEffect(players.size) {
        if (collapsedPlayers.size != players.size) {
            collapsedPlayers = List(players.size) { index -> collapsedPlayers.getOrElse(index) { false } }
        }
        if (playerRowKeys.size != players.size) {
            playerRowKeys = List(players.size) { index -> playerRowKeys.getOrElse(index) { java.util.UUID.randomUUID().toString() } }
        }
    }

    LaunchedEffect(extractedPlay) {
        if (extractedPlay != null && players.isNotEmpty()) {
            collapsedPlayers = players.map { it.isReadyToCollapse() }
        }
    }

    LaunchedEffect(postSaveInfo) { viewModel.setLogPlayPostSaveShowing(postSaveInfo != null) }

    BackHandler {
        if (postSaveInfo != null) {
            postSaveInfo = null
            onPosted()
        } else {
            onDiscard()
        }
    }

    val online = viewModel.isOnline()
    val totalGames = 1 + additionalGames.size
    var nameFieldFocusIndex by remember { mutableStateOf(-1) }

    // Collapse every card whose player data is complete. Called when the user moves away
    // from the current player by adding another one.
    val collapseCompletePlayers: () -> Unit = {
        collapsedPlayers = players.mapIndexed { i, p ->
            collapsedPlayers.getOrElse(i) { false } || p.name.isNotBlank()
        }
    }

    val addEditablePlayer: () -> Unit = {
        val nextIndex = players.size
        collapseCompletePlayers()
        collapsedPlayers = collapsedPlayers + false
        playerRowKeys = playerRowKeys + java.util.UUID.randomUUID().toString()
        viewModel.addPlayer()
        nameFieldFocusIndex = nextIndex
    }

    // Compute frequent players in composable scope so remember is valid.
    val gameId = viewModel.selectedGame?.id ?: 0
    val excludedNames = remember(players) { players.map { it.name.trim() }.toSet() }
    val frequentPlayers = remember(gameId, excludedNames) {
        viewModel.getFrequentPlayers(gameId, excludedNames)
    }
    val recentPlayers = remember(excludedNames) {
        viewModel.getRecentPlayers(excludedNames)
    }
    val fabLabel = when {
        posting               -> "Saving..."
        !online && totalGames > 1 -> "Save $totalGames plays locally"
        !online               -> "Save locally"
        totalGames > 1        -> "Log $totalGames plays"
        else                  -> "Log play"
    }

    fun submitPlay() {
        if (!posting) {
            errorMsg = null
            val parsedDate = date.toFlexibleLocalDateOrNull() ?: LocalDate.now()
            val durationMin = duration.toIntOrNull() ?: 0
            val progressBefore = viewModel.getChallengeProgressList()
            viewModel.captureHistorySnapshot()
            viewModel.postPlay(
                date = parsedDate,
                durationMinutes = durationMin,
                location = location,
                comments = comments,
                quantity = quantity,
                incomplete = incomplete,
                nowInStats = nowInStats,
                onSuccess = { savedPlay, progressAfter ->
                    val game = viewModel.selectedGame
                    if (game != null) {
                        val ctx = SessionContext(
                            sessionId = savedPlay.sessionId ?: java.util.UUID.randomUUID().toString(),
                            gameId = game.id,
                            gameName = game.name,
                            players = players,
                            location = location,
                            startedAt = savedPlay.playedAt ?: System.currentTimeMillis(),
                            lastPlayTimestamp = savedPlay.playedAt ?: System.currentTimeMillis()
                        )
                        val record = viewModel.detectRecord(game.id, game.name, players)
                        val advances = progressAfter.mapNotNull { after ->
                            val before = progressBefore.firstOrNull { it.challenge.id == after.challenge.id }
                                ?: return@mapNotNull null
                            if (after.currentCount > before.currentCount)
                                ChallengeAdvance(after.challenge, before.currentCount, after.currentCount, after.goalCount)
                            else null
                        }
                        val advancedIds = advances.map { it.challenge.id }.toSet()
                        val stillActive = progressAfter.filter { it.isActive && it.challenge.id !in advancedIds }
                        postSaveInfo = PostSaveInfo(ctx, record, savedPlay, advances, stillActive)
                    } else {
                        onPosted()
                    }
                },
                onError = { errorMsg = it }
            )
        }
    }

    val contentBlur by animateDpAsState(
        targetValue    = if (postSaveInfo != null) 18.dp else 0.dp,
        animationSpec  = tween(200),
        label          = "contentBlur"
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).blur(contentBlur)) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column {
                        HorizontalDivider(thickness = Dimens.Hairline, color = MaterialTheme.colorScheme.outlineVariant)
                        errorMsg?.let {
                            BoardFlowErrorBanner(
                                message = it,
                                modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    when (players.size) {
                                        0 -> "No players yet"
                                        1 -> "1 player"
                                        else -> "${players.size} players"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    listOfNotNull(
                                        formatDisplayDate(date),
                                        location.trim().takeIf { it.isNotBlank() },
                                        "saved on this device".takeIf { !online }
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BoardFlowButton(onClick = ::submitPlay, enabled = !posting && hasGame) {
                                if (posting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(Dimens.Icon),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimens.Icon)
                                    )
                                }
                                Spacer(Modifier.width(Spacing.sm))
                                Text(fabLabel)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Game art behind the top of the form, like the play details in Journal.
            // It scrolls away with the header so fields never slide over the artwork.
            val listState = rememberLazyListState()
            GameBackdrop(
                imageUrl = viewModel.selectedGame?.thumbnailUrl,
                height = 220.dp,
                baseBlur = 1.5.dp,
                fadeTo = MaterialTheme.colorScheme.background,
                modifier = Modifier.graphicsLayer {
                    translationY = if (listState.firstVisibleItemIndex == 0) {
                        -listState.firstVisibleItemScrollOffset.toFloat()
                    } else {
                        -size.height
                    }
                }
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                // The title starts at the top; the art sits behind it and the first rows.
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                item {
                    // Only show passive AI hint when no actionable suggestion banner is visible.
                    val detectedGameHint = extractedPlay?.detectedGameTitle
                        ?.takeIf { title ->
                            title.isNotBlank() &&
                            !title.equals(gameName, ignoreCase = true) &&
                            gameCandidates.isEmpty()
                        }
                    val locationSuggestions = remember(historyPlays) {
                        historyPlays.map { it.location.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sortedBy { it.lowercase() }
                    }
                    SessionDetailsCard(
                        onChooseGame = onChooseGame.takeIf { !hasGame },
                        gameName = headerGameName,
                        thumbnailUrl = viewModel.selectedGame?.thumbnailUrl,
                        detectedGameHint = detectedGameHint,
                        date = date,
                        duration = duration,
                        location = location,
                        notes = comments,
                        showAdvanced = showAdvanced,
                        quantity = quantity,
                        incomplete = incomplete,
                        nowInStats = nowInStats,
                        locationSuggestions = locationSuggestions,
                        onDateClick = { showDatePicker = true },
                        onDurationChange = { duration = it },
                        onLocationChange = { location = it },
                        onNotesChange = { comments = it },
                        onAdvancedToggle = { showAdvanced = !showAdvanced },
                        onQuantityDecrease = { if (quantity > 1) quantity-- },
                        onQuantityIncrease = { quantity++ },
                        onIncompleteChange = { incomplete = it },
                        onNowInStatsChange = { nowInStats = it }
                    )
                }

                gameRelations?.let { relations ->
                    val relatedGames = if (relations.isExpansion) relations.baseGames else relations.expansions
                    if (relatedGames.isNotEmpty()) {
                        item {
                            RelatedGamesBanner(
                                relations = relations,
                                additionalGames = additionalGames,
                                onToggleGame = { viewModel.toggleAdditionalGame(it) }
                            )
                        }
                    }
                }

                scanRecognitionResult?.let { result ->
                    item {
                        ScanResultBanner(
                            result = result,
                            hasPreselectedGame = scanStartedWithGame,
                            onDismiss = { viewModel.dismissScanRecognitionResult() },
                            onChooseGame = onChooseGame
                        )
                    }
                }

                if (gameCandidates.isNotEmpty()) {
                    item {
                        GameSuggestionBanner(
                            candidate = gameCandidates.first(),
                            geminiConfidence = extractedPlay?.detectedGameConfidence,
                            detectionEvidence = extractedPlay?.gameDetectionEvidence,
                            hasPreselectedGame = scanStartedWithGame,
                            onAccept = { viewModel.acceptGameSuggestion(gameCandidates.first().game) },
                            onDismiss = { viewModel.dismissGameSuggestion() },
                            onChooseGame = onChooseGame
                        )
                    }
                }

                if (scanRetryResult != null) {
                    item {
                        ScanRetryBanner(
                            onApply = { viewModel.acceptRetryResult() },
                            onDismiss = { viewModel.dismissRetryResult() }
                        )
                    }
                }

                item {
                    PlayersHeader(
                        playerCount = players.size,
                        // Manual entry has no AI text worth showing.
                        hasAiOutput = extractedPlay?.modelUsed != null,
                        onToggleAiOutput = { showAiOutput = !showAiOutput },
                        onScan = onScan
                    )
                }

                val extracted = extractedPlay
                if (showAiOutput && extracted != null) {
                    item {
                        AiOutputCard(rawText = extracted.rawText, modelUsed = extracted.modelUsed)
                    }
                }

                itemsIndexed(
                    items = players,
                    key = { index, _ -> playerRowKeys.getOrElse(index) { "player-$index" } }
                ) { index, player ->
                    PlayerEditCard(
                        player = player,
                        rosterPlayers = rosterPlayers,
                        onUpdate = { viewModel.updatePlayer(index, it) },
                        onRemove = {
                            if (index < collapsedPlayers.size) {
                                collapsedPlayers = collapsedPlayers.toMutableList().also { it.removeAt(index) }
                            }
                            if (index < playerRowKeys.size) {
                                playerRowKeys = playerRowKeys.toMutableList().also { it.removeAt(index) }
                            }
                            viewModel.removePlayer(index)
                        },
                        collapsed = collapsedPlayers.getOrElse(index) { false },
                        onToggleCollapsed = {
                            collapsedPlayers = collapsedPlayers.toMutableList().also { it[index] = !it[index] }
                        },
                        requestScoreFocus = index == 0 && focusFirstScore,
                        onFocusDone = { focusFirstScore = false },
                        requestNameFocus = index == nameFieldFocusIndex,
                        onNameFocusDone = { nameFieldFocusIndex = -1 }
                    )
                }

                item {
                    AddPlayersRow(
                        frequentPlayers = frequentPlayers,
                        recentPlayers = recentPlayers,
                        onAddPlayer = {
                            collapseCompletePlayers()
                            // A saved player needs only a score, so it is added as a single line.
                            collapsedPlayers = collapsedPlayers + true
                            playerRowKeys = playerRowKeys + java.util.UUID.randomUUID().toString()
                            viewModel.addPlayerFromRoster(it)
                        },
                        onNewPlayer = addEditablePlayer
                    )
                }
            }
            } // end backdrop Box
        }
    }

    // Post-save overlay — keeps form visible underneath, dims background
    AnimatedVisibility(
        visible = postSaveInfo != null,
        enter   = fadeIn(tween(180)),
        exit    = fadeOut(tween(160))
    ) {
        val info = lastPostSaveInfo ?: return@AnimatedVisibility
        val nextRecommendations = remember(info.anchorPlay, historyPlays) {
            if (recommendationsEnabled) viewModel.getPostSaveRecommendations(info.anchorPlay) else emptyList()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            PostSaveCard(
                info = info,
                nextRecommendations = nextRecommendations,
                challengeAdvances = info.challengeAdvances,
                stillActive = info.stillActive,
                onEditPlay = {
                    postSaveInfo = null
                    onEditPlay(info.anchorPlay)
                },
                onPlayAgain = {
                    viewModel.setupPlayAgain(info.sessionContext)
                    date = LocalDate.now().toString()
                    location = info.sessionContext.location
                    duration = ""
                    comments = ""
                    errorMsg = null
                    focusFirstScore = true
                    postSaveInfo = null
                },
                onChangeGame = {
                    viewModel.setupChangeGameSession(info.sessionContext)
                    postSaveInfo = null
                    onChangeGame()
                },
                onPickRecommendation = { game ->
                    viewModel.setupChangeGameSession(info.sessionContext)
                    viewModel.selectGame(game)
                    postSaveInfo = null
                    onPickRecommendation(game)
                },
                onDone = {
                    postSaveInfo = null
                    onPosted()
                }
            )
            FireworksLayer(
                primaryColor = MaterialTheme.colorScheme.primary,
                modifier     = Modifier.fillMaxSize()
            )
        }
    }

    sessionHubInfo?.let { info ->
        SessionHubDialog(
            session = historyPlays.deriveSessionHub(info.anchorPlay, sessionHubTitle),
            players = rosterPlayers,
            onDismiss = { sessionHubInfo = null },
            onRenameSession = { sessionId, title ->
                viewModel.renameSession(
                    sessionId = sessionId,
                    newTitle = title,
                    onSuccess = { savedTitle -> sessionHubTitle = savedTitle.ifBlank { null } }
                )
            },
            onPlayAgain = { session ->
                viewModel.setupPlayAgainFromSession(session.plays)
                sessionHubInfo = null
                postSaveInfo = null
                date = LocalDate.now().toString()
                location = session.plays.firstOrNull()?.location ?: ""
                duration = ""
                comments = ""
                errorMsg = null
                focusFirstScore = true
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Compact form composables
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionDetailsCard(
    gameName: String,
    thumbnailUrl: String?,
    detectedGameHint: String? = null,
    date: String,
    duration: String,
    location: String,
    notes: String,
    showAdvanced: Boolean,
    quantity: Int,
    incomplete: Boolean,
    nowInStats: Boolean,
    locationSuggestions: List<String> = emptyList(),
    onDateClick: () -> Unit,
    onDurationChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAdvancedToggle: () -> Unit,
    onQuantityDecrease: () -> Unit,
    onQuantityIncrease: () -> Unit,
    onIncompleteChange: (Boolean) -> Unit,
    onNowInStatsChange: (Boolean) -> Unit,
    onChooseGame: (() -> Unit)? = null
) {
    val titleShadow = Shadow(color = Color.Black.copy(alpha = 0.7f), blurRadius = 16f)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalAlignment = Alignment.Bottom
        ) {
            GameCover(name = gameName, thumbnailUrl = thumbnailUrl, size = 72.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "New play",
                    style = MaterialTheme.typography.labelLarge.copy(shadow = titleShadow),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                Text(
                    gameName,
                    style = MaterialTheme.typography.headlineMedium.copy(shadow = titleShadow),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (!detectedGameHint.isNullOrBlank()) {
                    Text(
                        "AI detected: $detectedGameHint",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (onChooseGame != null) {
            BoardFlowButton(onClick = onChooseGame) { Text("Choose game") }
        }

        BoardFlowFormGroup {
            BoardFlowFormRow(label = "Date", icon = Icons.Default.CalendarMonth, onClick = onDateClick) {
                Text(
                    formatDisplayDate(date),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Pick date",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BoardFlowFormDivider()
            BoardFlowFormRow(label = "Duration", icon = Icons.Default.Schedule) {
                BoardFlowInlineField(
                    value = duration,
                    onValueChange = onDurationChange,
                    placeholder = "Minutes",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                if (duration.isNotBlank()) {
                    Text(
                        "min",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            BoardFlowFormDivider()
            var locationFocused by remember { mutableStateOf(false) }
            BoardFlowFormRow(label = "Location", icon = Icons.Default.Place) {
                BoardFlowInlineField(
                    value = location,
                    onValueChange = onLocationChange,
                    placeholder = "Where did you play?",
                    modifier = Modifier.weight(1f).onFocusChanged { locationFocused = it.isFocused }
                )
            }
            val visibleSuggestions = remember(locationSuggestions, location, locationFocused) {
                if (!locationFocused) emptyList()
                else locationSuggestions.filter {
                    it.contains(location.trim(), ignoreCase = true) && !it.equals(location.trim(), ignoreCase = true)
                }.take(5)
            }
            if (visibleSuggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    visibleSuggestions.forEach { suggestion ->
                        BoardFlowFilterChip(
                            selected = false,
                            onClick = { onLocationChange(suggestion) },
                            label = { Text(suggestion) }
                        )
                    }
                }
            }
        }

        BoardFlowFormGroup {
            BoardFlowFormRow(
                label = "More options",
                icon = Icons.Default.Tune,
                labelWidth = null,
                onClick = onAdvancedToggle
            ) {
                Icon(
                    imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showAdvanced) "Hide options" else "Show options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = showAdvanced,
                enter = expandVertically() + fadeIn(tween(150)),
                exit = shrinkVertically() + fadeOut(tween(150))
            ) {
                Column {
                    BoardFlowFormDivider()
                    BoardFlowFormRow(label = "Notes") {
                        BoardFlowInlineField(
                            value = notes,
                            onValueChange = onNotesChange,
                            placeholder = "Anything worth remembering",
                            singleLine = false,
                            maxLines = 4,
                            modifier = Modifier.weight(1f).padding(vertical = Spacing.md)
                        )
                    }
                    BoardFlowFormDivider()
                    BoardFlowFormRow(label = "Quantity", labelWidth = null) {
                        BoardFlowIconButton(onClick = onQuantityDecrease, enabled = quantity > 1) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(24.dp),
                            textAlign = TextAlign.Center
                        )
                        BoardFlowIconButton(onClick = onQuantityIncrease) {
                            Icon(BoardFlowIcons.Add, contentDescription = "Increase")
                        }
                    }
                    BoardFlowFormDivider()
                    BoardFlowFormRow(label = "Incomplete play", labelWidth = null) {
                        Switch(checked = incomplete, onCheckedChange = onIncompleteChange)
                    }
                    BoardFlowFormDivider()
                    BoardFlowFormRow(label = "Count in BGG stats", labelWidth = null) {
                        Switch(checked = nowInStats, onCheckedChange = onNowInStatsChange)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayersHeader(
    playerCount: Int,
    hasAiOutput: Boolean,
    onToggleAiOutput: () -> Unit,
    onScan: () -> Unit
) {
    BoardFlowSectionTitle(
        title = "Players",
        supporting = if (playerCount > 0) "Tap the trophy to mark the winner" else "Add who played, or scan the scoresheet",
        modifier = Modifier.padding(top = Spacing.sm)
    ) {
        if (hasAiOutput) {
            BoardFlowInlineAction(onClick = onToggleAiOutput) { Text("AI output") }
        }
        BoardFlowSecondaryButton(onClick = onScan) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(Dimens.Icon))
            Spacer(Modifier.width(Spacing.sm))
            Text("Scan")
        }
    }
}

@Composable
private fun PlayerEditCard(
    player: cz.nicolsburg.boardflow.model.PlayerResult,
    rosterPlayers: List<cz.nicolsburg.boardflow.model.Player>,
    onUpdate: (cz.nicolsburg.boardflow.model.PlayerResult) -> Unit,
    onRemove: () -> Unit,
    collapsed: Boolean = false,
    onToggleCollapsed: (() -> Unit)? = null,
    requestScoreFocus: Boolean = false,
    onFocusDone: () -> Unit = {},
    requestNameFocus: Boolean = false,
    onNameFocusDone: () -> Unit = {}
) {
    PlayerResultEditorCard(
        player = player,
        rosterPlayers = rosterPlayers,
        onUpdate = onUpdate,
        onRemove = onRemove,
        collapsed = collapsed,
        onToggleCollapsed = onToggleCollapsed,
        requestScoreFocus = requestScoreFocus,
        onFocusDone = onFocusDone,
        requestNameFocus = requestNameFocus,
        onNameFocusDone = onNameFocusDone
    )
}

private fun cz.nicolsburg.boardflow.model.PlayerResult.isReadyToCollapse(): Boolean =
    name.isNotBlank() && score.isNotBlank()

/** Saved players as one-tap pills, then a pill for someone new. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPlayersRow(
    frequentPlayers: List<BggPlayer>,
    recentPlayers: List<BggPlayer>,
    onAddPlayer: (BggPlayer) -> Unit,
    onNewPlayer: () -> Unit
) {
    val suggestions = (frequentPlayers + recentPlayers).distinctBy { it.id }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        suggestions.forEach { player ->
            AddPill(label = player.displayName, onClick = { onAddPlayer(player) }) {
                PlayerAvatar(player.displayName, size = 28.dp)
            }
        }
        // Same button as Scan; centred on the 48dp row of the player pills.
        BoardFlowSecondaryButton(onClick = onNewPlayer, modifier = Modifier.align(Alignment.CenterVertically)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(Dimens.IconSmall))
            Spacer(Modifier.width(6.dp))
            Text("New player")
        }
    }
}

@Composable
private fun AddPill(
    label: String,
    onClick: () -> Unit,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    leading: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = BoardFlowShape.Pill,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .padding(start = 6.dp, end = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            leading()
            Text(label, style = MaterialTheme.typography.labelLarge, color = labelColor)
        }
    }
}

@Composable
private fun AiOutputCard(rawText: String, modelUsed: String? = null) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Raw AI response",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (modelUsed != null) {
                        Text(
                            modelUsed,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                BoardFlowInlineAction(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(rawText))
                        copied = true
                    },
                    icon = Icons.Default.ContentCopy
                ) {
                    Text(if (copied) "Copied!" else "Copy")
                }
            }
            SelectionContainer {
                Text(
                    text = rawText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Post-save card (overlay)
// ---------------------------------------------------------------------------

@Composable
private fun PostSaveCard(
    info: PostSaveInfo,
    nextRecommendations: List<RecommendationPick>,
    onEditPlay: () -> Unit,
    onPlayAgain: () -> Unit,
    onChangeGame: () -> Unit,
    onPickRecommendation: (BggGame) -> Unit,
    onDone: () -> Unit,
    challengeAdvances: List<ChallengeAdvance> = emptyList(),
    stillActive: List<ChallengeProgress> = emptyList()
) {
    var animIn by remember { mutableStateOf(false) }
    var recommendationsExpanded by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { animIn = true }

    val players = info.sessionContext.players
    val winners = players.filter { it.isWinner }
    // Pick one line per post-save card. Wrapped in remember(info) so the random choice stays put
    // across recompositions instead of reshuffling on every frame.
    val primaryMemory = remember(info) {
        info.record?.displayText ?: run {
            val winner = winners.firstOrNull()?.name?.trim().orEmpty()
            val winnerNames = winners.joinToString(" & ") { it.name.trim() }
            when {
                winners.size == 1 -> listOf(
                    "$winner takes the crown.",
                    "$winner wins it.",
                    "Victory goes to $winner.",
                    "$winner comes out on top.",
                    "$winner claims this one.",
                    "The table bows to $winner.",
                    "$winner carries the day."
                )
                winners.size > 1 -> listOf(
                    "$winnerNames share the victory.",
                    "$winnerNames share the spoils.",
                    "A shared triumph for $winnerNames.",
                    "$winnerNames tie for the win.",
                    "No clear champion — $winnerNames split it."
                )
                players.isNotEmpty() -> listOf(
                    "Session recorded. The chronicle grows.",
                    "Another entry in the chronicle.",
                    "The story gets one play longer.",
                    "Logged. The legend continues.",
                    "One more for the books.",
                    "Another night at the table, remembered."
                )
                else -> listOf(
                    "This play is part of the record now.",
                    "Logged for posterity.",
                    "It's in the record now.",
                    "Noted for the chronicle."
                )
            }.random()
        }
    }
    val hasNumericScores = players.any { it.score.trim().toDoubleOrNull() != null }
    val sortedPlayers = if (hasNumericScores) {
        players.sortedByDescending { it.score.trim().toDoubleOrNull() ?: Double.NEGATIVE_INFINITY }
    } else {
        players
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val maxCardHeight = maxHeight
        AnimatedVisibility(
            visible = animIn,
            enter = slideInVertically(
                initialOffsetY = { it / 6 },
                animationSpec  = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            ) + scaleIn(
                initialScale   = 0.96f,
                animationSpec  = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium))
        ) {
            Box {
            Card(
                modifier  = Modifier.fillMaxWidth().heightIn(max = maxCardHeight),
                shape     = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                val heroColor = MaterialTheme.colorScheme.primary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(heroColor.copy(alpha = 0.18f), Color.Transparent),
                                    endY   = size.height * 0.46f
                                )
                            )
                        }
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Game name — all-caps label at the very top
                    if (info.sessionContext.gameName.isNotBlank()) {
                        Text(
                            info.sessionContext.gameName.uppercase(),
                            style         = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.5.sp,
                            color         = MaterialTheme.colorScheme.primary,
                            textAlign     = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    // Hero badge — layered glow rings
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(heroColor.copy(alpha = 0.07f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(heroColor.copy(alpha = 0.13f), CircleShape)
                        )
                        Box(
                            modifier         = Modifier
                                .size(50.dp)
                                .background(heroColor.copy(alpha = 0.20f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint               = heroColor,
                                modifier           = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Primary message
                    Text(
                        primaryMemory,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        textAlign  = TextAlign.Center,
                        lineHeight = 30.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    // Date — day name adds warmth over a bare date
                    val dateLabel = buildString {
                        append(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d")))
                        val location = info.sessionContext.location
                        if (location.isNotBlank()) append(" · $location")
                    }
                    Text(
                        dateLabel,
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                        textAlign = TextAlign.Center
                    )

                    // Player results
                    if (sortedPlayers.isNotEmpty()) {
                        Spacer(Modifier.height(22.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Spacer(Modifier.height(14.dp))
                        Column(
                            modifier            = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sortedPlayers.forEach { player ->
                                VictoryPlayerRow(player = player)
                            }
                        }
                    }

                    val hasAnyChallengeData = challengeAdvances.isNotEmpty() || stillActive.isNotEmpty()
                    if (hasAnyChallengeData) {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Challenges",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )

                                // Completed advances — prominent accent cards
                                val pc  = MaterialTheme.colorScheme.primaryContainer
                                val opc = MaterialTheme.colorScheme.onPrimaryContainer
                                val pri = MaterialTheme.colorScheme.primary
                                challengeAdvances.filter { it.isNewlyComplete }.forEach { advance ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = pc.copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, pri.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(pri.copy(alpha = 0.22f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.EmojiEvents,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = pri
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    advance.challenge.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = opc,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "Complete!",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = pri
                                                )
                                            }
                                        }
                                    }
                                }

                                // Progress advances — in-progress card, one step below completion
                                challengeAdvances.filter { !it.isNewlyComplete }.take(2).forEach { advance ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = pc.copy(alpha = 0.30f),
                                        border = BorderStroke(1.dp, pri.copy(alpha = 0.20f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .background(pri.copy(alpha = 0.16f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(13.dp),
                                                        tint = pri
                                                    )
                                                }
                                                Text(
                                                    advance.challenge.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = pc.copy(alpha = 0.80f)
                                                ) {
                                                    Text(
                                                        "${advance.to} / ${advance.goal}",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = opc
                                                    )
                                                }
                                            }
                                            LinearProgressIndicator(
                                                progress = { advance.to.toFloat() / advance.goal.coerceAtLeast(1) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(999.dp)),
                                                color = pri,
                                                trackColor = pc.copy(alpha = 0.50f)
                                            )
                                        }
                                    }
                                }

                                // Still-active — muted compact rows, lower visual tier
                                val activeSlots = (3 - challengeAdvances.size).coerceIn(0, 2)
                                if (activeSlots > 0 && stillActive.isNotEmpty()) {
                                    if (challengeAdvances.isNotEmpty()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                        )
                                        Text(
                                            "Still in progress",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    stillActive.take(activeSlots).forEach { progress ->
                                        val fraction = progress.currentCount.toFloat() / progress.goalCount.coerceAtLeast(1)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    progress.challenge.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                LinearProgressIndicator(
                                                    progress = { fraction },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(3.dp)
                                                        .clip(RoundedCornerShape(999.dp)),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f),
                                                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)
                                                )
                                            }
                                            Text(
                                                "${progress.currentCount} / ${progress.goalCount}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Primary CTA — most common next action
                    BoardFlowButton(
                        onClick = onPlayAgain,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Play again")
                    }

                    Spacer(Modifier.height(8.dp))

                    // Secondary actions: side by side, equal priority
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        // The card is already a raised tone, so these sit one step lighter.
                        val onCard = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                        BoardFlowSecondaryButton(onClick = onEditPlay, colors = onCard) { Text("Edit play") }
                        BoardFlowSecondaryButton(onClick = onChangeGame, colors = onCard) { Text("Change game") }
                    }

                    // Ghost dismiss — de-emphasised so the eye skips it unless intended
                    if (nextRecommendations.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { recommendationsExpanded = !recommendationsExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Try next",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    if (recommendationsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AnimatedVisibility(visible = recommendationsExpanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    nextRecommendations.take(2).forEach { pick ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f)),
                                            modifier = Modifier.clickable { onPickRecommendation(pick.game) }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        pick.game.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        pick.reason,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Icon(
                                                    Icons.Default.ChevronRight,
                                                    contentDescription = "Log ${pick.game.name}",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            BoardFlowIconButton(
                onClick = onDone,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(32.dp)
            ) {
                BoardFlowCloseGlyph(contentDescription = "Close", modifier = Modifier.size(18.dp), iconSize = 18.dp)
            }
            }
        }
    }
}

@Composable
private fun VictoryPlayerRow(player: PlayerResult) {
    val isWinner = player.isWinner
    val primary  = MaterialTheme.colorScheme.primary
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = if (isWinner) primary.copy(alpha = 0.20f)
                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        border = if (isWinner) BorderStroke(1.dp, primary.copy(alpha = 0.55f)) else null
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent strip for winner
            if (isWinner) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(primary, primary.copy(alpha = 0.55f))
                            )
                        )
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start  = if (isWinner) 10.dp else 12.dp,
                        end    = 12.dp,
                        top    = 11.dp,
                        bottom = 11.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isWinner) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint               = primary,
                        modifier           = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                }
                Text(
                    player.name.trim(),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isWinner) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isWinner) primary else MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.weight(1f)
                )
                val score = player.score.trim()
                Text(
                    score.ifBlank { "—" },
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isWinner) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isWinner) primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Fireworks
// ---------------------------------------------------------------------------

private data class FireParticle(
    val cx: Float, val cy: Float,
    val angle: Float, val speed: Float,
    val color: Color, val radius: Float, val delay: Float
)

private fun buildFireworks(primary: Color): List<FireParticle> {
    val rng = Random(42)
    val palette = listOf(primary, primary, Color.White, Color(0xFFFFF59D))
    // Five burst origins concentrated at the top of the screen so particles
    // fan down through the card header without reaching the action buttons.
    val bursts = listOf(
        Triple(0.18f, 0.04f, 0.00f),
        Triple(0.82f, 0.06f, 0.14f),
        Triple(0.50f, 0.01f, 0.24f),
        Triple(0.33f, 0.13f, 0.36f),
        Triple(0.67f, 0.11f, 0.46f),
    )
    return buildList {
        for ((bx, by, bd) in bursts) {
            repeat(14) { i ->
                val base = i * (6.2832f / 14f)
                val jitter = (rng.nextFloat() - 0.5f) * 0.5f
                add(FireParticle(
                    cx     = bx,
                    cy     = by,
                    angle  = base + jitter,
                    speed  = 0.4f + rng.nextFloat() * 0.6f,
                    color  = palette[rng.nextInt(palette.size)],
                    radius = 3f + rng.nextFloat() * 3.5f,
                    delay  = bd + rng.nextFloat() * 0.04f
                ))
            }
        }
    }
}

@Composable
private fun FireworksLayer(primaryColor: Color, modifier: Modifier = Modifier) {
    val particles = remember(primaryColor) { buildFireworks(primaryColor) }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, animationSpec = tween(durationMillis = 1700, easing = LinearEasing))
    }
    val p = anim.value
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        for (particle in particles) {
            val t = ((p - particle.delay) / (1f - particle.delay)).coerceIn(0f, 1f)
            if (t <= 0f) continue
            val eased = 1f - (1f - t) * (1f - t)           // ease-out: fast burst, slow settle
            val dist  = w * 0.28f * particle.speed
            val dx    = cos(particle.angle) * dist * eased
            val dy    = sin(particle.angle) * dist * eased + dist * 0.28f * t * t  // gravity
            val alpha = ((1f - t * t) * 1.5f).coerceIn(0f, 1f)
            val r     = particle.radius * (1f - t * 0.4f)
            drawCircle(
                color  = particle.color.copy(alpha = alpha),
                radius = r,
                center = Offset(w * particle.cx + dx, h * particle.cy + dy)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Retry result banner — shown when a background re-extraction succeeded
// ---------------------------------------------------------------------------

@Composable
private fun ScanRetryBanner(onApply: () -> Unit, onDismiss: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = primary.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Scan retry got a cleaner result.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = primary
                )
            }
            Text(
                "Apply it to replace the current player data?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BoardFlowSecondaryButton(onClick = onDismiss) { Text("Dismiss") }
                BoardFlowButton(onClick = onApply) { Text("Apply update") }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Scan recognition result banner (non-blocking feedback after Quick Scan)
// ---------------------------------------------------------------------------

@Composable
private fun ScanResultBanner(
    result: ScanRecognitionResult,
    hasPreselectedGame: Boolean,
    onDismiss: () -> Unit,
    onChooseGame: () -> Unit
) {
    // Auto-dismiss the success state after 7 seconds to give time to read + act.
    if (result is ScanRecognitionResult.AutoSwitched) {
        LaunchedEffect(result) {
            kotlinx.coroutines.delay(7000)
            onDismiss()
        }
    }

    val isSuccess = result is ScanRecognitionResult.AutoSwitched
    val message = when (result) {
        is ScanRecognitionResult.AutoSwitched      -> "Detected and switched to ${result.gameName}"
        is ScanRecognitionResult.NoCollectionMatch -> "Detected ${result.detectedTitle}, but it is not in your collection"
        is ScanRecognitionResult.LowConfidence     -> "Could not confidently detect the game"
    }

    Surface(
        shape  = BoardFlowSurfaceTokens.ContentCardShape,
        color  = if (isSuccess)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSuccess)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    message,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = if (isSuccess) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                BoardFlowIconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(Dimens.Icon))
                }
            }
            when {
                isSuccess && hasPreselectedGame -> BoardFlowSecondaryButton(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Keep current") }

                isSuccess && !hasPreselectedGame -> BoardFlowSecondaryButton(
                    onClick  = onChooseGame,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Choose another game") }

                else -> BoardFlowSecondaryButton(
                    onClick  = onChooseGame,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Choose game") }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Game suggestion banner (shown when detection confidence is below auto-switch
// threshold or when there are multiple plausible candidates)
// ---------------------------------------------------------------------------

@Composable
private fun GameSuggestionBanner(
    candidate: GameCandidate,
    geminiConfidence: Float? = null,
    detectionEvidence: String? = null,
    hasPreselectedGame: Boolean = false,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    onChooseGame: () -> Unit = {}
) {
    val onSurfaceMuted = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
        border   = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Header row: label + dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Detected game",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceMuted.copy(alpha = 0.60f)
                )
                BoardFlowIconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss suggestion",
                        modifier = Modifier.size(Dimens.Icon)
                    )
                }
            }

            // Game title
            Text(
                candidate.game.name,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary
            )

            // Confidence + evidence
            val confidenceLabel = when {
                geminiConfidence != null && geminiConfidence >= 0.90f -> "High confidence"
                geminiConfidence != null && geminiConfidence >= 0.70f -> "Good match"
                geminiConfidence != null                              -> "Possible match"
                else                                                  -> null
            }
            val confPct = geminiConfidence?.let { "${(it * 100).toInt()}%" }
            val confLine = listOfNotNull(confidenceLabel, confPct).joinToString(" · ")
            if (confLine.isNotBlank()) {
                Text(
                    confLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceMuted.copy(alpha = 0.60f)
                )
            }
            if (!detectionEvidence.isNullOrBlank()) {
                Text(
                    detectionEvidence,
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceMuted.copy(alpha = 0.50f)
                )
            }

            // Actions — right-aligned, lightweight
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasPreselectedGame) {
                    BoardFlowInlineAction(onClick = onDismiss) { Text("Keep current") }
                } else {
                    BoardFlowInlineAction(onClick = onChooseGame) { Text("Choose another") }
                }
                Spacer(Modifier.width(4.dp))
                BoardFlowSecondaryButton(onClick = onAccept) { Text("Use this game") }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Related games banner (unchanged)
// ---------------------------------------------------------------------------

@Composable
private fun RelatedGamesBanner(
    relations: GameRelations,
    additionalGames: List<BggGame>,
    onToggleGame: (BggGame) -> Unit
) {
    val relatedGames = if (relations.isExpansion) relations.baseGames else relations.expansions
    val anySelected = relatedGames.any { game -> additionalGames.any { it.id == game.id } }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            if (anySelected) "Also logging, with the same players and scores"
            else if (relations.isExpansion) "Also log the base game"
            else "Also log an expansion",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            relatedGames.forEach { game ->
                BoardFlowFilterChip(
                    selected = additionalGames.any { it.id == game.id },
                    onClick = { onToggleGame(game) },
                    label = { Text(game.name) }
                )
            }
        }
    }
}
