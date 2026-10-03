package cz.nicolsburg.boardflow.ui.setup

import cz.nicolsburg.boardflow.ui.theme.Spacing
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import cz.nicolsburg.boardflow.ui.theme.BoardFlowColors
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.GameCover
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextAlign
import cz.nicolsburg.boardflow.ui.common.LocalBoardFlowMessenger
import android.content.Intent
import java.io.File
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideResolver
import cz.nicolsburg.boardflow.model.GuideModule
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
    onClose: () -> Unit,
    onEditGuide: (gameId: Int) -> Unit = {},
    thumbnailFor: (gameId: Int) -> String? = { null },
    gameNameFor: (gameId: Int) -> String? = { null }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drafting by viewModel.drafting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val messenger = LocalBoardFlowMessenger.current

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    when (val s = state) {
        QuickSetupUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        QuickSetupUiState.NotFound -> NoGuide(
            hasStandardGuide = viewModel.hasStandardGuide,
            canDraft = viewModel.canDraft,
            drafting = drafting,
            gameName = gameNameFor(viewModel.gameId),
            onDraft = { pdf, name ->
                viewModel.draftFromRulebook(pdf, name) { problem ->
                    messenger.show(problem ?: "Draft guide ready - check it against the rulebook")
                }
            },
            onClose = onClose
        )
        is QuickSetupUiState.Ready -> QuickSetupContent(
            state = s,
            onSelectPlayers = viewModel::selectPlayerCount,
            onToggleModule = viewModel::toggleModule,
            onToggleStep = viewModel::toggleStep,
            onReset = viewModel::resetChecklist,
            onStartGame = { onStartGame(s.loaded.guide.gameId, s.loaded.guide.gameName) },
            onShareGuide = { viewModel.exportJson()?.let { shareGuide(context, s.loaded.guide.gameName, it) } },
            onEditGuide = { onEditGuide(s.loaded.guide.gameId) },
            onUseStandardGuide = viewModel::useStandardGuide,
            onKeepUserGuide = viewModel::keepUserGuide,
            onMarkReviewed = {
                viewModel.markReviewed()
                messenger.show("Guide marked as reviewed")
            },
            thumbnailUrl = thumbnailFor(s.loaded.guide.gameId)
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
    onStartGame: () -> Unit,
    onShareGuide: () -> Unit,
    onEditGuide: () -> Unit,
    onUseStandardGuide: () -> Unit,
    onKeepUserGuide: () -> Unit,
    onMarkReviewed: () -> Unit,
    thumbnailUrl: String? = null
) {
    val guide = state.loaded.guide
    var showUseStandardConfirm by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }
    val sections = state.resolved.sections
    // Single-choice groups (e.g. "Mode") get their own picker; everything else is "Content".
    // A group with no member available at this player count (e.g. solo difficulty at 3 players) is hidden.
    val moduleGroups = remember(guide, state.resolved.playerCount) {
        guide.modules.filter { it.group != null }.groupBy { it.group!! }.toList()
            .filter { (_, members) -> members.any { SetupGuideResolver.isModuleAvailable(it, state.resolved.playerCount) } }
    }
    val ungroupedModules = remember(guide) { guide.modules.filter { it.group == null } }

    // Once everything is ticked the checklist and the pickers fold away, leaving the reminders
    // and "Start playing". "Show steps" brings them back without unticking anything.
    var showSteps by rememberSaveable { mutableStateOf(false) }
    val collapsed = state.isComplete && !showSteps
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) listState.animateScrollToItem(0) else showSteps = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (state.isComplete) "Table is ready" else "${state.doneSteps} of ${state.totalSteps} steps done",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Starts the play timer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BoardFlowButton(onClick = onStartGame) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(Dimens.Icon))
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Start game")
                    }
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
                SetupHeader(state = state, thumbnailUrl = thumbnailUrl, onReset = { showResetConfirm = true }, onMarkReviewed = onMarkReviewed)
            }
            if (state.loaded.upstreamUpdated) {
                item(key = "upstream-update") {
                    SectionCard {
                        SectionLabel("Updated guide available")
                        HintText(
                            "You are using your own version of this guide. The standard guide is now " +
                                "v${state.loaded.upstreamVersion}."
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BoardFlowInlineAction(onClick = onKeepUserGuide, neutral = true) { Text("Keep mine") }
                            Spacer(Modifier.width(Spacing.sm))
                            BoardFlowInlineAction(onClick = { showUseStandardConfirm = true }) { Text("Use updated guide") }
                        }
                    }
                }
            }
            if (state.isComplete) {
                item(key = "done") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "All ${state.totalSteps} setup steps done",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        BoardFlowInlineAction(onClick = { showSteps = !showSteps }) {
                            Text(if (showSteps) "Hide steps" else "Show steps")
                        }
                    }
                }
            }
            if (!collapsed) item(key = "players") {
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
                    if (state.selectablePlayerCounts.last() < state.playerCounts.last()) {
                        val unlocking = guide.modules
                            .filter { (it.extendsMaxPlayers ?: 0) > state.selectablePlayerCounts.last() }
                            .joinToString(" or ") { it.name }
                        HintText("${state.selectablePlayerCounts.last() + 1}+ players needs $unlocking")
                    }
                }
            }
            if (!collapsed) moduleGroups.forEach { (group, members) ->
                item(key = "group-$group") {
                    SectionCard {
                        SectionLabel(group)
                        ModuleChips(members, state, onToggleModule)
                    }
                }
            }
            if (!collapsed && ungroupedModules.isNotEmpty()) {
                item(key = "content") {
                    SectionCard {
                        SectionLabel("Content")
                        ModuleChips(ungroupedModules, state, onToggleModule, showBaseGame = true)
                    }
                }
            }
            sections.filter { !collapsed || it.kind != GuideSectionKind.SETUP }.forEach { section ->
                item(key = "section-${section.id}") {
                    when (section.kind) {
                        GuideSectionKind.SETUP -> ChecklistSection(section, state.checkedStepIds, onToggleStep)
                        GuideSectionKind.REMINDERS -> ReminderSection(section)
                        GuideSectionKind.START -> StartSection(section)
                    }
                }
            }
            item(key = "attribution") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = attribution(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    FlowRow(verticalArrangement = Arrangement.Center) {
                        BoardFlowInlineAction(onClick = onEditGuide) {
                            Text("Edit guide")
                        }
                        BoardFlowInlineAction(onClick = onShareGuide) {
                            Text("Share guide")
                        }
                        if (state.loaded.source == SetupGuideSource.USER && state.loaded.upstreamVersion != null &&
                            !state.loaded.upstreamUpdated
                        ) {
                            BoardFlowInlineAction(onClick = { showUseStandardConfirm = true }) {
                                Text("Use standard guide")
                            }
                        }
                        if (state.loaded.source == SetupGuideSource.USER && state.loaded.upstreamVersion == null) {
                            BoardFlowInlineAction(onClick = { showUseStandardConfirm = true }) {
                                Text("Delete guide")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUseStandardConfirm) {
        val hasStandard = state.loaded.upstreamVersion != null
        BoardFlowConfirmationDialog(
            title = if (hasStandard) "Use the standard guide?" else "Delete this guide?",
            message = if (hasStandard) {
                "Your version of this guide is removed from this phone. Share it first if you want to keep a copy."
            } else {
                "This game has no standard guide, so it will have no guide at all. Share it first if you want to keep a copy."
            },
            confirmLabel = if (hasStandard) "Use standard guide" else "Delete guide",
            kind = cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = { onUseStandardGuide(); showUseStandardConfirm = false },
            onDismiss = { showUseStandardConfirm = false }
        )
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
private fun SetupHeader(state: QuickSetupUiState.Ready, thumbnailUrl: String?, onReset: () -> Unit, onMarkReviewed: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (state.totalSteps == 0) 0f else state.doneSteps.toFloat() / state.totalSteps,
        label = "setupProgress"
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            GameCover(name = state.loaded.guide.gameName, thumbnailUrl = thumbnailUrl, size = 72.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    "Quick setup",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    state.loaded.guide.gameName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (state.isComplete) "Table is ready" else "${state.doneSteps} / ${state.totalSteps} steps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isComplete) BoardFlowColors.Success
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.doneSteps > 0) {
                BoardFlowInlineAction(onClick = onReset, icon = Icons.Default.RestartAlt) {
                    Text("Reset")
                }
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(BoardFlowShape.Pill),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        if (!state.loaded.guide.provenance.reviewed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { HintText("Draft guide - check it against the rulebook.") }
                BoardFlowInlineAction(onClick = onMarkReviewed) { Text("Mark as reviewed") }
            }
        }
    }
}

/** No guide to show: a standard one is not downloaded yet, or the game has none and can get an AI draft. */
@Composable
private fun NoGuide(
    hasStandardGuide: Boolean,
    canDraft: Boolean,
    drafting: Boolean,
    gameName: String?,
    onDraft: (Uri, String) -> Unit,
    onClose: () -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && gameName != null) onDraft(uri, gameName)
    }
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                hasStandardGuide -> {
                    Text("No setup guide available", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "This guide has not been downloaded yet. Connect to the internet and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BoardFlowSecondaryButton(onClick = onClose) { Text("Back") }
                }
                drafting -> {
                    CircularProgressIndicator()
                    Text("Reading the rulebook", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "This can take a minute or two. Keep this screen open.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    Text(
                        gameName?.let { "No setup guide for $it yet" } ?: "No setup guide yet",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        if (canDraft) {
                            "Pick the rulebook PDF and Gemini drafts a guide from it. Check the draft against the rulebook before you rely on it."
                        } else {
                            "Add a Gemini key in Settings > Scan to draft a guide from the rulebook PDF."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BoardFlowInlineAction(onClick = onClose, neutral = true, large = true) { Text("Back") }
                        if (canDraft && gameName != null) {
                            BoardFlowButton(onClick = { picker.launch(arrayOf("application/pdf")) }) {
                                Text("Draft from rulebook")
                            }
                        }
                    }
                }
            }
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
                style = MaterialTheme.typography.labelLarge,
                color = if (done == section.steps.size) BoardFlowColors.Success
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
    SectionCard {
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
    SectionCard {
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
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun HintText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Shares the guide as a .json file, the same format the catalog and "Import a guide" use. */
private fun shareGuide(context: Context, gameName: String, json: String) {
    val safeName = gameName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "guide" }
    val file = File(context.cacheDir, "$safeName-setup-guide.json")
    file.writeText(json)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "$gameName setup guide")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share setup guide"))
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
        SetupGuideSource.USER -> "your version"
    }
    return "$origin v${guide.version} ($where). Setup reminder only - see the rulebook for full rules."
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleChips(
    modules: List<GuideModule>,
    state: QuickSetupUiState.Ready,
    onToggleModule: (String) -> Unit,
    showBaseGame: Boolean = false
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showBaseGame) {
            // Always included; shown selected (not greyed out) so it reads as part of the setup.
            BoardFlowFilterChip(selected = true, onClick = {}, label = { Text("Base game") })
        }
        modules.forEach { module ->
            BoardFlowFilterChip(
                selected = module.id in state.selectedModules,
                onClick = { onToggleModule(module.id) },
                enabled = module.id !in state.lockedModules,
                label = { Text(module.name) }
            )
        }
    }
    modules
        .filter { it.id in state.selectedModules && it.note != null }
        .forEach { HintText("${it.name}: ${it.note}") }
}
