package cz.nicolsburg.boardflow.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.data.setupguide.GuideEdits
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideResolver
import cz.nicolsburg.boardflow.model.SetupGuide
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowFilterChip
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormDivider
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormGroup
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormRow
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineField
import cz.nicolsburg.boardflow.ui.common.BoardFlowModalBottomSheet
import cz.nicolsburg.boardflow.ui.common.BoardFlowSectionTitle
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.Spacing

/**
 * Edits a step's per-player quantities and when it shows (player counts, with or without each
 * module), or with [stepId] null when a whole section shows. Every change goes straight into the
 * editor's draft, so the editor's Cancel still discards it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun GuideRulesSheet(
    guide: SetupGuide,
    sectionId: String,
    stepId: String?,
    viewModel: GuideEditorViewModel,
    onDismiss: () -> Unit
) {
    val section = guide.sections.firstOrNull { it.id == sectionId } ?: return onDismiss()
    val step = stepId?.let { id -> section.steps.firstOrNull { it.id == id } ?: return onDismiss() }
    val condition = if (step != null) step.condition else section.condition
    val allCounts = SetupGuideResolver.allPlayerCounts(guide)
    val shownAt = GuideEdits.shownAtCounts(condition, allCounts)

    BoardFlowModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    if (step == null) "When this section shows" else "Quantities and when it shows",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    step?.text?.ifBlank { null } ?: section.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (step != null) {
                val names = GuideEdits.quantityNames(step)
                if (names.isEmpty()) {
                    Text(
                        "To add a quantity, put a name in braces in the step text, e.g. \"Place {n} cards\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                names.forEach { name ->
                    val rule = step.amounts[name]
                    val unused = name !in SetupGuideResolver.placeholders(step.text)
                    BoardFlowSectionTitle(
                        title = "Quantity {$name}",
                        supporting = if (unused) "Not used in the step text" else "Counts left empty use the Any count value"
                    )
                    BoardFlowFormGroup {
                        QuantityRow("Any count", rule?.default.orEmpty()) {
                            viewModel.setAmountValue(sectionId, step.id, name, null, it)
                        }
                        allCounts.forEach { n ->
                            BoardFlowFormDivider()
                            QuantityRow(if (n == 1) "1 player" else "$n players", rule?.byPlayers?.get(n).orEmpty()) {
                                viewModel.setAmountValue(sectionId, step.id, name, n, it)
                            }
                        }
                        rule?.cases?.forEachIndexed { index, case ->
                            BoardFlowFormDivider()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BoardFlowFormRow(
                                    label = GuideEdits.describeCase(case, guide.modules),
                                    labelWidth = 160.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    BoardFlowInlineField(
                                        value = case.value,
                                        onValueChange = { viewModel.setCaseValue(sectionId, step.id, name, index, it) },
                                        placeholder = "Value"
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteCase(sectionId, step.id, name, index) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete special case",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(Dimens.Icon)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (allCounts.size > 1) {
                BoardFlowSectionTitle(title = "Players", supporting = "Shows only at the selected player counts")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    allCounts.forEach { n ->
                        val selected = n in shownAt
                        BoardFlowFilterChip(
                            selected = selected,
                            // At least one count stays selected: a step that never shows is a deleted step.
                            onClick = {
                                val next = if (selected) shownAt - n else shownAt + n
                                viewModel.setShownAtCounts(sectionId, stepId, next, allCounts)
                            },
                            enabled = !(selected && shownAt.size == 1),
                            label = { Text("$n") }
                        )
                    }
                }
            }

            if (guide.modules.isNotEmpty()) {
                BoardFlowSectionTitle(title = "Content", supporting = "Show only with or without a module")
                guide.modules.forEach { module ->
                    val current = GuideEdits.moduleRule(condition, module.id)
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(module.name, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            listOf(
                                GuideEdits.ModuleRule.ANY to "Either way",
                                GuideEdits.ModuleRule.WITH to "With",
                                GuideEdits.ModuleRule.WITHOUT to "Without"
                            ).forEach { (rule, label) ->
                                BoardFlowFilterChip(
                                    selected = current == rule,
                                    onClick = { viewModel.setModuleRule(sectionId, stepId, module.id, rule) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                BoardFlowButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

@Composable
private fun QuantityRow(label: String, value: String, onValueChange: (String) -> Unit) {
    BoardFlowFormRow(label = label, labelWidth = 120.dp) {
        BoardFlowInlineField(
            value = value,
            onValueChange = onValueChange,
            placeholder = "-",
            keyboardType = KeyboardType.Text
        )
    }
}
