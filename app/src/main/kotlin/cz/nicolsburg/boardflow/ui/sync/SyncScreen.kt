package cz.nicolsburg.boardflow.ui.sync

import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import cz.nicolsburg.boardflow.ui.theme.Spacing
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.BoardFlowColors
import cz.nicolsburg.boardflow.ui.common.BoardFlowIcons
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormDivider
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormGroup
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import cz.nicolsburg.boardflow.ui.common.BoardFlowTextField
import android.accounts.Account
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.SyncViewModel
import cz.nicolsburg.boardflow.model.LogEntry
import cz.nicolsburg.boardflow.ui.common.AnimatedDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowAnimatedVisibility
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationKind
import cz.nicolsburg.boardflow.ui.common.BoardFlowOutlinedButton
import cz.nicolsburg.boardflow.ui.common.SectionCard

// ── Log summary model ─────────────────────────────────────────────────────────

private data class LogSummary(val headline: String, val detail: String?, val isError: Boolean)

private fun List<LogEntry>.deriveSummary(): LogSummary? {
    if (isEmpty()) return null
    val header = lastOrNull { it.type == LogEntry.Type.HEADER }
    val result = lastOrNull { it.type == LogEntry.Type.DONE || it.type == LogEntry.Type.ERROR }

    // A run can end on a DONE entry even though an earlier step failed (for example the
    // play history fetch). Do not report that as a clean "Done".
    val runStart = indexOfLast { it.type == LogEntry.Type.HEADER }.coerceAtLeast(0)
    val runErrors = subList(runStart, size).filter { it.type == LogEntry.Type.ERROR }
    if (result?.type == LogEntry.Type.DONE && runErrors.isNotEmpty()) {
        val failed = if (runErrors.size == 1) "1 step failed" else "${runErrors.size} steps failed"
        return LogSummary(
            headline = "Finished with errors",
            detail = listOfNotNull(result.status.ifBlank { null }, "$failed: ${runErrors.first().status}")
                .joinToString(" - "),
            isError = true
        )
    }

    val headline = when {
        result?.name?.contains("Collection cached", ignoreCase = true) == true -> "Collection updated"
        result?.name?.contains("Sleeve refresh", ignoreCase = true) == true -> "Sleeve data refreshed"
        result?.name?.contains("Sync complete", ignoreCase = true) == true -> "Sync complete"
        result?.name?.contains("Connected", ignoreCase = true) == true -> "Sheet connected"
        result?.name?.contains("Done", ignoreCase = true) == true -> when {
            header?.name?.contains("Folder", ignoreCase = true) == true -> "Folders ready"
            header?.name?.contains("CSV", ignoreCase = true) == true -> "CSV import complete"
            else -> "Done"
        }
        result?.type == LogEntry.Type.ERROR -> "Finished with errors"
        header?.name?.contains("Refresh", ignoreCase = true) == true -> "Refreshing…"
        header?.name?.contains("Sync", ignoreCase = true) == true -> "Syncing…"
        header?.name?.contains("Connect", ignoreCase = true) == true -> "Connecting…"
        else -> header?.name ?: "Working…"
    }

    return LogSummary(
        headline = headline,
        detail = result?.status?.ifBlank { null },
        isError = result?.type == LogEntry.Type.ERROR
    )
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun SyncScreen(
    syncViewModel: SyncViewModel,
    onPickCsv: () -> Unit,
    onSpreadsheetChanged: (String) -> Unit,
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    bggUsername: String = "",
    bggPassword: String = "",
    onSaveBggCredentials: (String, String) -> Unit = { _, _ -> }
) {
    val account by syncViewModel.account.collectAsState()
    val spreadsheetId by syncViewModel.spreadsheetId.collectAsState()
    val spreadsheetTitle by syncViewModel.spreadsheetTitle.collectAsState()
    val log by syncViewModel.log.collectAsState()
    val busy by syncViewModel.busy.collectAsState()
    val hasBggCredentials by syncViewModel.hasBggCredentials.collectAsState()
    val lastSyncedAt by syncViewModel.lastSyncedAt.collectAsState()

    val hasConfiguredSheet = spreadsheetId.isNotBlank()
    val googleConnected = account != null
    val canSync = googleConnected && hasConfiguredSheet && hasBggCredentials

    val syncHint = when {
        !hasBggCredentials && !googleConnected -> "Set up BGG and sign in to Google first"
        !hasBggCredentials -> "Set up your BGG account first"
        !googleConnected -> "Sign in to Google first"
        !hasConfiguredSheet -> "Connect a sheet above to sync"
        else -> null
    }

    // Compact display label for the connected sheet
    val sheetDisplayLabel = when {
        spreadsheetTitle.isNotBlank() -> spreadsheetTitle
        spreadsheetId.isNotBlank() -> "…${spreadsheetId.takeLast(8)}"
        else -> ""
    }

    var showSheetModal by remember { mutableStateOf(false) }
    var showGoogleModal by remember { mutableStateOf(false) }
    var showBggModal by remember { mutableStateOf(false) }
    var saveQrToDevice by remember { mutableStateOf(false) }
    var logDialogOpen by rememberSaveable { mutableStateOf(false) }
    var logAutoOpenDismissedRun by rememberSaveable { mutableStateOf(-1) }
    var showClearLogConfirm by remember { mutableStateOf(false) }
    var pendingSyncAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun triggerSync(action: () -> Unit) {
        val elapsed = System.currentTimeMillis() - lastSyncedAt
        if (lastSyncedAt > 0L && elapsed < 3_600_000L) {
            pendingSyncAction = action
        } else {
            action()
        }
    }

    val listState = rememberLazyListState()

    // Only show the log bar/dialog for actual sync/refresh operations — requires a
    // HEADER entry (produced by runSync) and must not be a sheet-connect operation.
    // Use the most recent HEADER so a prior "Connect" run doesn't suppress later syncs.
    val isSyncLog = log.lastOrNull { it.type == LogEntry.Type.HEADER }
        ?.name?.let { !it.startsWith("Connect", ignoreCase = true) } == true
    val currentLogRun = log.count { it.type == LogEntry.Type.HEADER }

    LaunchedEffect(Unit) { syncViewModel.refreshCredentialState() }
    LaunchedEffect(log.size, busy, currentLogRun) {
        if (log.isNotEmpty() && isSyncLog) {
            if (busy && logAutoOpenDismissedRun != currentLogRun) {
                logDialogOpen = true
            }
            listState.animateScrollToItem(log.size - 1)
        }
    }

    if (showSheetModal) {
        SpreadsheetConnectDialog(
            currentSheetName = spreadsheetTitle.ifBlank { null },
            onDismiss = { showSheetModal = false },
            onConnect = { input ->
                val acc = account ?: return@SpreadsheetConnectDialog
                showSheetModal = false
                onSpreadsheetChanged(input)
                syncViewModel.connectExistingSpreadsheet(acc, input)
            },
            onCreateNew = account?.let { acc -> {
                showSheetModal = false
                syncViewModel.createSpreadsheetFromBgg(acc)
            }}
        )
    }

    if (showGoogleModal) {
        GoogleManageDialog(
            accountEmail = account?.name,
            onDismiss = { showGoogleModal = false },
            onSignIn = { onSignIn(); showGoogleModal = false },
            onSignOut = { onSignOut(); showGoogleModal = false }
        )
    }

    if (showBggModal) {
        BggEditDialog(
            initialUsername = bggUsername,
            initialPassword = bggPassword,
            onDismiss = { showBggModal = false },
            onSave = { u, p ->
                onSaveBggCredentials(u, p)
                showBggModal = false
            }
        )
    }

    if (logDialogOpen && log.isNotEmpty()) {
        LogDialog(
            log = log,
            listState = listState,
            onDismiss = {
                logDialogOpen = false
                if (busy) {
                    logAutoOpenDismissedRun = currentLogRun
                }
            }
        )
    }

    if (showClearLogConfirm) {
        BoardFlowConfirmationDialog(
            title = "Clear sync log?",
            message = "Remove all entries from the local sync log?",
            confirmLabel = "Clear log",
            dismissLabel = "Cancel",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = {
                syncViewModel.clearLog()
                logDialogOpen = false
                logAutoOpenDismissedRun = -1
                showClearLogConfirm = false
            },
            onDismiss = { showClearLogConfirm = false }
        )
    }

    if (pendingSyncAction != null) {
        val elapsedMs = System.currentTimeMillis() - lastSyncedAt
        val minutes = (elapsedMs / 60_000).toInt()
        val timeText = if (minutes < 1) "less than a minute ago" else "$minutes minute${if (minutes == 1) "" else "s"} ago"
        BoardFlowConfirmationDialog(
            title = "Sync again?",
            message = "Collection was last synced $timeText. Do you want to sync again?",
            confirmLabel = "Sync",
            dismissLabel = "Cancel",
            kind = BoardFlowConfirmationKind.NEUTRAL,
            onConfirm = {
                pendingSyncAction?.invoke()
                pendingSyncAction = null
            },
            onDismiss = { pendingSyncAction = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (log.isNotEmpty() && isSyncLog) {
                LogBar(log = log, busy = busy, onClick = { logDialogOpen = true })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Readiness hub (status + actions) ──────────────────────
                ReadinessHub(
                    googleConnected = googleConnected,
                    googleLabel = account?.name.orEmpty(),
                    bggConnected = hasBggCredentials,
                    sheetConnected = hasConfiguredSheet,
                    sheetLabel = sheetDisplayLabel,
                    onManageGoogle = { showGoogleModal = true },
                    onEditBgg = { showBggModal = true },
                    onChangeSheet = { showSheetModal = true }
                )

                // ── Step 1 — BGG ──────────────────────────────────────────
                StepSectionHeader(
                    step = "1",
                    title = "BoardGameGeek",
                    subtitle = "Fetch your latest collection from BGG."
                )
                BoardFlowButton(
                    onClick = { triggerSync { syncViewModel.refreshCollection(forceRefresh = true) } },
                    enabled = !busy && hasBggCredentials
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(Dimens.Icon))
                    Spacer(Modifier.size(8.dp))
                    Text("Refresh collection")
                }
                BoardFlowFormGroup {
                    SyncActionRow(
                        icon = BoardFlowIcons.Sleeves,
                        title = "Refresh sleeve sizes",
                        detail = "Read card counts and sizes from BGG.",
                        enabled = !busy && hasBggCredentials,
                        onClick = { triggerSync { syncViewModel.refreshSleeveDataFromBgg(forceRefresh = true) } }
                    )
                    BoardFlowFormDivider()
                    SyncActionRow(
                        icon = Icons.Default.CloudUpload,
                        title = "Back up sleeve status to BGG",
                        detail = "Saves sleeved / to sleeve in each game's private notes.",
                        enabled = !busy && hasBggCredentials,
                        // Not a collection sync, so it skips the "Sync again?" prompt.
                        onClick = { syncViewModel.backupSleeveStatusToBgg() }
                    )
                }
                if (!hasBggCredentials) {
                    InlineHint("Set up your BGG account", onClick = { showBggModal = true })
                }

                // ── Step 2 — Google Sheets (only when signed in to Google) ──
                AnimatedVisibility(visible = googleConnected) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        HorizontalDivider()

                        StepSectionHeader(
                            step = "2",
                            title = "Google Sheets",
                            subtitle = "Push your collection to the connected spreadsheet."
                        )
                        run {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                BoardFlowButton(
                                    onClick = {
                                        val acc = account ?: return@BoardFlowButton
                                        triggerSync {
                                            onSpreadsheetChanged(spreadsheetId)
                                            syncViewModel.syncBgg(acc, forceRefresh = true)
                                        }
                                    },
                                    enabled = !busy && canSync
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("Sync to Google Sheets")
                                }

                                if (!canSync && syncHint != null) {
                                    InlineHint(
                                        text = syncHint,
                                        onClick = when {
                                            !hasBggCredentials -> { { showBggModal = true } }
                                            else -> null
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // ── Advanced (collapsed) ──────────────────────────
                        AdvancedSection(
                            busy = busy,
                            account = account,
                            hasConfiguredSheet = hasConfiguredSheet,
                            saveQrToDevice = saveQrToDevice,
                            onSaveQrChanged = { saveQrToDevice = it },
                            onPickCsv = {
                                account ?: return@AdvancedSection
                                onSpreadsheetChanged(spreadsheetId)
                                onPickCsv()
                            },
                            onCreateFolders = {
                                val acc = account ?: return@AdvancedSection
                                onSpreadsheetChanged(spreadsheetId)
                                syncViewModel.createFolders(acc, saveQrToGallery = saveQrToDevice)
                            }
                        )
                    }
                }

                // ── Controls ──────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    if (busy) {
                        BoardFlowOutlinedButton(
                            onClick = { syncViewModel.stopSync() }
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Stop")
                        }
                    }
                    if (log.isNotEmpty()) {
                        BoardFlowInlineAction(onClick = { showClearLogConfirm = true }) {
                            Text("Clear log")
                        }
                    }
                }

                if (busy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ── Private composables ────────────────────────────────────────────────────────

@Composable
private fun ReadinessHub(
    googleConnected: Boolean,
    googleLabel: String,
    bggConnected: Boolean,
    sheetConnected: Boolean,
    sheetLabel: String,
    onManageGoogle: () -> Unit,
    onEditBgg: () -> Unit,
    onChangeSheet: () -> Unit
) {
    BoardFlowFormGroup {
        ActionStatusRow(
            label = "Google",
            connected = googleConnected,
            detail = if (googleConnected) googleLabel else "Not signed in",
            actionLabel = if (googleConnected) "Manage" else "Sign in",
            onAction = onManageGoogle
        )
        BoardFlowFormDivider()
        ActionStatusRow(
            label = "BGG",
            connected = bggConnected,
            detail = if (bggConnected) "Account saved" else "Not set up",
            actionLabel = if (bggConnected) "Edit" else "Set up",
            onAction = onEditBgg
        )
        BoardFlowFormDivider()
        ActionStatusRow(
            label = "Sheet",
            connected = sheetConnected,
            detail = if (sheetConnected) sheetLabel else "No sheet selected",
            actionLabel = when {
                sheetConnected -> "Change"
                googleConnected -> "Connect"
                else -> null
            },
            onAction = if (sheetConnected || googleConnected) onChangeSheet else null
        )
    }
}

@Composable
private fun ActionStatusRow(
    label: String,
    connected: Boolean,
    detail: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier)
            .heightIn(min = Dimens.FieldHeight)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(
            if (connected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (connected) "Ready" else "Not ready",
            modifier = Modifier.size(Dimens.Icon),
            tint = if (connected) BoardFlowColors.Success else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        Text(
            detail,
            style = MaterialTheme.typography.bodyLarge,
            color = if (connected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** A secondary sync action: what it does, and a chevron. Runs when tapped. */
@Composable
private fun SyncActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .heightIn(min = 64.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.Icon), tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepSectionHeader(step: String, title: String, subtitle: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        step,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp)
            )
        }
    }
}

@Composable
private fun InlineHint(text: String, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onClick != null) {
            Text(
                "→",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AdvancedSection(
    busy: Boolean,
    account: Account?,
    hasConfiguredSheet: Boolean,
    saveQrToDevice: Boolean,
    onSaveQrChanged: (Boolean) -> Unit,
    onPickCsv: () -> Unit,
    onCreateFolders: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .heightIn(min = Dimens.MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Advanced",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BoardFlowAnimatedVisibility(visible = expanded) {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdvancedGroupLabel("Import and export")
                    BoardFlowOutlinedButton(
                        onClick = onPickCsv,
                        enabled = !busy && account != null && hasConfiguredSheet
                    ) {
                        Text("Import from CSV")
                    }

                    AdvancedGroupLabel("Automation")
                    BoardFlowOutlinedButton(
                        onClick = onCreateFolders,
                        enabled = !busy && account != null && hasConfiguredSheet
                    ) {
                        Text("Create Drive folders and QR codes")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = saveQrToDevice, onCheckedChange = onSaveQrChanged)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Also save QR images to this device", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Enable this to copy QR PNG files into local storage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedGroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun LogBar(log: List<LogEntry>, busy: Boolean, onClick: () -> Unit) {
    val summary = log.deriveSummary() ?: return
    val containerColor = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface
    val headlineColor = when {
        busy -> MaterialTheme.colorScheme.onSurface
        summary.isError -> MaterialTheme.colorScheme.error
        else -> BoardFlowColors.Success
    }

    Surface(
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(summary.headline, color = headlineColor, style = MaterialTheme.typography.titleSmall)
                summary.detail?.let {
                    Text(it, color = contentColor.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "View details",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "View log details",
                tint = contentColor.copy(alpha = 0.65f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LogDialog(
    log: List<LogEntry>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onDismiss: () -> Unit
) {
    val summary = log.deriveSummary()

    AnimatedDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Last sync",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                }

                // User-facing result summary
                if (summary != null) {
                    val summaryContainer = MaterialTheme.colorScheme.surfaceContainerHigh
                    val summaryContent = if (summary.isError) MaterialTheme.colorScheme.error
                    else BoardFlowColors.Success

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        color = summaryContainer,
                        shape = BoardFlowShape.Control
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                summary.headline,
                                style = MaterialTheme.typography.titleSmall,
                                color = summaryContent
                            )
                            summary.detail?.let { detail ->
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = summaryContent.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Details label
                Text(
                    "Details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp)
                )

                // Raw log entries
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(log) { entry -> LogEntryRow(entry) }
                }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val (iconText, iconColor) = when (entry.type) {
        LogEntry.Type.DONE -> "OK" to Color(0xFF4CAF50)
        LogEntry.Type.INSERTED -> "+" to MaterialTheme.colorScheme.primary
        LogEntry.Type.UPDATED -> "~" to MaterialTheme.colorScheme.onSurfaceVariant
        LogEntry.Type.ERROR -> "x" to MaterialTheme.colorScheme.error
        LogEntry.Type.HEADER -> ">" to MaterialTheme.colorScheme.primary
        LogEntry.Type.INFO -> "-" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            iconText,
            color = iconColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.size(width = 20.dp, height = 14.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.name,
                style = if (entry.type == LogEntry.Type.HEADER) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (entry.type == LogEntry.Type.HEADER) FontWeight.Bold else FontWeight.Normal,
                color = if (entry.type == LogEntry.Type.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (entry.status.isNotBlank()) {
                Text(
                    entry.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Account dialogs

@Composable
private fun GoogleManageDialog(
    accountEmail: String?,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    var showSignOutConfirm by remember { mutableStateOf(false) }

    if (showSignOutConfirm) {
        BoardFlowConfirmationDialog(
            title = "Sign out of Google?",
            message = "Google Sheets and Drive sync will be unavailable until you sign in again.",
            confirmLabel = "Sign out",
            dismissLabel = "Cancel",
            kind = BoardFlowConfirmationKind.NEUTRAL,
            onConfirm = {
                showSignOutConfirm = false
                onSignOut()
            },
            onDismiss = { showSignOutConfirm = false }
        )
    }

    AnimatedDialog(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "Google account",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (accountEmail != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Signed in as",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                accountEmail,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            BoardFlowOutlinedButton(
                                onClick = { showSignOutConfirm = true }
                            ) {
                                Text("Sign out")
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            "Sign in to enable Google Sheets sync.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            BoardFlowButton(onClick = onSignIn) {
                                Text("Sign in with Google")
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun BggEditDialog(
    initialUsername: String,
    initialPassword: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var showPwd by remember { mutableStateOf(false) }

    AnimatedDialog(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            "BGG account",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            BoardFlowTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column {
                            BoardFlowTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { showPwd = !showPwd }) {
                                        Icon(
                                            if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showPwd) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        BoardFlowInlineAction(onClick = onDismiss, destructive = true, large = true) { Text("Cancel") }
                        BoardFlowButton(
                            onClick = { onSave(username.trim(), password.trim()) },
                            enabled = username.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
        }
    }
}
