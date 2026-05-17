package cz.nicolsburg.boardflow.ui.search

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import cz.nicolsburg.boardflow.model.BggGame
import cz.nicolsburg.boardflow.model.SessionContext
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import cz.nicolsburg.boardflow.ui.common.BoardFlowCloseGlyph
import cz.nicolsburg.boardflow.ui.common.BoardFlowIconButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowOutlinedButton
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
    onScanQuick: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.logPlaySearchResults.collectAsState()
    val loading by viewModel.searchLoading.collectAsState()
    val error   by viewModel.searchError.collectAsState()
    val collectionLoaded by viewModel.collectionLoaded.collectAsState()
    val sessionBannerVisible by viewModel.sessionBannerVisible.collectAsState()
    val sessionContext by viewModel.sessionContext.collectAsState()
    val changeGameActive by viewModel.changeGameSessionActive.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadLogPlayGames() }

    LaunchedEffect(query) {
        delay(800)
        viewModel.filterLogPlayGames(query)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Continue last session banner
        AnimatedVisibility(visible = sessionBannerVisible && !changeGameActive) {
            sessionContext?.let { ctx ->
                SessionContinueBanner(
                    context   = ctx,
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            GameSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search games...",
                modifier = Modifier.fillMaxWidth(),
                trailingAction = {
                    SearchFieldActionButton(onClick = onScanQuick) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Scan score",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            AnimatedVisibility(visible = changeGameActive) {
                Text(
                    "Changing game — players from last game will be kept",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            when {
                loading -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                ) {
                    items(10) { ShimmerGameRow() }
                }

                error != null -> Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val message = error.orEmpty()
                            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                            if (message.contains("private") || message.contains("401")) {
                                Text(
                                    "Tip: Make your BGG profile public in account settings, or use search mode.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    BoardFlowOutlinedButton(onClick = { viewModel.loadLogPlayGames() }) {
                        Text("Use recent games instead")
                    }
                }

                results.isEmpty() && query.isNotBlank() -> Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No games found for \"$query\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                results.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                        Text(
                            "Log a Play",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (collectionLoaded)
                                "Search for a game above or pick from your collection below."
                            else
                                "Search for a game above, or load your BGG collection in the Sync tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    val listState = rememberLazyListState()
                    val scope = rememberCoroutineScope()
                    val showScrollBar = results.size > 20
                    var dragState by remember { mutableStateOf<ScrollDragState?>(null) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(
                                top = 8.dp,
                                bottom = 8.dp,
                                end = if (showScrollBar) 20.dp else 0.dp
                            )
                        ) {
                            items(results) { game ->
                                GameRow(game = game, onClick = {
                                    viewModel.selectGame(game)
                                    onGameSelected(game)
                                })
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

@Composable
private fun SessionContinueBanner(
    context: SessionContext,
    onPlayAgain: () -> Unit,
    onContinueWithAnotherGame: () -> Unit,
    onStartNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val playerNames = context.players.take(3).joinToString(", ") { it.name.trim() }
        .let { if (context.players.size > 3) "$it +${context.players.size - 3}" else it }
    val elapsedMs = System.currentTimeMillis() - context.lastPlayTimestamp
    val elapsedLabel = when {
        elapsedMs < 60_000L    -> "just now"
        elapsedMs < 3_600_000L -> "${elapsedMs / 60_000}m ago"
        else                   -> "${elapsedMs / 3_600_000}h ago"
    }
    val subtitle = buildString {
        append(context.gameName)
        if (playerNames.isNotBlank()) append(" · $playerNames")
        append(" · $elapsedLabel")
    }

    Surface(
        color  = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "Keep this session going?",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                BoardFlowIconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    BoardFlowCloseGlyph(contentDescription = "Dismiss", modifier = Modifier.size(14.dp), iconSize = 14.dp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onPlayAgain) { Text("Play again") }
                TextButton(onClick = onContinueWithAnotherGame) { Text("Another game") }
                TextButton(onClick = onStartNew) { Text("Start new") }
            }
        }
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
        Box(Modifier.weight(1f).height(14.dp).background(shimmer, RoundedCornerShape(4.dp)))
        Box(Modifier.size(16.dp).background(shimmer, RoundedCornerShape(3.dp)))
    }
}

@Composable
private fun GameRow(game: BggGame, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(game.name, fontWeight = FontWeight.Medium)
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Log play",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    )
}

