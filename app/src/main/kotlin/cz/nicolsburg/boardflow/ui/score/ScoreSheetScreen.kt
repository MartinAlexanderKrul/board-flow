package cz.nicolsburg.boardflow.ui.score

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.model.CountRow
import cz.nicolsburg.boardflow.model.MajorityRow
import cz.nicolsburg.boardflow.model.PointsRow
import cz.nicolsburg.boardflow.model.ScoreEntry
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.ScoreSheetDraft
import cz.nicolsburg.boardflow.model.ScoreSheetResult
import cz.nicolsburg.boardflow.model.ScoreSheetScoring
import cz.nicolsburg.boardflow.ui.common.AnimatedDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowCard
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationKind
import cz.nicolsburg.boardflow.ui.common.BoardFlowFilterChip
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowTextField
import cz.nicolsburg.boardflow.ui.common.GameBackdrop
import cz.nicolsburg.boardflow.ui.common.GameCover
import cz.nicolsburg.boardflow.ui.common.PlayerAvatar
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.Spacing

private val ColumnWidth = 72.dp
private val AddColumnWidth = 56.dp
private val MinLabelWidth = 136.dp
/** Below this the labels go compact (smaller text and icons) so long names still wrap by word. */
private val CompactLabelWidth = 176.dp
private val NameRowHeight = 76.dp
private val RowHeight = 56.dp
private val RowWithHintHeight = 68.dp
private val SubRowHeight = 52.dp
private val FadeWidth = 28.dp

// Rows separate by tone, not lines: every other row and the total are a shade lighter.
private val BandTint = Color.White.copy(alpha = 0.035f)
private val TotalTint = Color.White.copy(alpha = 0.07f)

/**
 * One game's score pad, in the app's own look: the game art and name on top like the play form,
 * the pad on a plain tonal card, the winner in translucent amber. What is the game's own is the
 * rows (its [ScoreSheet]) and the drawn icons (its [ScoreSheetDesign]).
 *
 * A column per player, headed by their avatar (tap to rename or remove). Keyboard Next goes down a
 * column, then to the top of the next player's, and the column being typed in is marked. With more
 * players than fit, the columns scroll sideways together and fade at the edge that has more. Once
 * the names scroll off the top, a bar with every player's running total stays pinned there.
 * A tiebreaker row appears only while players are tied for the win.
 */
