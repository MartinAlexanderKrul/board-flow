package cz.nicolsburg.boardflow.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.nicolsburg.boardflow.data.setupguide.GuideEdits
import cz.nicolsburg.boardflow.model.GuideModule
import cz.nicolsburg.boardflow.model.GuideSection
import cz.nicolsburg.boardflow.model.GuideSectionKind
import cz.nicolsburg.boardflow.model.GuideStep
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationKind
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormDivider
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormGroup
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineField
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import cz.nicolsburg.boardflow.ui.common.LocalBoardFlowMessenger
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.Spacing

/**
 * Edits a guide as the user's own version: section names, step text and notes, adding,
 * deleting and reordering steps, and adding sections. Player-count amounts and conditions are
 * kept as they are and shown read-only under each step. A form: Cancel and Save at the bottom.
 */
@Composable
fun GuideEditorScreen(
    viewModel: GuideEditorViewModel,
    // Bumped by the top bar's back arrow; handled here so unsaved changes can be confirmed.
    backRequests: Int,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val messenger = LocalBoardFlowMessenger.current
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    val dirty = (state as? GuideEditorUiState.Editing)?.dirty == true

    fun requestClose() {
        if (dirty) showDiscard = true else onClose()
    }
    BackHandler { requestClose() }
    LaunchedEffect(backRequests) { if (backRequests > 0) requestClose() }

    when (val s = state) {
        GuideEditorUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        GuideEditorUiState.NotFound -> Box(Modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text("This guide could not be opened", style = MaterialTheme.typography.titleMedium)
                BoardFlowSecondaryButton(onClick = onClose) { Text("Back") }
            }
        }
        is GuideEditorUiState.Editing -> EditorContent(
            state = s,
            saving = saving,
            viewModel = viewModel,
            onCancel = ::requestClose,
            onSave = {
                viewModel.save { problem ->
                    if (problem == null) {
                        messenger.show("Saved as your version of the guide")
                        onClose()
                    } else {
                        messenger.show("Not saved: $problem")
                    }
                }
            }
        )
    }

    if (showDiscard) {
        BoardFlowConfirmationDialog(
            title = "Discard changes?",
            message = "Your edits to this guide are not saved.",
            confirmLabel = "Discard",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = { showDiscard = false; onClose() },
            onDismiss = { showDiscard = false }
        )
    }
}

@Composable
private fun EditorContent(
    state: GuideEditorUiState.Editing,
    saving: Boolean,
    viewModel: GuideEditorViewModel,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val guide = state.guide
    var deleteSectionId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoardFlowInlineAction(onClick = onCancel, neutral = true, large = true) { Text("Cancel") }
                    Spacer(Modifier.width(Spacing.sm))
                    BoardFlowButton(onClick = onSave, enabled = !saving) { Text("Save") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = PaddingValues(vertical = Spacing.lg)
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(guide.gameName, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Saved as your version of this guide; the standard guide stays as it was. " +
                            "Grey lines show quantities and conditions that are kept as they are.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            guide.sections.forEach { section ->
                item(key = "section-${section.id}") {
                    SectionEditor(
                        section = section,
                        modules = guide.modules,
                        onTitle = { viewModel.setSectionTitle(section.id, it) },
                        onText = { stepId, text -> viewModel.setStepText(section.id, stepId, text) },
                        onNote = { stepId, note -> viewModel.setStepNote(section.id, stepId, note) },
                        onMove = { stepId, delta -> viewModel.moveStep(section.id, stepId, delta) },
                        onDelete = { stepId -> viewModel.deleteStep(section.id, stepId) },
                        onAddStep = { viewModel.addStep(section.id) },
                        onDeleteSection = { deleteSectionId = section.id }
                    )
                }
            }
            item(key = "add-section") {
                BoardFlowInlineAction(onClick = viewModel::addSection, icon = Icons.Default.Add) { Text("Add section") }
            }
        }
    }

    deleteSectionId?.let { id ->
        val title = guide.sections.firstOrNull { it.id == id }?.title.orEmpty()
        BoardFlowConfirmationDialog(
            title = "Delete \"$title\"?",
            message = "Removes the section and all of its steps from your version.",
            confirmLabel = "Delete",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = { viewModel.deleteSection(id); deleteSectionId = null },
            onDismiss = { deleteSectionId = null }
        )
    }
}

@Composable
private fun SectionEditor(
    section: GuideSection,
    modules: List<GuideModule>,
    onTitle: (String) -> Unit,
    onText: (stepId: String, text: String) -> Unit,
    onNote: (stepId: String, note: String) -> Unit,
    onMove: (stepId: String, delta: Int) -> Unit,
    onDelete: (stepId: String) -> Unit,
    onAddStep: () -> Unit,
    onDeleteSection: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (section.kind) {
                    GuideSectionKind.SETUP -> "Checklist"
                    GuideSectionKind.REMINDERS -> "Reminders"
                    GuideSectionKind.START -> "First turn"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDeleteSection) {
                Icon(Icons.Default.Delete, contentDescription = "Delete section", tint = MaterialTheme.colorScheme.error)
            }
        }
        BoardFlowFormGroup {
            Box(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                BoardFlowInlineField(
                    value = section.title,
                    onValueChange = onTitle,
                    placeholder = "Section name",
                    textStyle = MaterialTheme.typography.titleMedium
                )
            }
            section.steps.forEachIndexed { index, step ->
                BoardFlowFormDivider()
                StepEditor(
                    step = step,
                    rules = GuideEdits.describeRules(step, section.condition, modules),
                    canMoveUp = index > 0,
                    canMoveDown = index < section.steps.lastIndex,
                    onText = { onText(step.id, it) },
                    onNote = { onNote(step.id, it) },
                    onMove = { onMove(step.id, it) },
                    onDelete = { onDelete(step.id) }
                )
            }
            BoardFlowFormDivider()
            Box(Modifier.padding(horizontal = Spacing.sm)) {
                BoardFlowInlineAction(onClick = onAddStep, icon = Icons.Default.Add) { Text("Add step") }
            }
        }
    }
}

@Composable
private fun StepEditor(
    step: GuideStep,
    rules: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onText: (String) -> Unit,
    onNote: (String) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Box(Modifier.padding(end = Spacing.sm)) {
            BoardFlowInlineField(
                value = step.text,
                onValueChange = onText,
                placeholder = "What to do",
                singleLine = false,
                maxLines = 6
            )
        }
        Box(Modifier.padding(end = Spacing.sm)) {
            BoardFlowInlineField(
                value = step.note.orEmpty(),
                onValueChange = onNote,
                placeholder = "Add a note (optional)",
                singleLine = false,
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodySmall,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Read-only rules on the left, the step's actions on the right, on one line.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                rules.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(Dimens.Icon))
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(Dimens.Icon))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete step",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(Dimens.Icon)
                )
            }
        }
    }
}
