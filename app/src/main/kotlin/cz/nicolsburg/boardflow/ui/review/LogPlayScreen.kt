package cz.nicolsburg.boardflow.ui.review

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.AppViewModel
import cz.nicolsburg.boardflow.model.BggGame
import cz.nicolsburg.boardflow.model.GameCandidate
import cz.nicolsburg.boardflow.model.GameRelations
import cz.nicolsburg.boardflow.model.RecordMoment
import cz.nicolsburg.boardflow.model.ScanRecognitionResult
import cz.nicolsburg.boardflow.model.SessionContext
import cz.nicolsburg.boardflow.model.Player as BggPlayer
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowCloseGlyph
import cz.nicolsburg.boardflow.ui.common.BoardFlowIconButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowIcons
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import cz.nicolsburg.boardflow.ui.common.PlayerResultEditorCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private data class PostSaveInfo(
    val sessionContext: SessionContext,
    val record: RecordMoment?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LogPlayScreen(
    viewModel: AppViewModel,
    onPosted: () -> Unit,
    onChangeGame: () -> Unit,
    onNavigateBack: () -> Unit,
    onDiscard: () -> Unit = onNavigateBack,
    onChooseGame: () -> Unit = {}
) {
    val players         by viewModel.editablePlayers.collectAsState()
    val posting         by viewModel.postLoading.collectAsState()
    val extractedPlay   by viewModel.extractedPlay.collectAsState()
    val gameRelations   by viewModel.gameRelations.collectAsState()
    val additionalGames by viewModel.additionalGames.collectAsState()
    val rosterPlayers   by viewModel.players.collectAsState()
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
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

    val gameName = viewModel.selectedGame?.name ?: "Unknown game"

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

    BackHandler {
        if (postSaveInfo != null) {
            // Treat back as "Done" from post-save card
            viewModel.clearSession()
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
            collapsedPlayers.getOrElse(i) { false } || p.isReadyToCollapse()
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
        posting               -> "Posting..."
        !online && totalGames > 1 -> "Save $totalGames plays locally"
        !online               -> "Save Play Locally"
        totalGames > 1        -> "Log $totalGames plays to BGG"
        else                  -> "Log Play to BGG"
    }

    fun submitPlay() {
        if (!posting) {
            errorMsg = null
            val parsedDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
            val durationMin = duration.toIntOrNull() ?: 0
            viewModel.captureHistorySnapshot()
            viewModel.postPlay(
                date = parsedDate,
                durationMinutes = durationMin,
                location = location,
                comments = comments,
                quantity = quantity,
                incomplete = incomplete,
                nowInStats = nowInStats,
                onSuccess = {
                    val game = viewModel.selectedGame
                    if (game != null) {
                        val ctx = SessionContext(
                            gameId = game.id,
                            gameName = game.name,
                            players = players,
                            location = location,
                            lastPlayTimestamp = System.currentTimeMillis()
                        )
                        viewModel.saveSession(game, players, location)
                        val record = viewModel.detectRecord(game.id, game.name, players)
                        postSaveInfo = PostSaveInfo(ctx, record)
                    } else {
                        onPosted()
                    }
                },
                onError = { errorMsg = it }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (errorMsg != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = errorMsg.orEmpty(),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                        BoardFlowButton(
                            onClick = ::submitPlay,
                            enabled = !posting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (posting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(fabLabel)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 14.dp)
            ) {
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

                item {
                    // Only show passive AI hint when no actionable suggestion banner is visible.
                    val detectedGameHint = extractedPlay?.detectedGameTitle
                        ?.takeIf { title ->
                            title.isNotBlank() &&
                            !title.equals(gameName, ignoreCase = true) &&
                            gameCandidates.isEmpty()
                        }
                    SessionDetailsCard(
                        title = "Log Play",
                        gameName = gameName,
                        detectedGameHint = detectedGameHint,
                        date = date,
                        duration = duration,
                        location = location,
                        notes = comments,
                        showAdvanced = showAdvanced,
                        quantity = quantity,
                        incomplete = incomplete,
                        nowInStats = nowInStats,
                        onDateClick = { showDatePicker = true },
                        onDateChange = { date = it },
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
                        hasAiOutput = extractedPlay != null,
                        onToggleAiOutput = { showAiOutput = !showAiOutput },
                        onAddPlayer = addEditablePlayer
                    )
                }

                if (frequentPlayers.isNotEmpty() || recentPlayers.isNotEmpty()) {
                    item {
                        FrequentPlayerChips(
                            gameName = gameName,
                            frequentPlayers = frequentPlayers,
                            recentPlayers = recentPlayers,
                            onAddPlayer = {
                                collapseCompletePlayers()
                                collapsedPlayers = collapsedPlayers + false
                                playerRowKeys = playerRowKeys + java.util.UUID.randomUUID().toString()
                                viewModel.addPlayerFromRoster(it)
                            }
                        )
                    }
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
                    AddPlayerRow(onClick = addEditablePlayer)
                }
            }
        }
    }

    // Post-save overlay — keeps form visible underneath, dims background
    AnimatedVisibility(
        visible = postSaveInfo != null,
        enter   = fadeIn(tween(180)),
        exit    = fadeOut(tween(160))
    ) {
        val info = lastPostSaveInfo ?: return@AnimatedVisibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            PostSaveCard(
                info = info,
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
                onDone = {
                    viewModel.clearSession()
                    postSaveInfo = null
                    onPosted()
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Compact form composables
// ---------------------------------------------------------------------------

@Composable
private fun SessionDetailsCard(
    title: String,
    gameName: String,
    detectedGameHint: String? = null,
    date: String,
    duration: String,
    location: String,
    notes: String,
    showAdvanced: Boolean,
    quantity: Int,
    incomplete: Boolean,
    nowInStats: Boolean,
    onDateClick: () -> Unit,
    onDateChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAdvancedToggle: () -> Unit,
    onQuantityDecrease: () -> Unit,
    onQuantityIncrease: () -> Unit,
    onIncompleteChange: (Boolean) -> Unit,
    onNowInStatsChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompactGameHeader(title = title, gameName = gameName, detectedGameHint = detectedGameHint)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SessionFieldLabel("Date")
                    CompactTextField(
                        value = date,
                        onValueChange = onDateChange,
                        label = "Date",
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            BoardFlowIconButton(onClick = onDateClick, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date", modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                }
                Column(
                    modifier = Modifier.weight(0.7f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SessionFieldLabel("Duration (min)")
                    CompactTextField(
                        value = duration,
                        onValueChange = onDurationChange,
                        label = "Duration",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SessionFieldLabel("Location")
                CompactTextField(
                    value = location,
                    onValueChange = onLocationChange,
                    label = "Location",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TextButton(
                onClick = onAdvancedToggle,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (showAdvanced) "Hide options" else "More options")
                    Icon(
                        imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = showAdvanced,
                enter = expandVertically() + fadeIn(tween(150)),
                exit = shrinkVertically() + fadeOut(tween(150))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SessionFieldLabel("Notes")
                        CompactTextField(
                            value = notes,
                            onValueChange = onNotesChange,
                            label = "Notes",
                            singleLine = false,
                            minLines = 3,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    CompactStepperRow(
                        label = "Quantity",
                        value = quantity.toString(),
                        subtitle = "Log multiple identical plays"
                    ) {
                        BoardFlowIconButton(onClick = onQuantityDecrease) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            quantity.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(22.dp),
                            textAlign = TextAlign.Center
                        )
                        BoardFlowIconButton(onClick = onQuantityIncrease) {
                            Icon(BoardFlowIcons.Add, contentDescription = "Increase")
                        }
                    }
                    CompactSwitchRow(
                        label = "Incomplete play",
                        subtitle = "Game was not finished",
                        checked = incomplete,
                        onCheckedChange = onIncompleteChange
                    )
                    CompactSwitchRow(
                        label = "Count in stats",
                        subtitle = "Include this play in BGG statistics",
                        checked = nowInStats,
                        onCheckedChange = onNowInStatsChange
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionFieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
    )
}

@Composable
private fun CompactGameHeader(title: String, gameName: String, detectedGameHint: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                gameName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (!detectedGameHint.isNullOrBlank()) {
                Text(
                    "AI detected: $detectedGameHint",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
        placeholder = {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        },
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f),
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        ),
        modifier = modifier.height(if (singleLine) 52.dp else 92.dp)
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PlayersHeader(
    playerCount: Int,
    hasAiOutput: Boolean,
    onToggleAiOutput: () -> Unit,
    onAddPlayer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.then(
                if (hasAiOutput) {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onToggleAiOutput
                    )
                } else Modifier
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.People,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                if (playerCount > 0) "Players ($playerCount)" else "Players",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        BoardFlowIconButton(onClick = onAddPlayer) {
            Icon(Icons.Default.Add, contentDescription = "Add player", modifier = Modifier.size(20.dp))
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

@Composable
private fun AddPlayerRow(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Add player",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            )
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

@Composable
private fun CompactStepperRow(
    label: String,
    value: String,
    subtitle: String,
    controls: @Composable RowScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = controls
            )
        }
    }
}

@Composable
private fun CompactSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

// ---------------------------------------------------------------------------
// Post-save card (overlay)
// ---------------------------------------------------------------------------

@Composable
private fun PostSaveCard(
    info: PostSaveInfo,
    onPlayAgain: () -> Unit,
    onChangeGame: () -> Unit,
    onDone: () -> Unit
) {
    var animIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animIn = true }

    val players = info.sessionContext.players
    val winners = players.filter { it.isWinner }
    val hasNumericScores = players.any { it.score.trim().toDoubleOrNull() != null }
    val sortedPlayers = if (hasNumericScores) {
        players.sortedByDescending { it.score.trim().toDoubleOrNull() ?: Double.NEGATIVE_INFINITY }
    } else {
        players
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = animIn,
            enter = scaleIn(
                initialScale = 0.88f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium
                )
            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium))
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Game name
                    if (info.sessionContext.gameName.isNotBlank()) {
                        Text(
                            info.sessionContext.gameName,
                            style     = MaterialTheme.typography.labelMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Winner callout
                    if (winners.isNotEmpty()) {
                        val winnerText = winners.joinToString(" & ") { it.name.trim() }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "$winnerText wins!",
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color      = MaterialTheme.colorScheme.onSurface,
                                textAlign  = TextAlign.Center
                            )
                        }
                    }

                    // Score table
                    if (sortedPlayers.isNotEmpty()) {
                        HorizontalDivider()
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sortedPlayers.forEach { player ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (player.isWinner) {
                                        Icon(
                                            Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint     = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    } else {
                                        Spacer(Modifier.width(20.dp))
                                    }
                                    Text(
                                        player.name.trim(),
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (player.isWinner) FontWeight.SemiBold else FontWeight.Normal,
                                        color      = if (player.isWinner) MaterialTheme.colorScheme.primary
                                                     else MaterialTheme.colorScheme.onSurface,
                                        modifier   = Modifier.weight(1f)
                                    )
                                    val score = player.score.trim()
                                    Text(
                                        score.ifBlank { "—" },
                                        style      = if (player.isWinner) MaterialTheme.typography.bodyLarge
                                                     else MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (player.isWinner) FontWeight.SemiBold else FontWeight.Normal,
                                        color      = if (player.isWinner) MaterialTheme.colorScheme.primary
                                                     else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Record moment — integrated inline, no separate surface
                    info.record?.let { record ->
                        Text(
                            record.displayText,
                            style     = MaterialTheme.typography.labelMedium,
                            color     = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Actions
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BoardFlowButton(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
                            Text("Play again")
                        }
                        BoardFlowSecondaryButton(onClick = onChangeGame, modifier = Modifier.fillMaxWidth()) {
                            Text("Change game")
                        }
                        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Frequent player chips
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrequentPlayerChips(
    gameName: String,
    frequentPlayers: List<BggPlayer>,
    recentPlayers: List<BggPlayer>,
    onAddPlayer: (BggPlayer) -> Unit
) {
    val recentOnly = recentPlayers.filter { r -> frequentPlayers.none { it.id == r.id } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (frequentPlayers.isNotEmpty()) {
            Text(
                if (gameName.isNotBlank()) "Frequent for $gameName" else "Frequent players",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                frequentPlayers.forEach { player ->
                    PlayerChip(player = player, onClick = { onAddPlayer(player) })
                }
            }
        }
        if (recentOnly.isNotEmpty()) {
            Text(
                "Recent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                recentOnly.forEach { player ->
                    PlayerChip(player = player, onClick = { onAddPlayer(player) })
                }
            }
        }
    }
}

@Composable
private fun PlayerChip(player: BggPlayer, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label   = { Text(player.displayName, style = MaterialTheme.typography.labelMedium) },
        icon    = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(playerInitialColor(player.displayName), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = player.displayName.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                )
            }
        }
    )
}

private fun playerInitialColor(name: String): Color {
    val palette = listOf(
        Color(0xFF7C4DFF), Color(0xFF448AFF), Color(0xFF00ACC1),
        Color(0xFF43A047), Color(0xFFFF8F00), Color(0xFFE91E63),
        Color(0xFF795548), Color(0xFF546E7A)
    )
    return palette[(name.hashCode() and 0x7FFFFFFF) % palette.size]
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
        shape  = RoundedCornerShape(22.dp),
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
                BoardFlowIconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    BoardFlowCloseGlyph(
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(14.dp),
                        iconSize = 14.dp
                    )
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
                BoardFlowIconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    BoardFlowCloseGlyph(
                        contentDescription = "Dismiss suggestion",
                        modifier = Modifier.size(14.dp),
                        iconSize = 14.dp
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

@OptIn(ExperimentalLayoutApi::class)
private const val RELATED_GAMES_INITIAL_LIMIT = 6

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelatedGamesBanner(
    relations: GameRelations,
    additionalGames: List<BggGame>,
    onToggleGame: (BggGame) -> Unit
) {
    var dismissed by remember { mutableStateOf(false) }
    if (dismissed) return

    val relatedGames = if (relations.isExpansion) relations.baseGames else relations.expansions
    val label = if (relations.isExpansion) "Expansion - also post for base game?"
                else "Also post for an expansion?"
    val hasOverflow = relatedGames.size > RELATED_GAMES_INITIAL_LIMIT
    var expanded by rememberSaveable { mutableStateOf(false) }
    val visibleGames = if (!hasOverflow || expanded) relatedGames else relatedGames.take(RELATED_GAMES_INITIAL_LIMIT)
    val anySelected = relatedGames.any { game -> additionalGames.any { it.id == game.id } }

    Surface(
        color  = MaterialTheme.colorScheme.surfaceVariant,
        shape  = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { dismissed = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    BoardFlowCloseGlyph(
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        iconSize = 16.dp
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                visibleGames.forEach { game ->
                    val selected = additionalGames.any { it.id == game.id }
                    FilterChip(
                        selected = selected,
                        onClick  = { onToggleGame(game) },
                        label    = { Text(game.name, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            if (hasOverflow) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (expanded) "Show less"
                        else "Show all (${relatedGames.size})",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (anySelected) {
                Text(
                    "Ticked games will be posted automatically with the same players & scores.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