@Composable
fun ScoreSheetScreen(
    sheet: ScoreSheet,
    draft: ScoreSheetDraft,
    gameName: String,
    thumbnailUrl: String?,
    onDraftChange: (ScoreSheetDraft) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val design = remember(sheet.id) { scoreSheetDesign(sheet.id) }
    val result = remember(sheet, draft) { ScoreSheetScoring.score(sheet, draft) }
    val scroll = rememberScrollState()
    val hScroll = rememberScrollState()
    var confirmClear by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Int?>(null) }
    var focusedPlayer by remember { mutableStateOf<Int?>(null) }
    var activeCell by remember { mutableStateOf<CellActions?>(null) }
    var viewportTop by remember { mutableFloatStateOf(0f) }
    var namesBottom by remember { mutableFloatStateOf(Float.MAX_VALUE) }

    if (confirmClear) {
        BoardFlowConfirmationDialog(
            title = "Clear the score sheet?",
            message = "Every value typed on this sheet is removed. Player names stay.",
            confirmLabel = "Clear",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = { confirmClear = false; onClear() },
            onDismiss = { confirmClear = false }
        )
    }
    renaming?.let { index ->
        if (index in draft.players.indices) {
            PlayerNameDialog(
                initial = draft.players[index],
                canRemove = draft.players.size > 1,
                onSave = { onDraftChange(draft.renamed(index, it.trim())); renaming = null },
                onRemove = {
                    onDraftChange(draft.withoutPlayer(index))
                    if (focusedPlayer == index) focusedPlayer = null
                    renaming = null
                },
                onDismiss = { renaming = null }
            )
        } else {
            renaming = null
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { viewportTop = it.positionInRoot().y }
    ) {
        // Player columns keep their width; the label column takes what is left of the card.
        val canAddPlayer = draft.players.size < sheet.maxPlayers
        val columnsWidth = ColumnWidth * draft.players.size + if (canAddPlayer) AddColumnWidth else 0.dp
        val labelWidth = (maxWidth - Spacing.lg * 2 - columnsWidth).coerceAtLeast(MinLabelWidth)
        val geometry = GridGeometry(labelWidth, hScroll, if (canAddPlayer) AddColumnWidth else 0.dp)

        // Game art behind the header, scrolling away with it, like the play form.
        GameBackdrop(
            imageUrl = thumbnailUrl,
            height = 220.dp,
            baseBlur = 1.5.dp,
            fadeTo = MaterialTheme.colorScheme.background,
            modifier = Modifier.graphicsLayer { translationY = -scroll.value.toFloat() }
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .imePadding()
                .padding(horizontal = Spacing.lg)
                .padding(top = Spacing.lg, bottom = if (activeCell != null) 88.dp else Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SheetHeader(gameName, thumbnailUrl, design)

            BoardFlowCard(
                contentPadding = PaddingValues(vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Expansion switches sit on the card, not on the game art above it.
                if (sheet.modules.isNotEmpty()) {
                    ModuleChips(sheet, draft, onDraftChange)
                }
                SheetGrid(
                    sheet = sheet,
                    draft = draft,
                    result = result,
                    design = design,
                    geometry = geometry,
                    canAddPlayer = canAddPlayer,
                    focusedPlayer = focusedPlayer,
                    onFocusPlayer = { focusedPlayer = it },
                    onActiveCell = { activeCell = it },
                    onInactiveCell = { if (activeCell === it) activeCell = null },
                    onRename = { renaming = it },
                    onAddPlayer = {
                        onDraftChange(draft.withPlayer(""))
                        renaming = draft.players.size
                    },
                    onNamesBottom = { namesBottom = it },
                    onDraftChange = onDraftChange
                )
            }

            ResultSummary(draft, result)

            Text(
                "Totals and the winner go to the play form, where you can still change them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.xs)
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoardFlowInlineAction(
                    onClick = { confirmClear = true },
                    enabled = result.hasInput,
                    neutral = true,
                    large = true
                ) { Text("Clear sheet") }
                BoardFlowButton(onClick = onApply, enabled = result.hasInput) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(Dimens.Icon))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Use these scores")
                }
            }
        }

        // Names and running totals stay in view once the name row scrolls off the top.
        AnimatedVisibility(
            visible = namesBottom < viewportTop,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            PinnedTotals(draft, result, geometry, focusedPlayer)
        }

        AnimatedVisibility(
            visible = activeCell != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding(),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            SumBar(
                onPlus = { activeCell?.addPlus?.invoke() },
                onMinus = activeCell?.addMinus,
                onNext = { activeCell?.next?.invoke() }
            )
        }
    }
}

@Composable
private fun SheetHeader(gameName: String, thumbnailUrl: String?, design: ScoreSheetDesign) {
    val titleShadow = Shadow(color = Color.Black.copy(alpha = 0.7f), blurRadius = 16f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.Bottom
    ) {
        GameCover(name = gameName, thumbnailUrl = thumbnailUrl, size = 72.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SheetGlyph(design.emblem, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Score sheet",
                    style = MaterialTheme.typography.labelLarge.copy(shadow = titleShadow),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
            Text(
                gameName,
                style = MaterialTheme.typography.headlineMedium.copy(shadow = titleShadow),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleChips(
    sheet: ScoreSheet,
    draft: ScoreSheetDraft,
    onDraftChange: (ScoreSheetDraft) -> Unit
) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        for (module in sheet.modules) {
            val on = module.id in draft.enabledModules
            BoardFlowFilterChip(
                selected = on,
                onClick = {
                    onDraftChange(draft.copy(enabledModules = sheet.toggleModule(draft.enabledModules, module.id)))
                },
                label = { Text(module.name) },
                leadingIcon = if (on) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(Dimens.IconSmall)) }
                } else null
            )
        }
    }
}

