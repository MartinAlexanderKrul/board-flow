package cz.nicolsburg.boardflow.ui.review

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
    var errorMsg       by remember { mutableStateOf<String?>(null) }
    var showAiOutput   by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Post-save card state. Non-null while post-save card is visible.
    var postSaveInfo by remember { mutableStateOf<PostSaveInfo?>(null) }

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

    // Show post-save card when present
    if (postSaveInfo != null) {
        val info = postSaveInfo!!
        PostSaveScreen(
            info = info,
            onPlayAgain = {
                viewModel.setupPlayAgain(info.sessionContext)
                date = LocalDate.now().toString()
                location = info.sessionContext.location
                duration = ""
                comments = ""
                errorMsg = null
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
        return
    }

    val online = viewModel.isOnline()
    val totalGames = 1 + additionalGames.size

    // Compute frequent players in composable scope so remember is valid.
    val gameId = viewModel.selectedGame?.id ?: 0
    val excludedNames = remember(players) { players.map { it.name.trim() }.toSet() }
    val frequentPlayers = remember(gameId, excludedNames) {
        viewModel.getFrequentPlayers(gameId, excludedNames)
    }
    val fabLabel = when {
        posting               -> "Posting..."
        !online && totalGames > 1 -> "Save $totalGames plays locally"
        !online               -> "Save Play Locally"
        totalGames > 1        -> "Log $totalGames plays to BGG"
        else                  -> "Log Play to BGG"
    }

    Scaffold(
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
            if (frequentPlayers.isNotEmpty()) {
                item {
                    FrequentPlayerChips(
                        players     = frequentPlayers,
                        onAddPlayer = { viewModel.addPlayerFromRoster(it) }
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
                    player      = player,
                    onUpdate    = { viewModel.updatePlayer(index, it) },
                    onRemove    = { viewModel.removePlayer(index) },
                    suggestions = remember(player.name) { viewModel.getPlayerSuggestions(player.name) }
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
}

// ---------------------------------------------------------------------------
// Post-save screen
// ---------------------------------------------------------------------------

@Composable
private fun PostSaveScreen(
    info: PostSaveInfo,
    onPlayAgain: () -> Unit,
    onChangeGame: () -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "Play saved",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Record moment (if any)
                info.record?.let { record ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text  = record.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }

                // Game context summary
                val playerSummary = info.sessionContext.players
                    .take(4).joinToString(", ") { it.name.trim() }
                    .let { if (info.sessionContext.players.size > 4) "$it +${info.sessionContext.players.size - 4}" else it }
                if (playerSummary.isNotBlank()) {
                    Text(
                        text  = "${info.sessionContext.gameName} · $playerSummary",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoardFlowButton(
                        onClick   = onPlayAgain,
                        modifier  = Modifier.fillMaxWidth()
                    ) {
                        Text("Play again")
                    }
                    BoardFlowSecondaryButton(
                        onClick  = onChangeGame,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change game")
                    }
                    TextButton(
                        onClick  = onDone,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
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
    players: List<cz.nicolsburg.boardflow.model.Player>,
    onAddPlayer: (cz.nicolsburg.boardflow.model.Player) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Frequent players",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement   = Arrangement.spacedBy(4.dp)
        ) {
            players.forEach { player ->
                SuggestionChip(
                    onClick = { onAddPlayer(player) },
                    label   = { Text(player.displayName, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    }
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
    suggestions: List<BggPlayer> = emptyList()
) {
    val activeSuggestions: List<BggPlayer> = remember(suggestions, player.name) {
        suggestions.filter { it.displayName.lowercase() != player.name.lowercase().trim() }
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
                        modifier = Modifier.fillMaxWidth()
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
