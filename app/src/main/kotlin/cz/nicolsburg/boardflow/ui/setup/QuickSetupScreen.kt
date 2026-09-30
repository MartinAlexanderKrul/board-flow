package cz.nicolsburg.boardflow.ui.setup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.nicolsburg.boardflow.model.GuideOrigin
import cz.nicolsburg.boardflow.model.GuideSectionKind
import cz.nicolsburg.boardflow.model.ResolvedSection
import cz.nicolsburg.boardflow.model.ResolvedStep
import cz.nicolsburg.boardflow.model.SetupGuideSource
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowFilterChip
import cz.nicolsburg.boardflow.ui.common.SectionCard

/**
 * Box -> table -> first turn. Optimised for reading at arm's length: large tap rows, bold
 * quantities, and the screen stays awake while it is open.
 */
@Composable
fun QuickSetupScreen(
    viewModel: QuickSetupViewModel,
    onStartGame: (gameId: Int, gameName: String) -> Unit,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    when (val s = state) {
        QuickSetupUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        QuickSetupUiState.NotFound -> Box(
            Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No setup guide available", style = MaterialTheme.typography.titleMedium)
                Text(
                    "This guide has not been downloaded yet. Connect to the internet and try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onClose) { Text("Back") }
            }
        }
        is QuickSetupUiState.Ready -> QuickSetupContent(
            state = s,
            onSelectPlayers = viewModel::selectPlayerCount,
            onToggleModule = viewModel::toggleModule,
            onToggleStep = viewModel::toggleStep,
            onReset = viewModel::resetChecklist,
            onStartGame = { onStartGame(s.loaded.guide.gameId, s.loaded.guide.gameName) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickSetupContent(
    state: QuickSetupUiState.Ready,
    onSelectPlayers: (Int) -> Unit,
    onToggleModule: (String) -> Unit,
    onToggleStep: (String) -> Unit,
    onReset: () -> Unit,
    onStartGame: () -> Unit
) {
    val guide = state.loaded.guide
    val listState = rememberLazyListState()
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }
    val sections = state.resolved.sections
    // Items before the sections: header, players, content.
    val leadingItems = 3

    // Once everything is ticked, bring the "Start playing" card into view.
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            val startIndex = sections.indexOfFirst { it.kind == GuideSectionKind.START }
            if (startIndex >= 0) listState.animateScrollToItem(leadingItems + startIndex)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)) {
                BoardFlowButton(
                    onClick = onStartGame,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start game")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 14.dp)
        ) {
            item(key = "header") {
                SetupHeader(state = state, onReset = { showResetConfirm = true })
            }
            item(key = "players") {
                SectionCard {
                    SectionLabel("Players")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.playerCounts.forEach { count ->
                            BoardFlowFilterChip(
                                selected = count == state.resolved.playerCount,
                                onClick = { onSelectPlayers(count) },
                                enabled = count in state.selectablePlayerCounts,
                                label = { Text("$count", fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                    if (state.selectablePlayerCounts.last < state.playerCounts.last) {
                        val unlocking = guide.modules
                            .filter { (it.extendsMaxPlayers ?: 0) > state.selectablePlayerCounts.last }
                            .joinToString(" or ") { it.name }
                        HintText("${state.selectablePlayerCounts.last + 1}+ players needs $unlocking")
                    }
                }
            }
            item(key = "content") {
                SectionCard {
                    SectionLabel("Content")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BoardFlowFilterChip(
                            selected = true,
                            onClick = {},
                            enabled = false,
                            label = { Text("Base game") }
                        )
                        guide.modules.forEach { module ->
                            val locked = module.id in state.lockedModules
                            BoardFlowFilterChip(
                                selected = module.id in state.selectedModules,
                                onClick = { onToggleModule(module.id) },
                                enabled = !locked,
                                label = { Text(module.name) }
                            )
                        }
                    }
                    guide.modules
                        .filter { it.id in state.selectedModules && it.note != null }
                        .forEach { HintText("${it.name}: ${it.note}") }
                }
            }
            sections.forEach { section ->
                item(key = "section-${section.id}") {
                    when (section.kind) {
                        GuideSectionKind.SETUP -> ChecklistSection(section, state.checkedStepIds, onToggleStep)
                        GuideSectionKind.REMINDERS -> ReminderSection(section)
                        GuideSectionKind.START -> StartSection(section)
                    }
                }
            }
            item(key = "attribution") {
                Text(
                    text = attribution(state),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    if (showResetConfirm) {
        BoardFlowConfirmationDialog(
            title = "Reset checklist?",
            message = "Clears every ticked step. The guide itself is not changed.",
            confirmLabel = "Reset",
            icon = Icons.Default.RestartAlt,
            onConfirm = { onReset(); showResetConfirm = false },
            onDismiss = { showResetConfirm = false }
        )
    }
}

@Composable
private fun SetupHeader(state: QuickSetupUiState.Ready, onReset: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (state.totalSteps == 0) 0f else state.doneSteps.toFloat() / state.totalSteps,
        label = "setupProgress"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    state.loaded.guide.gameName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (state.isComplete) "Table is ready" else "${state.doneSteps} / ${state.totalSteps} steps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isComplete) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onReset, enabled = state.doneSteps > 0) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reset")
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        if (!state.loaded.guide.provenance.reviewed) {
            HintText("Draft guide - check it against the rulebook.")
        }
    }
}

@Composable
private fun ChecklistSection(
    section: ResolvedSection,
    checked: Set<String>,
    onToggleStep: (String) -> Unit
) {
    val done = section.steps.count { it.id in checked }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SectionLabel(section.title) }
            Text(
                "$done/${section.steps.size}",
                style = MaterialTheme.typography.labelMedium,
                color = if (done == section.steps.size) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column {
            section.steps.forEach { step ->
                ChecklistRow(step = step, checked = step.id in checked, onToggle = { onToggleStep(step.id) })
            }
        }
    }
}

@Composable
private fun ChecklistRow(step: ResolvedStep, checked: Boolean, onToggle: () -> Unit) {
    val contentAlpha by animateFloatAsState(if (checked) 0.45f else 1f, label = "stepAlpha")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp, end = 4.dp)
                .alpha(contentAlpha),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            StepText(step, strikethrough = checked)
            step.note?.let { NoteText(it) }
        }
    }
}

@Composable
private fun ReminderSection(section: ResolvedSection) {
    SectionCard(accented = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            SectionLabel(section.title)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            section.steps.forEach { step ->
                Row {
                    Text("•", modifier = Modifier.width(16.dp), color = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        StepText(step)
                        step.note?.let { NoteText(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartSection(section: ResolvedSection) {
    SectionCard(accented = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            SectionLabel(section.title)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            section.steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f).padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        StepText(step)
                        step.note?.let { NoteText(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepText(step: ResolvedStep, strikethrough: Boolean = false) {
    val amountColor = MaterialTheme.colorScheme.primary
    val text = remember(step, amountColor) {
        buildAnnotatedString {
            step.parts.forEach { part ->
                if (part.isAmount) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = amountColor)) { append(part.text) }
                } else {
                    append(part.text)
                }
            }
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        textDecoration = if (strikethrough) TextDecoration.LineThrough else null
    )
}

@Composable
private fun NoteText(note: String) {
    Text(
        note,
        style = MaterialTheme.typography.bodySmall,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun HintText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun attribution(state: QuickSetupUiState.Ready): String {
    val guide = state.loaded.guide
    val origin = when (guide.provenance.origin) {
        GuideOrigin.BOARDFLOW -> "BoardFlow guide"
        GuideOrigin.COMMUNITY -> "Community guide" + (guide.provenance.author?.let { " by $it" } ?: "")
        GuideOrigin.USER -> "Your guide"
        GuideOrigin.AI_DRAFT -> "AI draft"
    }
    val where = when (state.loaded.source) {
        SetupGuideSource.BUNDLED -> "built in"
        SetupGuideSource.CATALOG -> "downloaded"
        SetupGuideSource.USER -> "customised"
    }
    return "$origin v${guide.version} ($where). Setup reminder only - see the rulebook for full rules."
}
