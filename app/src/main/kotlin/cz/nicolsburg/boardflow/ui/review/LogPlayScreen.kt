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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.AppViewModel
import cz.nicolsburg.boardflow.model.BggGame
import cz.nicolsburg.boardflow.model.GameRelations
import cz.nicolsburg.boardflow.model.RecordMoment
import cz.nicolsburg.boardflow.model.SessionContext
import cz.nicolsburg.boardflow.model.Player as BggPlayer
import cz.nicolsburg.boardflow.model.PlayerResult
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowCloseGlyph
import cz.nicolsburg.boardflow.ui.common.BoardFlowIconButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowIcons
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private data class PostSaveInfo(
    val sessionContext: SessionContext,
    val record: RecordMoment?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogPlayScreen(
    viewModel: AppViewModel,
    onPosted: () -> Unit,
    onChangeGame: () -> Unit,
    onNavigateBack: () -> Unit,
    onDiscard: () -> Unit = onNavigateBack
) {
    val players         by viewModel.editablePlayers.collectAsState()
    val posting         by viewModel.postLoading.collectAsState()
    val extractedPlay   by viewModel.extractedPlay.collectAsState()
    val gameRelations   by viewModel.gameRelations.collectAsState()
    val additionalGames by viewModel.additionalGames.collectAsState()

    // Read prefill once on first composition (consumed from ViewModel).
    val prefill = remember { viewModel.takePrefill() }

    var date           by remember { mutableStateOf(LocalDate.now().toString()) }
    var duration       by remember { mutableStateOf(prefill?.durationSuggestion ?: "") }
    var location       by remember { mutableStateOf(prefill?.location ?: "") }
    var comments       by remember { mutableStateOf("") }
    var quantity       by remember { mutableStateOf(1) }
    var incomplete     by remember { mutableStateOf(false) }
    var nowInStats     by remember { mutableStateOf(true) }
    var showAdvanced   by remember { mutableStateOf(false) }
    var errorMsg          by remember { mutableStateOf<String?>(null) }
    var showAiOutput      by remember { mutableStateOf(false) }
    var showDatePicker    by remember { mutableStateOf(false) }
    var focusFirstScore   by remember { mutableStateOf(false) }

    // Post-save card state. Non-null while post-save card is visible.
    var postSaveInfo     by remember { mutableStateOf<PostSaveInfo?>(null) }
    // Snapshot kept alive so the exit fade animation has content to render.
    var lastPostSaveInfo by remember { mutableStateOf<PostSaveInfo?>(null) }
    if (postSaveInfo != null) lastPostSaveInfo = postSaveInfo

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = runCatching {
            LocalDate.parse(LocalDate.now().toString())
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
    )

    if (showDatePicker) {
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
    val hasUnsavedChanges by remember(date, duration, location, comments, players, additionalGames, extractedPlay) {
        derivedStateOf {
            date != initialDate ||
                duration.isNotBlank() ||
                location.isNotBlank() ||
                comments.isNotBlank() ||
                players.isNotEmpty() ||
                additionalGames.isNotEmpty() ||
                extractedPlay != null
        }
    }

    LaunchedEffect(hasUnsavedChanges) {
        viewModel.setLogPlayHasUnsavedChanges(hasUnsavedChanges)
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(fabLabel) },
                icon = { Icon(Icons.Default.EmojiEvents, null) },
                onClick = {
                    if (!posting) {
                        errorMsg = null
                        val parsedDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
                        val durationMin = duration.toIntOrNull() ?: 0
                        viewModel.captureHistorySnapshot()
                        viewModel.postPlay(
                            date            = parsedDate,
                            durationMinutes = durationMin,
                            location        = location,
                            comments        = comments,
                            quantity        = quantity,
                            incomplete      = incomplete,
                            nowInStats      = nowInStats,
                            onSuccess       = {
                                val game = viewModel.selectedGame
                                if (game != null) {
                                    val ctx = SessionContext(
                                        gameId            = game.id,
                                        gameName          = game.name,
                                        players           = players,
                                        location          = location,
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
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Related games banner
            gameRelations?.let { relations ->
                val relatedGames = if (relations.isExpansion) relations.baseGames else relations.expansions
                if (relatedGames.isNotEmpty()) {
                    item {
                        RelatedGamesBanner(
                            relations       = relations,
                            additionalGames = additionalGames,
                            onToggleGame    = { viewModel.toggleAdditionalGame(it) }
                        )
                    }
                }
            }

            // Play details card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Play Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Date",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = date,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    trailingIcon = {
                                        BoardFlowIconButton(onClick = { showDatePicker = true }) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Column(
                                modifier = Modifier.width(100.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Duration (min)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = duration,
                                    onValueChange = { duration = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Location",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                placeholder = { Text("e.g. Game Night HQ") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Notes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = comments,
                                onValueChange = { comments = it },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Advanced options toggle
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvanced = !showAdvanced }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Advanced options",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showAdvanced) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = showAdvanced,
                            enter = expandVertically() + fadeIn(tween(150)),
                            exit = shrinkVertically() + fadeOut(tween(150))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Quantity stepper
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Quantity",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Log multiple identical plays at once",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        BoardFlowIconButton(
                                            onClick = { if (quantity > 1) quantity-- }
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                        }
                                        Text(
                                            "$quantity",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.width(24.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        BoardFlowIconButton(
                                            onClick = { quantity++ }
                                        ) {
                                            Icon(BoardFlowIcons.Add, contentDescription = "Increase")
                                        }
                                    }
                                }

                                // Incomplete toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Incomplete play",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Game wasn't finished",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Switch(checked = incomplete, onCheckedChange = { incomplete = it })
                                }

                                // Exclude from stats toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Count in stats",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Include this play in BGG statistics",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    Switch(checked = nowInStats, onCheckedChange = { nowInStats = it })
                                }
                            }
                        }
                    }
                }
            }

            // Players header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Players", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (extractedPlay != null) {
                            BoardFlowInlineAction(onClick = { showAiOutput = !showAiOutput }) {
                                Text(
                                    if (showAiOutput) "Hide AI output" else "AI output",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        BoardFlowIconButton(onClick = { viewModel.addPlayer() }) {
                            Icon(BoardFlowIcons.Add, "Add player")
                        }
                    }
                }
            }

            // Frequent player chips
            if (frequentPlayers.isNotEmpty() || recentPlayers.isNotEmpty()) {
                item {
                    FrequentPlayerChips(
                        gameName        = gameName,
                        frequentPlayers = frequentPlayers,
                        recentPlayers   = recentPlayers,
                        onAddPlayer     = { viewModel.addPlayerFromRoster(it) }
                    )
                }
            }

            // AI output debug card
            val extracted = extractedPlay
            if (showAiOutput && extracted != null) {
                item {
                    val clipboardManager = LocalClipboardManager.current
                    var copied by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Raw AI response",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                BoardFlowInlineAction(onClick = {
                                    clipboardManager.setText(AnnotatedString(extracted.rawText))
                                    copied = true
                                }, icon = Icons.Default.ContentCopy) {
                                    Text(if (copied) "Copied!" else "Copy")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            SelectionContainer {
                                Text(
                                    text = extracted.rawText,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Player rows
            itemsIndexed(players) { index, player ->
                PlayerRow(
                    player           = player,
                    onUpdate         = { viewModel.updatePlayer(index, it) },
                    onRemove         = { viewModel.removePlayer(index) },
                    suggestions      = remember(player.name) { viewModel.getPlayerSuggestions(player.name) },
                    requestScoreFocus = index == 0 && focusFirstScore,
                    onFocusDone      = { focusFirstScore = false }
                )
            }

            // Error card
            errorMsg?.let { message ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text  = message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
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
    } // end outer Box
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
// Related games banner (unchanged)
// ---------------------------------------------------------------------------

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

    val anySelected = relatedGames.any { game -> additionalGames.any { it.id == game.id } }

    Surface(
        color  = MaterialTheme.colorScheme.surfaceVariant,
        shape  = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                relatedGames.take(6).forEach { game ->
                    val selected = additionalGames.any { it.id == game.id }
                    FilterChip(
                        selected = selected,
                        onClick  = { onToggleGame(game) },
                        label    = { Text(game.name, style = MaterialTheme.typography.labelMedium) }
                    )
                }
                if (relatedGames.size > 6) {
                    Text(
                        "& ${relatedGames.size - 6} more",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.CenterVertically)
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

// ---------------------------------------------------------------------------
// Player row (unchanged)
// ---------------------------------------------------------------------------

@Composable
private fun PlayerRow(
    player: PlayerResult,
    onUpdate: (PlayerResult) -> Unit,
    onRemove: () -> Unit,
    suggestions: List<BggPlayer> = emptyList(),
    requestScoreFocus: Boolean = false,
    onFocusDone: () -> Unit = {}
) {
    val activeSuggestions: List<BggPlayer> = remember(suggestions, player.name) {
        suggestions.filter { it.displayName.lowercase() != player.name.lowercase().trim() }
    }
    val scoreFocusRequester = remember { FocusRequester() }
    LaunchedEffect(requestScoreFocus) {
        if (requestScoreFocus) {
            delay(150)
            runCatching { scoreFocusRequester.requestFocus() }
            onFocusDone()
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldBlock(label = "Name") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = player.name,
                        onValueChange = { onUpdate(player.copy(name = it)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    BoardFlowIconButton(onClick = { onUpdate(player.copy(isWinner = !player.isWinner)) }) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = "Toggle winner",
                            tint = if (player.isWinner) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }
                }
            }

            if (activeSuggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Match:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    activeSuggestions.forEach { p ->
                        SuggestionChip(
                            onClick = { onUpdate(player.copy(name = p.displayName)) },
                            label = { Text(p.displayName, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                FieldBlock(label = "Score", modifier = Modifier.width(100.dp)) {
                    OutlinedTextField(
                        value = player.score,
                        onValueChange = { onUpdate(player.copy(score = it)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().focusRequester(scoreFocusRequester)
                    )
                }
                FieldBlock(label = "Team / color", modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = player.color,
                        onValueChange = { onUpdate(player.copy(color = it)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldBlock(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}