@Composable
private fun SheetGrid(
    sheet: ScoreSheet,
    draft: ScoreSheetDraft,
    result: ScoreSheetResult,
    design: ScoreSheetDesign,
    geometry: GridGeometry,
    canAddPlayer: Boolean,
    focusedPlayer: Int?,
    onFocusPlayer: (Int) -> Unit,
    onActiveCell: (CellActions) -> Unit,
    onInactiveCell: (CellActions) -> Unit,
    onRename: (Int) -> Unit,
    onAddPlayer: () -> Unit,
    onNamesBottom: (Float) -> Unit,
    onDraftChange: (ScoreSheetDraft) -> Unit
) {
    val players = draft.players.indices.toList()

    // Keyboard Next goes down a player's column, then to the top of the next player's.
    val visibleRows = sheet.rowsFor(draft.enabledModules)
    val inputOrder = visibleRows.flatMap { it.inputKeys }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requester(p: Int, key: String) = requesters.getOrPut("$p|$key") { FocusRequester() }
    fun next(p: Int, key: String): FocusRequester? {
        val i = inputOrder.indexOf(key)
        return when {
            i >= 0 && i < inputOrder.lastIndex -> requester(p, inputOrder[i + 1])
            p < players.lastIndex && inputOrder.isNotEmpty() -> requester(p + 1, inputOrder.first())
            else -> null
        }
    }

    Column {
        // Names
        SheetLine(
            geometry,
            NameRowHeight,
            Color.Transparent,
            padTrailing = false,
            modifier = Modifier.onGloballyPositioned {
                onNamesBottom(it.positionInRoot().y + it.size.height)
            },
            label = {
                Text(
                    "Tap a player to rename",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        ) {
            for (p in players) {
                PlayerHeader(
                    name = draft.players[p],
                    placeholder = "Player ${p + 1}",
                    isWinner = p in result.winners,
                    isFocused = p == focusedPlayer,
                    onClick = { onRename(p) }
                )
            }
            if (canAddPlayer) {
                AddPlayerColumn(onAddPlayer)
            }
        }

        var band = true
        for (row in visibleRows) {
            val background = if (band) BandTint else Color.Transparent
            band = !band
            when (row) {
                is PointsRow, is CountRow -> {
                    val multiplier = (row as? CountRow)?.badge()
                    SheetLine(
                        geometry,
                        if (row.hint != null) RowWithHintHeight else RowHeight,
                        background,
                        label = { RowLabel(design, row.id, row.label, row.hint, badge = multiplier, compact = geometry.compact) }
                    ) {
                        for (p in players) {
                            InputCell(
                                entry = draft.entry(p, row.id),
                                focusRequester = requester(p, row.id),
                                next = next(p, row.id),
                                caption = countCaption(row, draft.value(p, row.id)),
                                allowNegative = row.allowsNegative,
                                onFocused = { onFocusPlayer(p); onActiveCell(it) },
                                onBlurred = onInactiveCell,
                                onEntry = { onDraftChange(draft.withEntry(p, row.id, it)) }
                            )
                        }
                    }
                }
                is MajorityRow -> {
                    SheetLine(
                        geometry,
                        if (row.hint != null) RowWithHintHeight else RowHeight,
                        background,
                        label = { RowLabel(design, row.id, row.label, row.hint, compact = geometry.compact) }
                    ) {
                        for (p in players) {
                            ComputedCell(result.rowPoints[row.id]?.getOrNull(p) ?: 0)
                        }
                    }
                    for (group in row.groups) {
                        val key = row.groupKey(group)
                        SheetLine(
                            geometry,
                            SubRowHeight,
                            background,
                            label = { RowLabel(design, key, group.label, hint = null, indent = true, compact = geometry.compact) }
                        ) {
                            for (p in players) {
                                InputCell(
                                    entry = draft.entry(p, key),
                                    focusRequester = requester(p, key),
                                    next = next(p, key),
                                    caption = result.groupPoints[key]?.getOrNull(p)?.takeIf { it > 0 }?.let { "+$it pts" },
                                    onFocused = { onFocusPlayer(p); onActiveCell(it) },
                                    onBlurred = onInactiveCell,
                                    onEntry = { onDraftChange(draft.withEntry(p, key, it)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Total
        SheetLine(geometry, RowHeight, TotalTint, label = {
            Text(
                "Total",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }) {
            for (p in players) {
                TotalCell(result.totals.getOrElse(p) { 0 }, isWinner = p in result.winners)
            }
        }

        // Tiebreakers, only while they matter
        for (tiebreaker in result.shownTiebreakers.filter { it.asks }) {
            val tied = result.tiedPlayers[tiebreaker.id].orEmpty()
            SheetLine(geometry, RowWithHintHeight, Color.Transparent, label = {
                RowLabel(design, tiebreaker.inputKey, tiebreaker.label, hint = "Tiebreaker", compact = geometry.compact)
            }) {
                for (p in players) {
                    if (p in tied) {
                        InputCell(
                            entry = draft.entry(p, tiebreaker.inputKey),
                            onFocused = { onFocusPlayer(p); onActiveCell(it) },
                            onBlurred = onInactiveCell,
                            onEntry = { onDraftChange(draft.withEntry(p, tiebreaker.inputKey, it)) }
                        )
                    } else {
                        Box(Modifier.width(ColumnWidth), contentAlignment = Alignment.Center) {
                            Text("–", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shared by every row so the columns line up and scroll as one. [trailing] is the width of the
 * name row's Add column: every other row leaves the same space, so all rows are equally wide
 * (one scroll position is shared, and a narrower row would cap how far it can go).
 */
/** "×2" for pearls, "÷2" for 1 point per 2, "n²" for squared counts; none for 1 point each. */
private fun CountRow.badge(): String? = when {
    squared -> "n²"
    per > 1 -> "÷$per"
    pointsEach != 1 -> "×$pointsEach"
    else -> null
}

/** Points under a counted cell when they differ from the count ("= 6"). */
private fun countCaption(row: cz.nicolsburg.boardflow.model.ScoreRow, count: Int?): String? {
    val counted = row as? CountRow ?: return null
    if (counted.badge() == null || count == null) return null
    return "= ${counted.points(count)}"
}

private class GridGeometry(val labelWidth: Dp, val hScroll: ScrollState, val trailing: Dp) {
    val compact: Boolean get() = labelWidth < CompactLabelWidth
}

@Composable
private fun SheetLine(
    geometry: GridGeometry,
    height: Dp,
    background: Color,
    modifier: Modifier = Modifier,
    /** False for the name row, which fills the trailing space with its Add column. */
    padTrailing: Boolean = true,
    label: @Composable () -> Unit,
    cells: @Composable RowScope.() -> Unit
) {
    // At least [height]; taller when a label and its hint need more lines.
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .background(background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(geometry.labelWidth)
                .padding(start = Spacing.lg, end = Spacing.xs, top = Spacing.sm, bottom = Spacing.sm),
            contentAlignment = Alignment.CenterStart
        ) { label() }
        Row(
            Modifier
                .weight(1f)
                .fadingEdges(geometry.hScroll)
                .horizontalScroll(geometry.hScroll),
            verticalAlignment = Alignment.CenterVertically
        ) {
            cells()
            if (padTrailing && geometry.trailing > 0.dp) Spacer(Modifier.width(geometry.trailing))
        }
    }
}

/** Fades the player columns out at an edge that has more columns past it. */
private fun Modifier.fadingEdges(state: ScrollState): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = FadeWidth.toPx()
        if (state.value > 0) {
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = 0f, endX = fade),
                size = Size(fade, size.height),
                blendMode = BlendMode.DstIn
            )
        }
        if (state.value < state.maxValue) {
            drawRect(
                Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - fade, endX = size.width),
                topLeft = Offset(size.width - fade, 0f),
                size = Size(fade, size.height),
                blendMode = BlendMode.DstIn
            )
        }
    }

@Composable
private fun RowLabel(
    design: ScoreSheetDesign,
    glyphKey: String,
    label: String,
    hint: String?,
    badge: String? = null,
    indent: Boolean = false,
    compact: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (indent) Spacer(Modifier.width(if (compact) Spacing.sm else Spacing.md))
        val glyph = design.glyphs[glyphKey]
        if (glyph != null) {
            SheetGlyph(glyph, Modifier.size(if (indent || compact) 18.dp else 24.dp))
            Spacer(Modifier.width(if (compact) Spacing.sm else Spacing.md))
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = if (indent || compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (badge != null) {
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** A player's avatar and name, centred over their column; the trophy marks the winner. */
@Composable
private fun PlayerHeader(
    name: String,
    placeholder: String,
    isWinner: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val shown = name.trim().ifEmpty { placeholder }
    Column(
        Modifier
            .width(ColumnWidth)
            .height(NameRowHeight)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            PlayerAvatar(name = shown, size = 32.dp)
            if (isWinner) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(18.dp)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Winner",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            shown,
            style = MaterialTheme.typography.labelMedium,
            color = when {
                isWinner -> MaterialTheme.colorScheme.primary
                name.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs)
        )
        // The column being typed in.
        Box(
            Modifier
                .padding(top = 3.dp)
                .width(20.dp)
                .height(2.dp)
                .background(
                    if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    BoardFlowShape.Pill
                )
        )
    }
}

@Composable
private fun AddPlayerColumn(onClick: () -> Unit) {
    Column(
        Modifier
            .width(AddColumnWidth)
            .height(NameRowHeight)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add player",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.Icon)
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text("Add", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(5.dp))
    }
}

/** What the bar above the keyboard does to the cell being typed in; [addMinus] only where points can go below 0. */
private class CellActions(val addPlus: () -> Unit, val addMinus: (() -> Unit)?, val next: () -> Unit)

/**
 * A number, or numbers joined by "+" that add up ("12+8+5"). While typing, the running sum shows
 * under the cell; once you leave it, the cell shows the total and the sum moves under it.
 */
@Composable
private fun InputCell(
    entry: String,
    onEntry: (String) -> Unit,
    onFocused: (CellActions) -> Unit,
    onBlurred: (CellActions) -> Unit,
    focusRequester: FocusRequester? = null,
    /** Where Next goes; null closes the keyboard (tiebreakers just move down). */
    next: FocusRequester? = null,
    caption: String? = null,
    allowNegative: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val currentEntry by rememberUpdatedState(entry)
    val currentOnEntry by rememberUpdatedState(onEntry)
    var field by remember { mutableStateOf(TextFieldValue(entry, TextRange(entry.length))) }
    var focused by remember { mutableStateOf(false) }
    // Follow changes made elsewhere (Clear sheet, a removed player).
    if (!focused && field.text != entry) field = TextFieldValue(entry, TextRange(entry.length))

    fun goNext() {
        when {
            next != null -> next.requestFocus()
            focusRequester != null || !focusManager.moveFocus(FocusDirection.Down) -> focusManager.clearFocus()
        }
    }
    fun appendSign(sign: Char) {
        val signed = ScoreEntry.clean(field.text + sign, allowNegative)
        if (signed != field.text) {
            field = TextFieldValue(signed, TextRange(signed.length))
            currentOnEntry(signed)
        }
    }
    val actions = remember {
        CellActions(
            addPlus = { appendSign('+') },
            addMinus = if (allowNegative) ({ appendSign('-') }) else null,
            next = { goNext() }
        )
    }

    val total = ScoreEntry.sum(entry)
    // A sum is anything other than a plain number ("12+8", "4-3"); "-3" alone is just a number.
    val isSum = entry.isNotEmpty() && entry != total?.toString()
    val shown = if (focused || !isSum) field else TextFieldValue(total?.toString().orEmpty())
    val sumCaption = when {
        !isSum -> null
        focused -> total?.let { "= $it" }
        else -> entry.trimEnd('+', '-')
    }
    val captionText = listOfNotNull(sumCaption, caption).joinToString("  ").ifEmpty { null }

    Column(Modifier.width(ColumnWidth), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(
            value = shown,
            onValueChange = { typed ->
                when {
                    !focused -> Unit
                    // Only the cursor moved.
                    typed.text == field.text -> field = typed
                    // The tap that focuses a summed cell echoes the total it was showing; keep the sum.
                    typed.text == shown.text -> Unit
                    else -> {
                        val cleaned = ScoreEntry.clean(typed.text, allowNegative)
                        field = if (cleaned == typed.text) typed else TextFieldValue(cleaned, TextRange(cleaned.length))
                        currentOnEntry(cleaned)
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { goNext() }),
            modifier = (if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged {
                    if (it.isFocused != focused) {
                        focused = it.isFocused
                        if (it.isFocused) {
                            field = TextFieldValue(currentEntry, TextRange(currentEntry.length))
                            onFocused(actions)
                        } else {
                            // A sum left ending in "+" counts as typed so far.
                            val trimmed = currentEntry.trimEnd('+', '-')
                            if (trimmed != currentEntry) currentOnEntry(trimmed)
                            onBlurred(actions)
                        }
                    }
                },
            decorationBox = { inner ->
                Box(
                    Modifier
                        .width(ColumnWidth - Spacing.lg)
                        .height(40.dp)
                        .background(
                            if (focused) MaterialTheme.colorScheme.surfaceContainerHighest
                            else MaterialTheme.colorScheme.surfaceVariant,
                            BoardFlowShape.Cover
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (shown.text.isEmpty()) {
                        Text(
                            "–",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    inner()
                }
            }
        )
        if (captionText != null) {
            Text(
                captionText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSum && focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

/** Above the keyboard while a cell is typed in: "+" (number pads rarely have one) and Next. */
@Composable
private fun SumBar(onPlus: () -> Unit, onMinus: (() -> Unit)?, onNext: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                "Add up points: 12 + 8",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (onMinus != null) {
                BoardFlowSecondaryButton(onClick = onMinus) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(Dimens.Icon))
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Minus")
                }
            }
            BoardFlowSecondaryButton(onClick = onPlus) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(Dimens.Icon))
                Spacer(Modifier.width(Spacing.xs))
                Text("Plus")
            }
            BoardFlowInlineAction(onClick = onNext, neutral = true, large = true) { Text("Next") }
        }
    }
}

@Composable
private fun ComputedCell(points: Int) {
    Box(Modifier.width(ColumnWidth), contentAlignment = Alignment.Center) {
        Text(
            points.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TotalCell(total: Int, isWinner: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val fill by animateColorAsState(if (isWinner) primary.copy(alpha = 0.16f) else Color.Transparent, label = "totalFill")
    Box(Modifier.width(ColumnWidth), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .background(fill, BoardFlowShape.Pill)
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                total.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWinner) primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Every player's name and running total, pinned to the top while the rows scroll under it. */
@Composable
private fun PinnedTotals(
    draft: ScoreSheetDraft,
    result: ScoreSheetResult,
    geometry: GridGeometry,
    focusedPlayer: Int?
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = BoardFlowShape.Card,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
    ) {
        SheetLine(geometry, 52.dp, Color.Transparent, label = {
            Text(
                "Total so far",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }) {
            draft.players.forEachIndexed { p, raw ->
                val isWinner = p in result.winners
                val name = raw.trim().ifEmpty { "Player ${p + 1}" }
                Column(Modifier.width(ColumnWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        name,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isWinner || p == focusedPlayer -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs)
                    )
                    Text(
                        result.totals.getOrElse(p) { 0 }.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/** Renames a player column; a column can also be removed while another stays. */
@Composable
private fun PlayerNameDialog(
    initial: String,
    canRemove: Boolean,
    onSave: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AnimatedDialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text(
                if (initial.isBlank()) "Who is playing?" else "Player",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            BoardFlowTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave(name) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canRemove) {
                    BoardFlowInlineAction(onClick = onRemove, destructive = true, large = true) { Text("Remove") }
                }
                Spacer(Modifier.weight(1f))
                BoardFlowInlineAction(onClick = onDismiss, neutral = true, large = true) { Text("Cancel") }
                Spacer(Modifier.width(Spacing.sm))
                BoardFlowButton(onClick = { onSave(name) }) { Text("Save") }
            }
        }
    }
}

@Composable
private fun ResultSummary(draft: ScoreSheetDraft, result: ScoreSheetResult) {
    fun name(p: Int) = draft.players.getOrNull(p)?.trim().orEmpty().ifBlank { "Player ${p + 1}" }
    fun names(ps: Collection<Int>) = ps.map(::name).let {
        if (it.size <= 1) it.joinToString() else it.dropLast(1).joinToString(", ") + " and " + it.last()
    }

    val text = when {
        !result.hasInput -> "Fill in a column per player. Totals and the winner update as you go."
        result.pendingTiebreaker != null -> {
            val tied = result.tiedPlayers[result.pendingTiebreaker.id].orEmpty()
            "${names(tied)} are tied at ${result.totals[tied.first()]}. " +
                "Enter ${result.pendingTiebreaker.label.lowercase()} to break it."
        }
        result.winners.size > 1 -> "${names(result.winners)} share the win at ${result.totals[result.winners.first()]}."
        result.winners.size == 1 -> {
            val winner = result.winners.first()
            val decided = result.decidedBy?.let { ", on ${it.label.lowercase()}" }.orEmpty()
            "${name(winner)} wins with ${result.totals[winner]} points$decided."
        }
        else -> ""
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = Spacing.xs)
    )
}
