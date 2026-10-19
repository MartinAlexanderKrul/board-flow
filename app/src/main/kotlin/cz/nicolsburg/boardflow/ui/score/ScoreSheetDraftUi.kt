package cz.nicolsburg.boardflow.ui.score

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.BuildConfig
import cz.nicolsburg.boardflow.data.scoresheet.ScoreSheetDraftService.DraftState
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.SheetOrigin
import cz.nicolsburg.boardflow.ui.common.BoardFlowCard
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationKind
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.rememberNotificationPermissionRequest
import cz.nicolsburg.boardflow.ui.theme.Spacing

/**
 * Log Play's offer for a game with no score sheet: pick the rulebook PDF and Gemini drafts one in
 * the background. Shows the running draft, or why the last one failed, in place. "Not needed"
 * marks the game as needing no sheet, so the offer stops (games known to need none never get it).
 */
@Composable
fun ScoreSheetDraftCard(
    state: DraftState,
    onPickPdf: (Uri) -> Unit,
    onNotNeeded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPickPdf(uri)
    }
    // The draft reports back by notification, so ask for it as the user starts one.
    val requestNotifications = rememberNotificationPermissionRequest()
    val running = state is DraftState.Running
    BoardFlowCard(
        modifier = modifier,
        onClick = if (running) null else ({ requestNotifications(); picker.launch(arrayOf("application/pdf")) }),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (running) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    when (state) {
                        DraftState.Running -> "Drafting a score sheet"
                        is DraftState.Failed -> "Could not draft a score sheet"
                        else -> "Draft a score sheet"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    when (state) {
                        DraftState.Running -> "Gemini is reading the rulebook. You can keep logging; it shows up here when ready."
                        is DraftState.Failed -> "${state.message} Tap to try another rulebook PDF."
                        else -> "No score sheet for this game yet. Pick its rulebook PDF and Gemini writes one."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state is DraftState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!running) {
                BoardFlowInlineAction(onClick = onNotNeeded, neutral = true) { Text("Not needed") }
            }
        }
    }
}

/** Left where the draft card was after "Not needed", until the form is left. */
@Composable
fun NoScoreSheetUndo(onUndo: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            "No score sheet for this game. A draft is not offered again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        BoardFlowInlineAction(onClick = onUndo) { Text("Undo") }
    }
}

/**
 * Above the user's own sheets: a drafted sheet is not trusted until the user has checked it, so it
 * says so and offers to mark it reviewed. Any user sheet can be deleted, which brings back the
 * built-in sheet if the game has one.
 */
@Composable
fun UserSheetBanner(
    sheet: ScoreSheet,
    onMarkReviewed: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (sheet.origin == SheetOrigin.BUILT_IN || sheet.origin == SheetOrigin.COMMUNITY) return
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        BoardFlowConfirmationDialog(
            title = "Delete this score sheet?",
            message = "The sheet is removed from this phone. You can draft it again from the rulebook.",
            confirmLabel = "Delete",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false }
        )
    }
    val draft = sheet.origin == SheetOrigin.AI_DRAFT && !sheet.reviewed
    BoardFlowCard(modifier = modifier, contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                if (draft) "Draft – check it against the rulebook" else "Your own score sheet",
                style = MaterialTheme.typography.titleSmall,
                color = if (draft) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                buildString {
                    append(if (sheet.origin == SheetOrigin.AI_DRAFT) "Drafted by Gemini" else "Imported")
                    sheet.sources.firstOrNull()?.let { append(" from $it") }
                    append(".")
                    if (draft) {
                        append(" Point values or rows may be wrong; compare them with the rulebook's scoring section.")
                        if (BuildConfig.CONTRIBUTION_RELAY_URL.isNotBlank()) append(" Once reviewed, it is shared with all BoardFlow users.")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End)
            ) {
                BoardFlowInlineAction(onClick = { confirmDelete = true }, destructive = true) { Text("Delete") }
                if (draft) BoardFlowInlineAction(onClick = onMarkReviewed) { Text("Mark as reviewed") }
            }
        }
    }
}
