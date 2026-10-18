package cz.nicolsburg.boardflow.ui.score

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.nicolsburg.boardflow.model.CountRow
import cz.nicolsburg.boardflow.model.MajorityRow
import cz.nicolsburg.boardflow.model.PointsRow
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.ScoreSheetDraft
import cz.nicolsburg.boardflow.model.ScoreSheetResult
import cz.nicolsburg.boardflow.model.ScoreSheetScoring
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.theme.Spacing

private val ColumnWidth = 68.dp
private val AddColumnWidth = 48.dp
private val MinLabelWidth = 132.dp
private val RowHeight = 52.dp
private val RowWithHintHeight = 64.dp
private val SubRowHeight = 44.dp

/**
 * One game's score pad: rows from its [ScoreSheet], the look from its [ScoreSheetDesign].
 * Values type into a column per player (keyboard Next goes down the column); totals, places and
 * the winner follow at once. A tiebreaker row appears only while players are tied for the win.
 */
@Composable
fun ScoreSheetScreen(
    sheet: ScoreSheet,
    draft: ScoreSheetDraft,
    onDraftChange: (ScoreSheetDraft) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val design = remember(sheet.id) { scoreSheetDesign(sheet.id) }
    val result = remember(sheet, draft) { ScoreSheetScoring.score(sheet, draft) }
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear the score sheet?") },
            text = { Text("Every value typed on this sheet is removed. Player names stay.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClear() }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = design.paper,
            border = BorderStroke(1.dp, design.line)
        ) {
            Column {
                design.header()
                if (sheet.modules.isNotEmpty()) {
                    ModuleToggles(sheet, draft, design, onDraftChange)
                }
                SheetGrid(sheet, draft, result, design, onDraftChange)
                ResultSummary(draft, result, design)
            }
        }

        BoardFlowButton(
            onClick = onApply,
            enabled = result.hasInput,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text("Use these scores")
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Totals and the winner go to the play form, where you can still change them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            BoardFlowInlineAction(onClick = { confirmClear = true }, enabled = result.hasInput, neutral = true) {
                Text("Clear sheet")
            }
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleToggles(
    sheet: ScoreSheet,
    draft: ScoreSheetDraft,
    design: ScoreSheetDesign,
    onDraftChange: (ScoreSheetDraft) -> Unit
) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (module in sheet.modules) {
            val on = module.id in draft.enabledModules
            val fill by animateColorAsState(if (on) design.accent else Color.Transparent, label = "moduleFill")
            Row(
                Modifier
                    .background(fill, RoundedCornerShape(50))
                    .border(1.dp, design.accent, RoundedCornerShape(50))
                    .clickable {
                        val modules = if (on) draft.enabledModules - module.id else draft.enabledModules + module.id
                        onDraftChange(draft.copy(enabledModules = modules))
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (on) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = design.onAccent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    module.name,
                    color = if (on) design.onAccent else design.accent,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
    HorizontalDivider(color = design.line)
}

@Composable
private fun SheetGrid(
    sheet: ScoreSheet,
    draft: ScoreSheetDraft,
    result: ScoreSheetResult,
    design: ScoreSheetDesign,
    onDraftChange: (ScoreSheetDraft) -> Unit
) {
    val hScroll = rememberScrollState()
    val players = draft.players.indices.toList()
    val canAddPlayer = draft.players.size < sheet.maxPlayers

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Player columns keep their width; the label column takes what is left (and the columns
        // scroll sideways, all rows together, when there are too many players to fit).
        val columnsWidth = ColumnWidth * players.size + if (canAddPlayer) AddColumnWidth else 0.dp
        val labelWidth = (maxWidth - columnsWidth).coerceAtLeast(MinLabelWidth)
        val geometry = GridGeometry(labelWidth, hScroll)

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
            SheetLine(geometry, RowHeight, design.paper, label = {}) {
                for (p in players) {
                    NameCell(
                        name = draft.players[p],
                        placeholder = "Player ${p + 1}",
                        isWinner = p in result.winners,
                        design = design,
                        onName = { onDraftChange(draft.renamed(p, it)) }
                    )
                }
                if (canAddPlayer) {
                    Box(Modifier.width(AddColumnWidth).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .border(1.dp, design.accent, RoundedCornerShape(50))
                                .clickable { onDraftChange(draft.withPlayer("")) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add player", tint = design.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            HorizontalDivider(color = design.line)

            var band = false
            for (row in visibleRows) {
                band = !band
                val background = if (band) design.paperBand else design.paper
                when (row) {
                    is PointsRow, is CountRow -> {
                        val multiplier = (row as? CountRow)?.pointsEach?.takeIf { it != 1 }
                        SheetLine(
                            geometry,
                            if (row.hint != null) RowWithHintHeight else RowHeight,
                            background,
                            label = {
                                RowLabel(design, row.id, row.label, row.hint, badge = multiplier?.let { "×$it" })
                            }
                        ) {
                            for (p in players) {
                                InputCell(
                                    value = draft.value(p, row.id),
                                    design = design,
                                    focusRequester = requester(p, row.id),
                                    next = next(p, row.id),
                                    caption = multiplier?.let { m -> draft.value(p, row.id)?.let { "= ${it * m}" } },
                                    onValue = { onDraftChange(draft.withValue(p, row.id, it)) }
                                )
                            }
                        }
                    }
                    is MajorityRow -> {
                        SheetLine(
                            geometry,
                            if (row.hint != null) RowWithHintHeight else RowHeight,
                            background,
                            label = { RowLabel(design, row.id, row.label, row.hint) }
                        ) {
                            for (p in players) {
                                ComputedCell(result.rowPoints[row.id]?.getOrNull(p) ?: 0, design, bold = true)
                            }
                        }
                        for (group in row.groups) {
                            val key = row.groupKey(group)
                            SheetLine(
                                geometry,
                                SubRowHeight,
                                background,
                                label = { RowLabel(design, key, group.label, hint = null, indent = true) }
                            ) {
                                for (p in players) {
                                    InputCell(
                                        value = draft.value(p, key),
                                        design = design,
                                        focusRequester = requester(p, key),
                                        next = next(p, key),
                                        compact = true,
                                        caption = result.groupPoints[key]?.getOrNull(p)?.takeIf { it > 0 }?.let { "+$it" },
                                        onValue = { onDraftChange(draft.withValue(p, key, it)) }
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = design.line)
            }

            // Total
            SheetLine(geometry, RowHeight, design.accent, label = {
                Text(
                    "Total",
                    color = design.onAccent,
                    fontFamily = design.titleFont,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }) {
                for (p in players) {
                    TotalCell(result.totals.getOrElse(p) { 0 }, isWinner = p in result.winners, design = design)
                }
            }

            // Tiebreakers, only while they matter
            for (tiebreaker in result.shownTiebreakers) {
                val tied = result.tiedPlayers[tiebreaker.id].orEmpty()
                HorizontalDivider(color = design.line)
                SheetLine(geometry, RowWithHintHeight, design.paperBand, label = {
                    RowLabel(design, tiebreaker.inputKey, tiebreaker.label, hint = "Tiebreaker")
                }) {
                    for (p in players) {
                        if (p in tied) {
                            InputCell(
                                value = draft.value(p, tiebreaker.inputKey),
                                design = design,
                                onValue = { onDraftChange(draft.withValue(p, tiebreaker.inputKey, it)) }
                            )
                        } else {
                            Box(Modifier.width(ColumnWidth), contentAlignment = Alignment.Center) {
                                Text("–", color = design.inkMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

private class GridGeometry(val labelWidth: Dp, val hScroll: ScrollState)

@Composable
private fun SheetLine(
    geometry: GridGeometry,
    height: Dp,
    background: Color,
    label: @Composable () -> Unit,
    cells: @Composable RowScope.() -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(background)
    ) {
        Box(
            Modifier
                .width(geometry.labelWidth)
                .fillMaxHeight()
                .padding(start = 12.dp, end = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) { label() }
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(geometry.hScroll),
            verticalAlignment = Alignment.CenterVertically,
            content = cells
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
    indent: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (indent) Spacer(Modifier.width(12.dp))
        val glyph = design.glyphs[glyphKey]
        if (glyph != null) {
            SheetGlyph(glyph, Modifier.size(if (indent) 20.dp else 28.dp))
            Spacer(Modifier.width(8.dp))
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    color = design.ink,
                    fontFamily = design.titleFont,
                    fontWeight = if (indent) FontWeight.Normal else FontWeight.SemiBold,
                    style = if (indent) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (badge != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        badge,
                        color = design.accent,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            if (hint != null) {
                Text(
                    hint,
                    color = design.inkMuted,
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 13.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NameCell(
    name: String,
    placeholder: String,
    isWinner: Boolean,
    design: ScoreSheetDesign,
    onName: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        Modifier.width(ColumnWidth).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.height(16.dp), contentAlignment = Alignment.Center) {
            if (isWinner) {
                Icon(Icons.Default.EmojiEvents, contentDescription = "Winner", tint = design.winner, modifier = Modifier.size(16.dp))
            }
        }
        BasicTextField(
            value = name,
            onValueChange = { onName(it.take(24)) },
            singleLine = true,
            textStyle = TextStyle(
                color = if (isWinner) design.winner else design.ink,
                fontFamily = design.titleFont,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(design.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.padding(horizontal = 4.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (name.isEmpty()) {
                        Text(placeholder, color = design.inkMuted, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun InputCell(
    value: Int?,
    design: ScoreSheetDesign,
    onValue: (Int?) -> Unit,
    focusRequester: FocusRequester? = null,
    /** Where Next goes; null closes the keyboard (tiebreakers just move down). */
    next: FocusRequester? = null,
    compact: Boolean = false,
    caption: String? = null
) {
    val focusManager = LocalFocusManager.current
    var text by remember(value) { mutableStateOf(value?.toString() ?: "") }
    Column(
        Modifier.width(ColumnWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicTextField(
            value = text,
            onValueChange = { typed ->
                val digits = typed.filter(Char::isDigit).take(4)
                text = digits
                onValue(digits.toIntOrNull())
            },
            singleLine = true,
            textStyle = TextStyle(
                color = design.ink,
                fontFamily = design.titleFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 15.sp else 18.sp,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(design.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                when {
                    next != null -> next.requestFocus()
                    focusRequester != null || !focusManager.moveFocus(FocusDirection.Down) -> focusManager.clearFocus()
                }
            }),
            modifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
            decorationBox = { inner ->
                Box(
                    Modifier
                        .width(ColumnWidth - 14.dp)
                        .height(if (compact) 30.dp else 36.dp)
                        .background(design.cell, RoundedCornerShape(8.dp))
                        .border(1.dp, design.line, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (text.isEmpty()) Text("·", color = design.inkMuted, fontSize = 18.sp)
                    inner()
                }
            }
        )
        if (caption != null) {
            Text(caption, color = design.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
        }
    }
}

@Composable
private fun ComputedCell(points: Int, design: ScoreSheetDesign, bold: Boolean = false) {
    Box(Modifier.width(ColumnWidth), contentAlignment = Alignment.Center) {
        Text(
            points.toString(),
            color = design.ink,
            fontFamily = design.titleFont,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun TotalCell(total: Int, isWinner: Boolean, design: ScoreSheetDesign) {
    val fill by animateColorAsState(if (isWinner) design.winner else Color.Transparent, label = "totalFill")
    Box(Modifier.width(ColumnWidth), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .background(fill, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                total.toString(),
                color = if (isWinner) design.onWinner else design.onAccent,
                fontFamily = design.titleFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun ResultSummary(draft: ScoreSheetDraft, result: ScoreSheetResult, design: ScoreSheetDesign) {
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
            val decided = result.decidedBy?.let { " — on ${it.label.lowercase()}" }.orEmpty()
            "${name(winner)} wins with ${result.totals[winner]} points$decided."
        }
        else -> ""
    }
    Text(
        text,
        color = design.ink,
        fontFamily = design.titleFont,
        fontStyle = FontStyle.Italic,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
    )
}
