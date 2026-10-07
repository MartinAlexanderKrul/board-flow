package cz.nicolsburg.boardflow.ui.challenges

import androidx.compose.runtime.collectAsState
import cz.nicolsburg.boardflow.ui.common.SearchBggRow
import cz.nicolsburg.boardflow.ui.common.BggSearchSheet
import cz.nicolsburg.boardflow.model.bggOnlyGameItem
import cz.nicolsburg.boardflow.data.BggGameSearch
import androidx.compose.material3.ButtonDefaults
import cz.nicolsburg.boardflow.ui.common.boardFlowDatePickerColors
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import androidx.compose.foundation.layout.offset
import cz.nicolsburg.boardflow.ui.common.GameCover
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.CircularProgressIndicator
import cz.nicolsburg.boardflow.ui.common.formatDisplayDate
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineField
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormRow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import cz.nicolsburg.boardflow.ui.theme.Spacing
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import cz.nicolsburg.boardflow.ui.theme.BoardFlowColors
import cz.nicolsburg.boardflow.ui.common.withTabularNumbers
import cz.nicolsburg.boardflow.ui.common.formatChipDate
import cz.nicolsburg.boardflow.ui.common.BoardFlowSectionTitle
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormGroup
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormDivider
import cz.nicolsburg.boardflow.ui.common.BoardFlowEmptyState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.model.Challenge
import cz.nicolsburg.boardflow.model.ChallengeProgress
import cz.nicolsburg.boardflow.model.ChallengeStatus
import cz.nicolsburg.boardflow.model.ChallengeType
import cz.nicolsburg.boardflow.model.GameItem
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.ui.common.AnimatedDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationKind
import cz.nicolsburg.boardflow.ui.common.BoardFlowFilterChip
import cz.nicolsburg.boardflow.ui.common.BoardFlowIconButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowModalBottomSheet
import cz.nicolsburg.boardflow.ui.common.BoardFlowSurfaceTokens
import cz.nicolsburg.boardflow.ui.common.SectionCard
import cz.nicolsburg.boardflow.ui.common.SectionHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChallengesTabContent(
    progressList: List<ChallengeProgress>,
    onEdit: (Challenge) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val activeProgress = remember(progressList) { progressList.filter { it.isActive } }
    val pausedProgress = remember(progressList) { progressList.filter { it.isPaused && !it.isArchived && !it.isComplete && !it.isFailed } }
    val failedProgress = remember(progressList) { progressList.filter { it.isFailed } }
    val completedProgress = remember(progressList) { progressList.filter { it.isComplete && !it.isArchived } }
    val archivedProgress = remember(progressList) { progressList.filter { it.isArchived } }
    val overallFraction = remember(progressList) {
        val relevant = progressList.filter { (it.isActive || it.isComplete) && !it.isArchived }
        if (relevant.isEmpty()) 0f else relevant.map { it.fraction }.average().toFloat()
    }
    var completedExpanded by rememberSaveable { mutableStateOf(false) }
    var failedExpanded by rememberSaveable { mutableStateOf(false) }
    var pausedExpanded by rememberSaveable { mutableStateOf(true) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }

    if (progressList.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BoardFlowEmptyState(
                icon = Icons.Default.EmojiEvents,
                title = "No challenges yet",
                message = "Set a goal for this month, your group or a favourite game, and track it as you log plays."
            )
        }
    } else {
        LazyColumn(
            state = listState,
            // Bottom space keeps the last card clear of the New challenge button.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize()
        ) {
            item {
                ChallengesHeroCard(
                    activeCount = activeProgress.size,
                    completeCount = completedProgress.size,
                    overallFraction = overallFraction
                )
            }

            if (activeProgress.isNotEmpty()) {
                item {
                    BoardFlowSectionTitle(title = "Active")
                }
                items(activeProgress, key = { it.challenge.id }) { progress ->
                    ChallengeCard(
                        progress = progress,
                        onEdit = { onEdit(progress.challenge) },
                        onPause = { onPause(progress.challenge.id) },
                        onResume = { onResume(progress.challenge.id) },
                        onArchive = { onArchive(progress.challenge.id) },
                        onRestore = { onRestore(progress.challenge.id) },
                        onDelete = { onDelete(progress.challenge.id) }
                    )
                }
            } else {
                item {
                    Text(
                        if (pausedProgress.isNotEmpty()) "No active challenges. Resume a paused one below, or start a new one."
                        else "No active challenges. Everything is wrapped up.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (pausedProgress.isNotEmpty()) {
                item {
                    CollapsibleChallengesHeader(
                        label = "Paused",
                        count = pausedProgress.size,
                        expanded = pausedExpanded,
                        onToggle = { pausedExpanded = !pausedExpanded },
                        countColor = BoardFlowColors.Warning
                    )
                }
                if (pausedExpanded) {
                    items(pausedProgress, key = { it.challenge.id }) { progress ->
                        ChallengeCard(
                            progress = progress,
                            onEdit = { onEdit(progress.challenge) },
                            onPause = { onPause(progress.challenge.id) },
                            onResume = { onResume(progress.challenge.id) },
                            onArchive = { onArchive(progress.challenge.id) },
                            onRestore = { onRestore(progress.challenge.id) },
                            onDelete = { onDelete(progress.challenge.id) }
                        )
                    }
                }
            }

            if (failedProgress.isNotEmpty()) {
                item {
                    CollapsibleChallengesHeader(
                        label = "Missed",
                        count = failedProgress.size,
                        expanded = failedExpanded,
                        onToggle = { failedExpanded = !failedExpanded },
                        countColor = MaterialTheme.colorScheme.error
                    )
                }
                if (failedExpanded) {
                    items(failedProgress, key = { it.challenge.id }) { progress ->
                        ChallengeCard(
                            progress = progress,
                            onEdit = { onEdit(progress.challenge) },
                            onPause = { onPause(progress.challenge.id) },
                            onResume = { onResume(progress.challenge.id) },
                            onArchive = { onArchive(progress.challenge.id) },
                            onRestore = { onRestore(progress.challenge.id) },
                            onDelete = { onDelete(progress.challenge.id) }
                        )
                    }
                }
            }

            if (completedProgress.isNotEmpty()) {
                item {
                    CollapsibleChallengesHeader(
                        label = "Completed",
                        count = completedProgress.size,
                        expanded = completedExpanded,
                        onToggle = { completedExpanded = !completedExpanded },
                        countColor = BoardFlowColors.Success
                    )
                }
                if (completedExpanded) {
                    items(completedProgress, key = { it.challenge.id }) { progress ->
                        ChallengeCard(
                            progress = progress,
                            onEdit = { onEdit(progress.challenge) },
                            onPause = { onPause(progress.challenge.id) },
                            onResume = { onResume(progress.challenge.id) },
                            onArchive = { onArchive(progress.challenge.id) },
                            onRestore = { onRestore(progress.challenge.id) },
                            onDelete = { onDelete(progress.challenge.id) }
                        )
                    }
                }
            }

            if (archivedProgress.isNotEmpty()) {
                item {
                    CollapsibleChallengesHeader(
                        label = "Archived",
                        count = archivedProgress.size,
                        expanded = archivedExpanded,
                        onToggle = { archivedExpanded = !archivedExpanded },
                        countColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (archivedExpanded) {
                    items(archivedProgress, key = { it.challenge.id }) { progress ->
                        ChallengeCard(
                            progress = progress,
                            onEdit = { onEdit(progress.challenge) },
                            onPause = { onPause(progress.challenge.id) },
                            onResume = { onResume(progress.challenge.id) },
                            onArchive = { onArchive(progress.challenge.id) },
                            onRestore = { onRestore(progress.challenge.id) },
                            onDelete = { onDelete(progress.challenge.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChallengeCard(
    progress: ChallengeProgress,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val challenge = progress.challenge
    var showActionsSheet by remember { mutableStateOf(false) }
    var detailsExpanded by rememberSaveable(challenge.id) { mutableStateOf(false) }

    if (showActionsSheet) {
        BoardFlowModalBottomSheet(
            onDismissRequest = { showActionsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ChallengeActionSheetContent(
                progress = progress,
                onEdit = {
                    showActionsSheet = false
                    onEdit()
                },
                onPause = {
                    showActionsSheet = false
                    onPause()
                },
                onResume = {
                    showActionsSheet = false
                    onResume()
                },
                onArchive = {
                    showActionsSheet = false
                    onArchive()
                },
                onRestore = {
                    showActionsSheet = false
                    onRestore()
                },
                onDelete = {
                    showActionsSheet = false
                    onDelete()
                }
            )
        }
    }

    // Status colour: amber while it is running, then green, red, orange or grey.
    val accentColor = when {
        progress.isArchived -> MaterialTheme.colorScheme.onSurfaceVariant
        progress.isPaused   -> BoardFlowColors.Warning
        progress.isComplete -> BoardFlowColors.Success
        progress.isFailed   -> MaterialTheme.colorScheme.error
        else                -> MaterialTheme.colorScheme.primary
    }
    val statusLabel = when {
        progress.isArchived -> "Archived"
        progress.isPaused   -> "Paused"
        progress.isComplete -> "Complete"
        progress.isFailed   -> "Missed"
        else                -> null
    }
    val progressPercent = (progress.fraction * 100).toInt().coerceIn(0, 100)
    val typeMeta = challengeTypeMeta(challenge.type)
    val hasGames = progress.countedGameNames.isNotEmpty()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BoardFlowShape.Card)
            .combinedClickable(
                onClick = { if (hasGames) detailsExpanded = !detailsExpanded else showActionsSheet = true },
                onLongClick = { showActionsSheet = true }
            ),
        shape = BoardFlowShape.Card,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(start = Spacing.md, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.md)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = BoardFlowShape.Pill, color = accentColor.copy(alpha = 0.16f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(typeMeta.icon, contentDescription = null, modifier = Modifier.size(Dimens.Icon), tint = accentColor)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // The goal, unless the title already says it.
                    val description = challengeDescription(challenge)
                    if (!description.equals(challenge.title.trim(), ignoreCase = true)) {
                        Text(
                            description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // The visible way to edit, pause, archive or delete.
                BoardFlowIconButton(onClick = { showActionsSheet = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Challenge options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(
                modifier = Modifier.padding(end = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                ChallengeMetaRow(
                    challenge = challenge,
                    typeLabel = typeMeta.label,
                    statusLabel = statusLabel,
                    statusColor = accentColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${progress.currentCount}",
                            style = MaterialTheme.typography.headlineMedium.withTabularNumbers(),
                            color = accentColor
                        )
                        Text(
                            " / ${progress.goalCount}",
                            style = MaterialTheme.typography.titleMedium.withTabularNumbers(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Text(
                        when {
                            progress.isComplete -> "Done"
                            progress.isFailed   -> "Goal not reached"
                            else                -> progress.remainingText ?: "$progressPercent%"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(BoardFlowShape.Pill),
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )

                if (hasGames) {
                    ChallengeCountedGamesRow(gameNames = progress.countedGameNames, expanded = detailsExpanded)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChallengeCountedGamesRow(gameNames: List<String>, expanded: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                if (expanded) "Hide games" else "Show ${gameNames.size} counted ${if (gameNames.size == 1) "game" else "games"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.Icon)
            )
        }
        if (expanded) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                gameNames.forEach { gameName ->
                    Surface(shape = BoardFlowShape.Pill, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Text(
                            text = gameName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeActionSheetContent(
    progress: ChallengeProgress,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = progress.challenge.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${progress.currentCount} of ${progress.goalCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BoardFlowFormGroup {
            ChallengeSheetActionRow(Icons.Default.Edit, "Edit", onEdit)
            when {
                progress.isArchived -> {
                    BoardFlowFormDivider()
                    ChallengeSheetActionRow(Icons.Default.Unarchive, "Restore", onRestore)
                }
                progress.isPaused -> {
                    BoardFlowFormDivider()
                    ChallengeSheetActionRow(Icons.Default.PlayArrow, "Resume", onResume)
                    BoardFlowFormDivider()
                    ChallengeSheetActionRow(Icons.Default.Archive, "Archive", onArchive)
                }
                progress.isActive -> {
                    BoardFlowFormDivider()
                    ChallengeSheetActionRow(Icons.Default.Pause, "Pause", onPause)
                    BoardFlowFormDivider()
                    ChallengeSheetActionRow(Icons.Default.Archive, "Archive", onArchive)
                }
            }
            BoardFlowFormDivider()
            ChallengeSheetActionRow(Icons.Default.Delete, "Delete", onDelete, destructive = true)
        }
    }
}

@Composable
private fun ChallengeSheetActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(Dimens.Icon))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
private fun CollapsibleChallengesHeader(
    label: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    countColor: Color = MaterialTheme.colorScheme.primary
) {
    BoardFlowSectionTitle(
        title = label,
        modifier = Modifier.clip(BoardFlowShape.Control).clickable(onClick = onToggle)
    ) {
        Surface(shape = BoardFlowShape.Pill, color = countColor.copy(alpha = 0.16f)) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = countColor,
                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
            )
        }
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The board header: what is running, what is done, and overall progress as a ring. */
@Composable
private fun ChallengesHeroCard(
    activeCount: Int,
    completeCount: Int,
    overallFraction: Float
) {
    val amber = MaterialTheme.colorScheme.primary
    val percent = (overallFraction * 100).toInt().coerceIn(0, 100)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = BoardFlowShape.Card,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = amber, modifier = Modifier.size(Dimens.IconLarge))
                    Text(
                        "Challenge Board",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    "Keep ongoing goals visible and give completed runs a proper finish line.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                    ChallengeSummaryStat("Active", activeCount.toString())
                    ChallengeSummaryStat("Completed", completeCount.toString())
                }
            }
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { overallFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.size(84.dp),
                    color = amber,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$percent%",
                        style = MaterialTheme.typography.titleMedium.withTabularNumbers(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "overall",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeSummaryStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.withTabularNumbers(),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class ChallengeTypeMeta(
    val label: String,
    val description: String,
    val icon: ImageVector
)

private fun challengeTypeMeta(type: ChallengeType): ChallengeTypeMeta = when (type) {
    ChallengeType.PLAY_N_TIMES ->
        ChallengeTypeMeta("Play volume", "Set a target for total logged plays.", Icons.Default.EmojiEvents)
    ChallengeType.PLAY_SPECIFIC_GAME ->
        ChallengeTypeMeta("Single game", "Keep one title coming back to the table.", Icons.Default.SportsEsports)
    ChallengeType.PLAY_N_DISTINCT ->
        ChallengeTypeMeta("Variety run", "Push yourself to rotate through more games.", Icons.Default.Flag)
    ChallengeType.PLAYER_WIN_STREAK ->
        ChallengeTypeMeta("Hot streak", "Track a player chasing consecutive wins.", Icons.Default.LocalFireDepartment)
    ChallengeType.PLAY_WITH_GROUP_N_TIMES ->
        ChallengeTypeMeta("Table group", "Follow how often your regular crew gets together.", Icons.Default.Groups)
    ChallengeType.PLAY_STREAK ->
        ChallengeTypeMeta("Play streak", "Stay consistent by logging plays every day, week, or month.", Icons.Default.Repeat)
    ChallengeType.PLAY_N_UNPLAYED ->
        ChallengeTypeMeta("First plays", "Get owned games off the shelf and onto the table.", Icons.Default.Flag)
}

@Composable
private fun ChallengeBadge(icon: ImageVector?, label: String, tint: Color? = null) {
    Surface(
        shape = BoardFlowShape.Pill,
        color = tint?.copy(alpha = 0.16f) ?: MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = tint ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChallengeMetaRow(
    challenge: Challenge,
    typeLabel: String,
    statusLabel: String?,
    statusColor: Color
) {
    val start = challenge.startDate?.let { formatChipDate(it) }
    val end = challenge.endDate?.let { formatChipDate(it) }
    val dateLabel = when {
        start != null && end != null -> "$start - $end"
        start != null -> "From $start"
        end != null -> "Until $end"
        else -> null
    }
    val focusLabel = when (challenge.type) {
        ChallengeType.PLAY_SPECIFIC_GAME -> challenge.gameName
        ChallengeType.PLAYER_WIN_STREAK -> challenge.playerNames.firstOrNull()
        ChallengeType.PLAY_WITH_GROUP_N_TIMES -> challenge.playerNames.takeIf { it.isNotEmpty() }?.let { names ->
            when {
                names.size <= 3 -> names.joinToString(", ")
                else -> names.take(3).joinToString(", ") + " +${names.size - 3}"
            }
        }
        ChallengeType.PLAY_STREAK -> when (challenge.streakPeriod ?: "WEEKLY") {
            "DAILY" -> "Daily"
            "MONTHLY" -> "Monthly"
            else -> "Weekly"
        }
        else -> null
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ChallengeBadge(icon = null, label = statusLabel ?: typeLabel, tint = statusColor)
        focusLabel?.let { ChallengeBadge(icon = challengeTypeMeta(challenge.type).icon, label = it) }
        dateLabel?.let { ChallengeBadge(icon = Icons.Default.CalendarMonth, label = it) }
    }
}

fun challengeDescription(challenge: Challenge): String = when (challenge.type) {
    ChallengeType.PLAY_N_TIMES -> "Play ${challenge.targetCount} times"
    ChallengeType.PLAY_SPECIFIC_GAME ->
        "Play ${challenge.gameName ?: "a game"} ${challenge.targetCount} times"
    ChallengeType.PLAY_N_DISTINCT ->
        "Play ${challenge.targetCount} different games"
    ChallengeType.PLAYER_WIN_STREAK -> {
        val playerLabel = challenge.playerNames.firstOrNull()?.takeIf { it.isNotBlank() } ?: "a player"
        "Reach a ${challenge.targetCount}-win streak with $playerLabel"
    }
    ChallengeType.PLAY_WITH_GROUP_N_TIMES -> {
        val names = challenge.playerNames.filter { it.isNotBlank() }
        val label = when {
            names.isEmpty() -> "this group"
            names.size <= 3 -> names.joinToString(", ")
            else -> names.take(3).joinToString(", ") + " +${names.size - 3}"
        }
        "Play ${challenge.targetCount} times with $label"
    }
    ChallengeType.PLAY_STREAK -> {
        val periodLabel = when (challenge.streakPeriod ?: "WEEKLY") {
            "DAILY" -> "day"
            "MONTHLY" -> "month"
            else -> "week"
        }
        "Log plays for ${challenge.targetCount} consecutive ${periodLabel}s"
    }
    ChallengeType.PLAY_N_UNPLAYED ->
        "Play ${challenge.targetCount} owned games for the first time"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateChallengeDialog(
    collectionItems: List<GameItem>,
    players: List<Player>,
    initialChallenge: Challenge? = null,
    // BoardGameGeek search for a game outside the collection; null hides "Search BoardGameGeek".
    bggSearch: BggGameSearch? = null,
    onDismiss: () -> Unit,
    onSave: (Challenge) -> Unit
) {
    val isEditing = initialChallenge != null
    var title by rememberSaveable(initialChallenge?.id) { mutableStateOf(initialChallenge?.title.orEmpty()) }
    var selectedType by rememberSaveable(initialChallenge?.id) { mutableStateOf(initialChallenge?.type ?: ChallengeType.PLAY_N_TIMES) }
    var targetCount by rememberSaveable(initialChallenge?.id) { mutableStateOf(initialChallenge?.targetCount?.toString() ?: "10") }
    var gameQuery by rememberSaveable(initialChallenge?.id) { mutableStateOf(initialChallenge?.gameName.orEmpty()) }
    var selectedGame by remember(initialChallenge?.id, collectionItems) {
        mutableStateOf(
            initialChallenge?.gameId?.let { id ->
                collectionItems.firstOrNull { it.objectId.toIntOrNull() == id }
                    // A challenge can be for a game picked from BoardGameGeek, outside the collection.
                    ?: initialChallenge.gameName?.takeIf { it.isNotBlank() }?.let { bggOnlyGameItem(id, it) }
            }
        )
    }
    var playerQuery by rememberSaveable(initialChallenge?.id) { mutableStateOf("") }
    var selectedPlayers by remember(initialChallenge?.id, players) {
        mutableStateOf(
            initialChallenge?.playerIds?.mapNotNull { playerId -> players.firstOrNull { it.id == playerId } }
                ?.ifEmpty {
                    initialChallenge?.playerNames.orEmpty().mapNotNull { playerName ->
                        players.firstOrNull { it.displayName.equals(playerName, ignoreCase = true) }
                    }
                }
                ?: emptyList()
        )
    }
    var startDate by rememberSaveable(initialChallenge?.id) { mutableStateOf(initialChallenge?.startDate.orEmpty()) }
    var endDate by rememberSaveable(initialChallenge?.id) { mutableStateOf(initialChallenge?.endDate.orEmpty()) }
    var streakPeriod by rememberSaveable(initialChallenge?.id) { mutableStateOf(initialChallenge?.streakPeriod ?: "WEEKLY") }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showBggSearch by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var debouncedQuery by remember { mutableStateOf("") }
    var debouncedPlayerQuery by remember { mutableStateOf("") }

    LaunchedEffect(gameQuery) {
        delay(400)
        debouncedQuery = gameQuery
    }

    LaunchedEffect(playerQuery) {
        delay(250)
        debouncedPlayerQuery = playerQuery
    }

    val filteredGames = remember(debouncedQuery, selectedGame, collectionItems) {
        if (debouncedQuery.length < 2 || selectedGame != null) emptyList()
        else collectionItems.filter { it.name.contains(debouncedQuery, ignoreCase = true) }.take(8)
    }

    val target = targetCount.toIntOrNull() ?: 0
    val requiresGame = selectedType == ChallengeType.PLAY_SPECIFIC_GAME
    val gameOk = !requiresGame || selectedGame != null
    val requiresSinglePlayer = selectedType == ChallengeType.PLAYER_WIN_STREAK
    val requiresGroupPlayers = selectedType == ChallengeType.PLAY_WITH_GROUP_N_TIMES

    val filteredPlayers = remember(debouncedPlayerQuery, players, selectedPlayers) {
        val query = debouncedPlayerQuery.trim()
        if (query.length < 2 || (requiresSinglePlayer && selectedPlayers.isNotEmpty())) emptyList()
        else players.filter { player ->
            selectedPlayers.none { it.id == player.id } &&
                (player.displayName.contains(query, ignoreCase = true) ||
                    player.aliases.any { it.contains(query, ignoreCase = true) })
        }.take(8)
    }
    val playerOk = when {
        requiresSinglePlayer -> selectedPlayers.size == 1
        requiresGroupPlayers -> selectedPlayers.size >= 2
        else -> true
    }

    fun String.toInitialMillis(): Long = runCatching {
        LocalDate.parse(this).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }.getOrDefault(System.currentTimeMillis())

    if (showStartDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate.toInitialMillis())
        DatePickerDialog(
            colors = boardFlowDatePickerColors(),
            tonalElevation = 0.dp,
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        startDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Cancel") }
            }
        ) { DatePicker(state = state, colors = boardFlowDatePickerColors()) }
    }

    if (showEndDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate.toInitialMillis())
        DatePickerDialog(
            colors = boardFlowDatePickerColors(),
            tonalElevation = 0.dp,
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        endDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) { Text("Cancel") }
            }
        ) { DatePicker(state = state, colors = boardFlowDatePickerColors()) }
    }

    val missingInput = when {
        !gameOk -> "Pick a game to continue."
        requiresSinglePlayer && !playerOk -> "Pick one player to continue."
        requiresGroupPlayers && !playerOk -> "Pick at least two players to continue."
        target <= 0 -> "Enter a target above zero."
        else -> null
    }

    val save = {
        val effectiveTitle = title.trim().ifBlank {
            challengeDescription(
                Challenge(
                    id = "", title = "",
                    type = selectedType,
                    targetCount = target,
                    gameId = selectedGame?.objectId?.toIntOrNull(),
                    gameName = selectedGame?.name,
                    playerIds = selectedPlayers.map { it.id },
                    playerNames = selectedPlayers.map { it.displayName },
                    streakPeriod = if (selectedType == ChallengeType.PLAY_STREAK) streakPeriod else null
                )
            )
        }
        onSave(
            Challenge(
                id = initialChallenge?.id ?: UUID.randomUUID().toString(),
                title = effectiveTitle,
                type = selectedType,
                targetCount = target,
                gameId = selectedGame?.objectId?.toIntOrNull(),
                gameName = selectedGame?.name,
                playerIds = selectedPlayers.map { it.id },
                playerNames = selectedPlayers.map { it.displayName },
                startDate = startDate.trim().ifBlank { null },
                endDate = endDate.trim().ifBlank { null },
                createdAt = initialChallenge?.createdAt ?: System.currentTimeMillis(),
                streakPeriod = if (selectedType == ChallengeType.PLAY_STREAK) streakPeriod else null,
                status = initialChallenge?.status ?: ChallengeStatus.ACTIVE
            )
        )
    }

    AnimatedDialog(onDismissRequest = onDismiss) {
        Column {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                item {
                    Text(
                        if (isEditing) "Edit challenge" else "New challenge",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        BoardFlowSectionTitle(title = "What is the goal?")
                        BoardFlowFormGroup(raised = true) {
                            ChallengeType.entries.forEachIndexed { index, type ->
                                if (index > 0) BoardFlowFormDivider()
                                val isSelected = type == selectedType
                                val meta = challengeTypeMeta(type)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedType = type
                                            if (type != ChallengeType.PLAY_SPECIFIC_GAME) {
                                                selectedGame = null
                                                gameQuery = ""
                                            }
                                            val newSinglePlayer = type == ChallengeType.PLAYER_WIN_STREAK
                                            val newGroupPlayers = type == ChallengeType.PLAY_WITH_GROUP_N_TIMES
                                            if (!newSinglePlayer && !newGroupPlayers) {
                                                selectedPlayers = emptyList()
                                                playerQuery = ""
                                            } else if (newSinglePlayer && selectedPlayers.size > 1) {
                                                selectedPlayers = selectedPlayers.take(1)
                                            }
                                        }
                                        .heightIn(min = 56.dp)
                                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        meta.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimens.Icon),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            type.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Text(
                                                meta.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(Dimens.Icon),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (requiresGame) {
                    item {
                        BoardFlowFormGroup(raised = true) {
                            BoardFlowFormRow(label = "Game", icon = Icons.Default.SportsEsports) {
                                BoardFlowInlineField(
                                    value = selectedGame?.name ?: gameQuery,
                                    onValueChange = {
                                        gameQuery = it
                                        selectedGame = null
                                    },
                                    placeholder = "Search your collection",
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedGame != null) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Game picked",
                                        modifier = Modifier.size(Dimens.Icon),
                                        tint = BoardFlowColors.Success
                                    )
                                }
                            }
                            filteredGames.forEach { game ->
                                BoardFlowFormDivider()
                                SuggestionRow(game.name) {
                                    selectedGame = game
                                    gameQuery = game.name
                                }
                            }
                            if (bggSearch != null && selectedGame == null && debouncedQuery.trim().length >= BggGameSearch.MIN_QUERY) {
                                BoardFlowFormDivider()
                                SearchBggRow(query = debouncedQuery) {
                                    bggSearch.search(debouncedQuery)
                                    showBggSearch = true
                                }
                            }
                        }
                    }
                }

                if (requiresSinglePlayer || requiresGroupPlayers) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            if (selectedPlayers.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                    selectedPlayers.forEach { player ->
                                        BoardFlowFilterChip(
                                            selected = true,
                                            onClick = {
                                                selectedPlayers = selectedPlayers.filterNot { it.id == player.id }
                                            },
                                            label = { Text(player.displayName) }
                                        )
                                    }
                                }
                            }
                            if (!requiresSinglePlayer || selectedPlayers.isEmpty()) {
                                BoardFlowFormGroup(raised = true) {
                                    BoardFlowFormRow(
                                        label = if (requiresSinglePlayer) "Player" else "Players",
                                        icon = Icons.Default.Groups
                                    ) {
                                        BoardFlowInlineField(
                                            value = playerQuery,
                                            onValueChange = { playerQuery = it },
                                            placeholder = "Search your players",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    filteredPlayers.forEach { player ->
                                        BoardFlowFormDivider()
                                        SuggestionRow(player.displayName) {
                                            selectedPlayers = if (requiresSinglePlayer) listOf(player)
                                            else (selectedPlayers + player).distinctBy { it.id }
                                            playerQuery = ""
                                        }
                                    }
                                }
                            }
                            Text(
                                if (requiresSinglePlayer)
                                    "Counts the best run of consecutive wins in the date window."
                                else
                                    "A play counts when every selected player took part.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (selectedType == ChallengeType.PLAY_STREAK) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                listOf("DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly").forEach { (key, label) ->
                                    BoardFlowFilterChip(
                                        selected = streakPeriod == key,
                                        onClick = { streakPeriod = key },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            Text(
                                "A period counts when you log at least one play in it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        BoardFlowSectionTitle(title = "Details")
                        BoardFlowFormGroup(raised = true) {
                            BoardFlowFormRow(label = "Target", icon = Icons.Default.Flag) {
                                BoardFlowInlineField(
                                    value = targetCount,
                                    onValueChange = { typed -> targetCount = typed.filter { it.isDigit() } },
                                    placeholder = "How many",
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            BoardFlowFormDivider()
                            DateRow("Starts", startDate, { showStartDatePicker = true }, { startDate = "" })
                            BoardFlowFormDivider()
                            DateRow("Ends", endDate, { showEndDatePicker = true }, { endDate = "" })
                            BoardFlowFormDivider()
                            BoardFlowFormRow(label = "Title", icon = Icons.Default.Edit) {
                                BoardFlowInlineField(
                                    value = title,
                                    onValueChange = { title = it },
                                    placeholder = "Optional",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (missingInput != null) {
                    item {
                        Text(
                            missingInput,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(thickness = Dimens.Hairline, color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End)
            ) {
                BoardFlowInlineAction(onClick = onDismiss, neutral = true, large = true) { Text("Cancel") }
                BoardFlowButton(onClick = save, enabled = missingInput == null) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(Dimens.Icon))
                    Spacer(Modifier.width(Spacing.sm))
                    Text(if (isEditing) "Save" else "Create")
                }
            }
        }
        // Inside the dialog, so the sheet's window opens on top of the form.
        if (showBggSearch && bggSearch != null) {
            val search by bggSearch.state.collectAsState()
            val collectionIds = remember(collectionItems) { collectionItems.mapNotNull { it.objectId.toIntOrNull() }.toSet() }
            BggSearchSheet(
                search = search,
                title = "Pick a game",
                collectionIds = collectionIds,
                onOpen = { game ->
                    selectedGame = collectionItems.firstOrNull { it.objectId == game.id.toString() }
                        ?: bggOnlyGameItem(game.id, game.name, game.thumbnailUrl)
                    gameQuery = game.name
                    showBggSearch = false
                    bggSearch.clear()
                },
                onDismiss = {
                    showBggSearch = false
                    bggSearch.clear()
                }
            )
        }
    }
}

/** A search result inside a form group. Amber, because tapping picks it. */
@Composable
private fun SuggestionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = Dimens.MinTouchTarget)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/** An optional date: tap to pick, the cross clears it. */
@Composable
private fun DateRow(label: String, value: String, onPick: () -> Unit, onClear: () -> Unit) {
    BoardFlowFormRow(label = label, icon = Icons.Default.CalendarMonth, onClick = onPick) {
        Text(
            if (value.isBlank()) "Any time" else formatDisplayDate(value),
            style = MaterialTheme.typography.bodyLarge,
            color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (value.isBlank()) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Pick date", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            BoardFlowIconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Clear date", modifier = Modifier.size(Dimens.Icon))
            }
        }
    }
}
