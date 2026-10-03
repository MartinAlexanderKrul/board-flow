package cz.nicolsburg.boardflow.ui.settings

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Key
import cz.nicolsburg.boardflow.ui.common.LocalBoardFlowMessenger
import cz.nicolsburg.boardflow.ui.common.BoardFlowSectionTitle
import cz.nicolsburg.boardflow.ui.common.BoardFlowSettingValue
import cz.nicolsburg.boardflow.ui.common.BoardFlowSettingRow
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import androidx.compose.material3.IconButtonDefaults
import cz.nicolsburg.boardflow.ui.common.BoardFlowIconButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.RowScope
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormDivider
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormGroup
import cz.nicolsburg.boardflow.ui.common.BoardFlowIcons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Casino
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowDestructiveButton
import cz.nicolsburg.boardflow.ui.theme.Spacing
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.common.BoardFlowTextField
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.ui.common.AnimatedDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowAnimatedVisibility
import cz.nicolsburg.boardflow.AppViewModel
import cz.nicolsburg.boardflow.data.GeminiModels
import cz.nicolsburg.boardflow.SyncViewModel
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowConfirmationKind
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowPickerField
import cz.nicolsburg.boardflow.ui.common.BoardFlowPickerSheet
import cz.nicolsburg.boardflow.ui.common.ScreenTabRow
import cz.nicolsburg.boardflow.ui.common.SectionCard
import cz.nicolsburg.boardflow.ui.common.SectionHeader
import cz.nicolsburg.boardflow.ui.common.clickableRow
import cz.nicolsburg.boardflow.ui.common.swipeToNavigateTabs
import kotlinx.coroutines.flow.collect
import cz.nicolsburg.boardflow.ui.sync.SpreadsheetConnectDialog
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.SuggestionChip
import cz.nicolsburg.boardflow.BuildConfig
import cz.nicolsburg.boardflow.model.GameRecognitionHint
import cz.nicolsburg.boardflow.model.SleeveManufacturer
import cz.nicolsburg.boardflow.model.StatsPlayScope
import java.time.LocalDate

private enum class SettingsSection(val title: String) {
    SYNC("Sync"),
    PREFERENCES("Preferences"),
    SCAN("Scan"),
    DATA("Data")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    syncViewModel: SyncViewModel,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onActiveTabChange: (String?) -> Unit = {},
    // Sync lives here as the first tab; the shell passes the Sync screen in.
    syncContent: @Composable () -> Unit = {}
) {
    val prefs = viewModel.prefs
    val context = LocalContext.current
    val messenger = LocalBoardFlowMessenger.current

    var username by remember { mutableStateOf(prefs.bggUsername) }
    var password by remember { mutableStateOf(prefs.bggPassword) }
    var apiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var modelEndpoint by remember { mutableStateOf(prefs.geminiModelEndpoint) }
    var showPwd by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }
    var manufacturerExpanded by remember { mutableStateOf(false) }
    var statsScopeExpanded by remember { mutableStateOf(false) }
    var selectedSection by rememberSaveable { mutableStateOf(SettingsSection.SYNC) }

    val currentManufacturer by viewModel.sleevePreferredManufacturer.collectAsState()
    val currentStatsPlayScope by viewModel.statsPlayScope.collectAsState()
    val recommendationsEnabled by viewModel.recommendationsEnabled.collectAsState()
    val chronicleEnabled by viewModel.chronicleEnabled.collectAsState()
    val googleAccount by syncViewModel.account.collectAsState()
    val spreadsheetId by syncViewModel.spreadsheetId.collectAsState()
    val spreadsheetTitle by syncViewModel.spreadsheetTitle.collectAsState()
    val cachedCollection by syncViewModel.collectionGames.collectAsState()

    var showSheetModal by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf<String?>(null) }
    var includeSensitiveBackup by remember { mutableStateOf(false) }
    var modelListLoading by remember { mutableStateOf(false) }
    var availableModels by remember {
        mutableStateOf(GeminiModels.candidates(GeminiModels.AUTO, prefs.getAvailableModels()).takeIf { prefs.getAvailableModels().isNotEmpty() })
    }
    var showGoogleSignOutConfirm by remember { mutableStateOf(false) }
    var showSetupGuide by remember { mutableStateOf(false) }
    var showClearCollectionConfirm by remember { mutableStateOf(false) }
    var templateCount by remember { mutableStateOf(viewModel.getGameRecognitionHints().size) }
    var showClearTemplatesConfirm by remember { mutableStateOf(false) }
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var playerHintCount by remember { mutableStateOf(viewModel.getPlayerRecognitionHintCount()) }
    var showClearPlayerHintsConfirm by remember { mutableStateOf(false) }
    val customMoods by viewModel.customMoods.collectAsState()
    var showCustomMoodsDialog by remember { mutableStateOf(false) }
    val hasCollection = cachedCollection.isNotEmpty()

    val listState = rememberLazyListState()
    var controlsVisible by remember { mutableStateOf(true) }
    val collectionSize = cachedCollection.size

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(viewModel.exportData(includeSensitiveBackup).toByteArray())
                }
                messenger.show("Data exported successfully")
            } catch (e: Exception) {
                messenger.show("Export failed: ${e.message}")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: throw Exception("Could not read file!")
                showImportConfirm = json
            } catch (e: Exception) {
                messenger.show("Import failed: ${e.message}")
            }
        }
    }

    showImportConfirm?.let { pendingJson ->
        BoardFlowConfirmationDialog(
            title = "Import backup and replace current data?",
            message = "This replaces local players, history, and non-sensitive settings on this device.",
            confirmLabel = "Import",
            dismissLabel = "Cancel",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = {
                try {
                    viewModel.importData(pendingJson)
                    username = prefs.bggUsername
                    password = prefs.bggPassword
                    apiKey = prefs.geminiApiKey
                    modelEndpoint = prefs.geminiModelEndpoint
                    syncViewModel.reloadLocalSyncPreferences()
                    syncViewModel.loadCachedCollection()
                    messenger.show("Data imported successfully")
                } catch (e: Exception) {
                    messenger.show("Import failed: ${e.message}")
                }
                showImportConfirm = null
            },
            onDismiss = { showImportConfirm = null }
        )
    }

    if (showGoogleSignOutConfirm) {
        BoardFlowConfirmationDialog(
            title = "Sign out of Google?",
            message = "Google Sheets and Drive sync will be unavailable until you sign in again.",
            confirmLabel = "Sign out",
            dismissLabel = "Cancel",
            kind = BoardFlowConfirmationKind.NEUTRAL,
            onConfirm = {
                showGoogleSignOutConfirm = false
                onSignOut()
            },
            onDismiss = { showGoogleSignOutConfirm = false }
        )
    }

    if (showClearCollectionConfirm) {
        BoardFlowConfirmationDialog(
            title = "Clear collection cache?",
            message = "This removes the cached collection from this device. You can refresh it again later from Sync.",
            confirmLabel = "Clear cache",
            dismissLabel = "Cancel",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = {
                showClearCollectionConfirm = false
                syncViewModel.clearCollectionCache()
            },
            onDismiss = { showClearCollectionConfirm = false }
        )
    }

    if (showClearTemplatesConfirm) {
        BoardFlowConfirmationDialog(
            title = "Clear recognition templates?",
            message = "This removes all saved game scoring layouts used to improve scan recognition. Your plays, roster, and collection are not affected.",
            confirmLabel = "Clear templates",
            dismissLabel = "Cancel",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = {
                showClearTemplatesConfirm = false
                viewModel.clearGameRecognitionHints()
                templateCount = 0
                messenger.show("Recognition templates cleared.")
            },
            onDismiss = { showClearTemplatesConfirm = false }
        )
    }

    if (showClearPlayerHintsConfirm) {
        BoardFlowConfirmationDialog(
            title = "Clear player recognition hints?",
            message = "This removes all saved scan-name-to-player mappings. Future scans will rely on aliases and fuzzy matching until hints are re-learned.",
            confirmLabel = "Clear hints",
            dismissLabel = "Cancel",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = {
                showClearPlayerHintsConfirm = false
                viewModel.clearPlayerRecognitionHints()
                playerHintCount = 0
                messenger.show("Player recognition hints cleared.")
            },
            onDismiss = { showClearPlayerHintsConfirm = false }
        )
    }

    if (showSetupGuide) {
        AnimatedDialog(onDismissRequest = { showSetupGuide = false }) {
            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                item { cz.nicolsburg.boardflow.ui.intro.SetupGuideContent() }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        BoardFlowButton(
                            onClick = { showSetupGuide = false }
                        ) { Text("Close") }
                    }
                }
            }
        }
    }

    if (showSheetModal) {
        SpreadsheetConnectDialog(
            currentSheetName = spreadsheetTitle.ifBlank { null },
            onDismiss = { showSheetModal = false },
            onConnect = { input ->
                val acc = googleAccount ?: return@SpreadsheetConnectDialog
                showSheetModal = false
                syncViewModel.connectExistingSpreadsheet(acc, input)
            },
            onCreateNew = googleAccount?.let { acc -> {
                showSheetModal = false
                syncViewModel.createSpreadsheetFromBgg(acc)
            }}
        )
    }

    LaunchedEffect(listState) {
        var lastIndex = 0
        var lastOffset = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val scrollingDown = index > lastIndex || (index == lastIndex && offset > lastOffset)
                val atTop = index == 0 && offset < 8
                controlsVisible = atTop || !scrollingDown
                lastIndex = index
                lastOffset = offset
            }
    }

    LaunchedEffect(selectedSection) {
        controlsVisible = true
        listState.scrollToItem(0)
    }

    LaunchedEffect(controlsVisible, selectedSection) {
        onActiveTabChange(if (controlsVisible) null else selectedSection.title)
    }

    DisposableEffect(Unit) {
        onDispose { onActiveTabChange(null) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            BoardFlowAnimatedVisibility(visible = controlsVisible) {
                ScreenTabRow(
                    tabs = SettingsSection.entries.map { it.title },
                    selectedIndex = selectedSection.ordinal,
                    onTabSelected = { selectedSection = SettingsSection.entries[it] }
                )
            }
            if (selectedSection == SettingsSection.SYNC) {
                syncContent()
            } else LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .swipeToNavigateTabs(
                        tabCount = SettingsSection.entries.size,
                        selectedIndex = selectedSection.ordinal,
                        onNavigate = { selectedSection = SettingsSection.entries[it] }
                    ),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

            if (selectedSection == SettingsSection.PREFERENCES) {
                // Sections of rows, like Sync: a title, then one group.
                item { BoardFlowSectionTitle(title = "Stats") }
                item {
                    BoardFlowFormGroup {
                        BoardFlowSettingRow(
                            icon = Icons.Default.BarChart,
                            title = "Stats source",
                            detail = currentStatsPlayScope.description,
                            onClick = { statsScopeExpanded = true }
                        ) { BoardFlowSettingValue(currentStatsPlayScope.label) }
                    }
                }
                item { BoardFlowSectionTitle(title = "Logging plays") }
                item {
                    BoardFlowFormGroup {
                        BoardFlowSettingRow(
                            icon = Icons.Default.Lightbulb,
                            title = "Recommendations",
                            detail = "Suggest what to play next after you log a play",
                            onClick = { viewModel.setRecommendationsEnabled(!recommendationsEnabled) }
                        ) {
                            androidx.compose.material3.Switch(
                                checked = recommendationsEnabled,
                                onCheckedChange = { viewModel.setRecommendationsEnabled(it) }
                            )
                        }
                        BoardFlowFormDivider()
                        BoardFlowSettingRow(
                            icon = Icons.Default.AutoStories,
                            title = "Chronicles",
                            detail = "A story line for each session, written by AI",
                            onClick = { viewModel.setChronicleEnabled(!chronicleEnabled) }
                        ) {
                            androidx.compose.material3.Switch(
                                checked = chronicleEnabled,
                                onCheckedChange = { viewModel.setChronicleEnabled(it) }
                            )
                        }
                        BoardFlowFormDivider()
                        val moodCount = customMoods.size
                        BoardFlowSettingRow(
                            icon = Icons.Default.Bookmark,
                            title = "Mood templates",
                            detail = if (moodCount == 0) "Moods you add to a session appear here"
                                     else "$moodCount custom mood${if (moodCount == 1) "" else "s"}",
                            onClick = if (moodCount > 0) ({ showCustomMoodsDialog = true }) else null
                        ) {
                            if (moodCount > 0) BoardFlowSettingValue("Manage")
                        }
                    }
                }
                item { BoardFlowSectionTitle(title = "Collection") }
                item {
                    BoardFlowFormGroup {
                        BoardFlowSettingRow(
                            icon = BoardFlowIcons.Sleeves,
                            title = "Sleeve brand",
                            detail = "Shown first in sleeve recommendations",
                            onClick = { manufacturerExpanded = true }
                        ) { BoardFlowSettingValue(currentManufacturer.label) }
                    }
                }
                item { BoardFlowSectionTitle(title = "Help") }
                item {
                    BoardFlowFormGroup {
                        BoardFlowSettingRow(
                            icon = Icons.Default.Info,
                            title = "Setup guide",
                            detail = "How to connect BGG, Google and Gemini",
                            onClick = { showSetupGuide = true }
                        )
                    }
                    if (statsScopeExpanded) {
                        BoardFlowPickerSheet(
                            title = "Choose stats source",
                            options = StatsPlayScope.entries,
                            selectedOption = currentStatsPlayScope,
                            optionLabel = { it.label },
                            onSelect = { scope ->
                                viewModel.setStatsPlayScope(scope)
                                statsScopeExpanded = false
                            },
                            onDismiss = { statsScopeExpanded = false }
                        )
                    }
                    if (manufacturerExpanded) {
                        BoardFlowPickerSheet(
                            title = "Choose sleeve brand",
                            options = SleeveManufacturer.entries,
                            selectedOption = currentManufacturer,
                            optionLabel = { it.label },
                            onSelect = { manufacturer ->
                                viewModel.setSleevePreferredManufacturer(manufacturer)
                                manufacturerExpanded = false
                            },
                            onDismiss = { manufacturerExpanded = false }
                        )
                    }
                    if (showCustomMoodsDialog) {
                        CustomMoodsDialog(
                            viewModel = viewModel,
                            onDismiss = { showCustomMoodsDialog = false }
                        )
                    }
                }
            }

            if (selectedSection == SettingsSection.SCAN) {
                item { BoardFlowSectionTitle(title = "Gemini", supporting = "Optional. Reads scoresheet photos and writes chronicles.") }
                item {
                    var showKeyDialog by remember { mutableStateOf(false) }
                    var showBackupKeysDialog by remember { mutableStateOf(false) }
                    var modelPickerOpen by remember { mutableStateOf(false) }
                    var extraKeys by remember { mutableStateOf(prefs.getGeminiExtraApiKeys()) }
                    val autoLabel = "Automatic"
                    BoardFlowFormGroup {
                        BoardFlowSettingRow(
                            icon = Icons.Default.Key,
                            title = "API key",
                            detail = "From Google AI Studio",
                            onClick = { showKeyDialog = true }
                        ) { BoardFlowSettingValue(if (apiKey.isBlank()) "Not set" else "Saved") }
                        BoardFlowFormDivider()
                        BoardFlowSettingRow(
                            icon = Icons.Default.VpnKey,
                            title = "Backup keys",
                            detail = "Used when the main key hits its limit",
                            enabled = apiKey.isNotBlank() || extraKeys.isNotEmpty(),
                            onClick = { showBackupKeysDialog = true }
                        ) { BoardFlowSettingValue(if (extraKeys.isEmpty()) "None" else extraKeys.size.toString()) }
                        BoardFlowFormDivider()
                        BoardFlowSettingRow(
                            icon = Icons.Default.Tune,
                            title = "Model",
                            detail = "Automatic picks the newest Flash model",
                            onClick = { modelPickerOpen = true }
                        ) { BoardFlowSettingValue(modelEndpoint.ifBlank { autoLabel }) }
                        BoardFlowFormDivider()
                        BoardFlowSettingRow(
                            icon = Icons.Default.Refresh,
                            title = "Refresh available models",
                            detail = "Ask Google which models your key can use",
                            enabled = apiKey.isNotBlank() && !modelListLoading,
                            onClick = {
                                modelListLoading = true
                                viewModel.checkAvailableModels { models ->
                                    availableModels = models
                                    modelListLoading = false
                                    messenger.show(
                                        if (models.isEmpty()) "No models found. Check your API key."
                                        else "${models.size} model${if (models.size == 1) "" else "s"} available"
                                    )
                                }
                            }
                        ) {
                            if (modelListLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(Dimens.Icon), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (showKeyDialog) {
                        GeminiKeyDialog(
                            initial = apiKey,
                            onSave = { key ->
                                apiKey = key
                                prefs.geminiApiKey = key
                                showKeyDialog = false
                                messenger.show(if (key.isBlank()) "API key removed" else "API key saved")
                            },
                            onDismiss = { showKeyDialog = false }
                        )
                    }
                    if (showBackupKeysDialog) {
                        BackupKeysDialog(
                            keys = extraKeys,
                            onChange = { updated ->
                                extraKeys = updated
                                prefs.saveGeminiExtraApiKeys(updated)
                            },
                            onDismiss = { showBackupKeysDialog = false }
                        )
                    }
                    if (modelPickerOpen) {
                        BoardFlowPickerSheet(
                            title = "Choose Gemini model",
                            options = listOf(GeminiModels.AUTO) + (availableModels ?: emptyList()),
                            selectedOption = modelEndpoint,
                            optionLabel = { it.ifBlank { "$autoLabel (recommended)" } },
                            onSelect = { model ->
                                modelEndpoint = model
                                prefs.geminiModelEndpoint = model.trim()
                                modelPickerOpen = false
                            },
                            onDismiss = { modelPickerOpen = false }
                        )
                    }
                }

                item { BoardFlowSectionTitle(title = "Learned from scans", supporting = "Built up from the scans you confirm.") }
                item {
                    BoardFlowFormGroup {
                        BoardFlowSettingRow(
                            icon = Icons.Default.DocumentScanner,
                            title = "Recognition templates",
                            detail = "Scoring layouts that help recognise the game",
                            onClick = if (templateCount > 0) ({ showTemplatesDialog = true }) else null
                        ) {
                            BoardFlowSettingValue(
                                if (templateCount == 0) "None" else "$templateCount saved",
                                chevron = templateCount > 0
                            )
                        }
                        BoardFlowFormDivider()
                        BoardFlowSettingRow(
                            icon = Icons.Default.Person,
                            title = "Player recognition hints",
                            detail = "Scanned names matched to your players"
                        ) {
                            BoardFlowSettingValue(if (playerHintCount == 0) "None" else "$playerHintCount saved", chevron = false)
                        }
                        BoardFlowFormDivider()
                        BoardFlowSettingRow(
                            icon = Icons.Default.Delete,
                            title = "Clear recognition templates",
                            detail = "Scans start learning game layouts again",
                            destructive = true,
                            enabled = templateCount > 0,
                            onClick = { showClearTemplatesConfirm = true }
                        )
                        BoardFlowFormDivider()
                        BoardFlowSettingRow(
                            icon = Icons.Default.Delete,
                            title = "Clear player recognition hints",
                            detail = "Scans go back to names and aliases only",
                            destructive = true,
                            enabled = playerHintCount > 0,
                            onClick = { showClearPlayerHintsConfirm = true }
                        )
                    }
                    if (showTemplatesDialog) {
                        RecognitionTemplatesDialog(
                            viewModel = viewModel,
                            onDismiss = { showTemplatesDialog = false },
                            onTemplatesChanged = { newCount -> templateCount = newCount }
                        )
                    }
                }
            }

            if (selectedSection == SettingsSection.DATA) {
                item { BoardFlowSectionTitle(title = "Backup and restore", supporting = "Move everything to a new phone: players, plays, challenges and settings.") }
                item {
                    BoardFlowFormGroup {
                        BoardFlowSettingRow(
                            icon = Icons.Default.Key,
                            title = "Include passwords and API keys",
                            detail = "Adds your BGG password and Gemini keys to the backup",
                            onClick = { includeSensitiveBackup = !includeSensitiveBackup }
                        ) {
                            androidx.compose.material3.Switch(
                                checked = includeSensitiveBackup,
                                onCheckedChange = { includeSensitiveBackup = it }
                            )
                        }
                        BoardFlowFormDivider()
                        BoardFlowSettingRow(
                            icon = Icons.Default.FileUpload,
                            title = "Export data",
                            detail = "Save a backup file",
                            onClick = { exportLauncher.launch("boardflow-backup-${LocalDate.now()}.json") }
                        )
                        BoardFlowFormDivider()
                        BoardFlowSettingRow(
                            icon = Icons.Default.FileDownload,
                            title = "Import data",
                            detail = "Replace this phone's data with a backup",
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                        )
                    }
                }
                item { BoardFlowSectionTitle(title = "Storage") }
                item {
                    BoardFlowFormGroup {
                        BoardFlowSettingRow(
                            icon = Icons.Default.Storage,
                            title = "Clear collection cache",
                            detail = if (hasCollection) "$collectionSize games cached. The next refresh downloads them again."
                                     else "No collection cached",
                            destructive = true,
                            enabled = hasCollection,
                            onClick = { showClearCollectionConfirm = true }
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "BoardFlow",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecognitionTemplatesDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onTemplatesChanged: (Int) -> Unit
) {
    var templates by remember { mutableStateOf(viewModel.getGameRecognitionHints()) }
    var editingHint by remember { mutableStateOf<GameRecognitionHint?>(null) }

    AnimatedDialog(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item { SettingsDialogHeader("Recognition templates", "What a scan of each game looked like. Edit the categories or delete a template.") }
            if (templates.isEmpty()) {
                item { SettingsDialogEmpty("No templates saved.") }
            } else item {
                BoardFlowFormGroup(raised = true) {
                    templates.forEachIndexed { index, hint ->
                        if (index > 0) BoardFlowFormDivider()
                        Column(
                            modifier = Modifier.padding(start = Spacing.lg, end = Spacing.xs, top = Spacing.sm, bottom = Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hint.gameName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        if (hint.timesConfirmed == 1) "Confirmed once" else "Confirmed ${hint.timesConfirmed} times",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                BoardFlowIconButton(
                                    onClick = {
                                        viewModel.deleteGameRecognitionHint(hint.gameObjectId)
                                        templates = viewModel.getGameRecognitionHints()
                                        onTemplatesChanged(templates.size)
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete template", modifier = Modifier.size(Dimens.Icon))
                                }
                                BoardFlowIconButton(onClick = { editingHint = hint }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit template",
                                        modifier = Modifier.size(Dimens.Icon),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (hint.normalizedCategories.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.padding(end = Spacing.md),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    hint.normalizedCategories.forEach { cat -> CategoryTag(cat) }
                                }
                            } else {
                                Text(
                                    "No categories saved",
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

    editingHint?.let { hint ->
        EditTemplateDialog(
            hint = hint,
            onSave = { updated ->
                viewModel.replaceGameRecognitionHint(updated)
                templates = viewModel.getGameRecognitionHints()
                editingHint = null
            },
            onDismiss = { editingHint = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditTemplateDialog(
    hint: GameRecognitionHint,
    onSave: (GameRecognitionHint) -> Unit,
    onDismiss: () -> Unit
) {
    var categories by remember { mutableStateOf(hint.normalizedCategories.toMutableList()) }
    var newCatInput by remember { mutableStateOf("") }

    AnimatedDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                hint.gameName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Scoring categories",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (categories.isEmpty()) {
                Text(
                    "No categories yet. Add one below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        SuggestionChip(
                            onClick = {
                                categories = categories.toMutableList().also { it.remove(cat) }
                            },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            icon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoardFlowTextField(
                    value = newCatInput,
                    onValueChange = { newCatInput = it },
                    label = { Text("Add category") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val normalized = newCatInput.trim()
                            .lowercase()
                            .replace(Regex("[^a-z0-9 ]"), " ")
                            .replace(Regex("\\s+"), " ")
                            .trim()
                        if (normalized.isNotBlank() && !categories.contains(normalized)) {
                            categories = categories.toMutableList().also { it.add(normalized) }
                        }
                        newCatInput = ""
                    },
                    enabled = newCatInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoardFlowInlineAction(onClick = onDismiss, neutral = true, large = true) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                BoardFlowButton(
                    onClick = { onSave(hint.copy(normalizedCategories = categories.toList())) }
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun CustomMoodsDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val moods by viewModel.customMoods.collectAsState()
    var moodPendingDelete by remember { mutableStateOf<String?>(null) }

    moodPendingDelete?.let { mood ->
        BoardFlowConfirmationDialog(
            title = "Delete mood?",
            message = "\"$mood\" will be removed from your templates and from every play that uses it. This cannot be undone.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            kind = BoardFlowConfirmationKind.DESTRUCTIVE,
            onConfirm = {
                viewModel.deleteCustomMood(mood)
                moodPendingDelete = null
            },
            onDismiss = { moodPendingDelete = null }
        )
    }

    AnimatedDialog(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item { SettingsDialogHeader("Mood templates", "Custom moods you've added while logging session memories.") }
            if (moods.isEmpty()) {
                item { SettingsDialogEmpty("No custom moods saved.") }
            } else item {
                BoardFlowFormGroup(raised = true) {
                    moods.forEachIndexed { index, mood ->
                        if (index > 0) BoardFlowFormDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(start = Spacing.lg, end = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                mood,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            BoardFlowIconButton(
                                onClick = { moodPendingDelete = mood },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete $mood", modifier = Modifier.size(Dimens.Icon))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Title and one line of explanation at the top of a Settings dialog. */
@Composable
private fun SettingsDialogHeader(title: String, description: String) {
    Column(
        modifier = Modifier.padding(top = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsDialogEmpty(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = Spacing.md)
    )
}

/** A scoring category saved in a recognition template. Read-only, so grey. */
@Composable
private fun CategoryTag(label: String) {
    Surface(shape = BoardFlowShape.Pill, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Edits the main Gemini key. A blank key turns scanning and chronicles off. */
@Composable
private fun GeminiKeyDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(initial) }
    var visible by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    AnimatedDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SettingsDialogHeader(
                "Gemini API key",
                "Create a key in Google AI Studio (your profile, API keys) and paste it here."
            )
            BoardFlowTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (visible) "Hide key" else "Show key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BoardFlowInlineAction(onClick = { uriHandler.openUri("https://aistudio.google.com") }) {
                    Text("Open AI Studio")
                }
                Spacer(Modifier.weight(1f))
                BoardFlowInlineAction(onClick = onDismiss, neutral = true, large = true) { Text("Cancel") }
                Spacer(Modifier.width(Spacing.sm))
                BoardFlowButton(onClick = { onSave(value.trim()) }) { Text("Save") }
            }
        }
    }
}

/** Backup Gemini keys: the app moves to the next one when a key hits its limit. */
@Composable
private fun BackupKeysDialog(keys: List<String>, onChange: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var newKey by remember { mutableStateOf("") }
    AnimatedDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SettingsDialogHeader(
                "Backup keys",
                "When the main key hits its limit and every model is busy, the app moves to the next key."
            )
            if (keys.isNotEmpty()) {
                BoardFlowFormGroup(raised = true) {
                    keys.forEachIndexed { index, key ->
                        if (index > 0) BoardFlowFormDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(start = Spacing.lg, end = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Key ${index + 1}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(72.dp)
                            )
                            Text(
                                "\u2022\u2022\u2022\u2022 " + key.takeLast(4),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            BoardFlowIconButton(
                                onClick = { onChange(keys.filterIndexed { i, _ -> i != index }) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove key ${index + 1}", modifier = Modifier.size(Dimens.Icon))
                            }
                        }
                    }
                }
            }
            BoardFlowTextField(
                value = newKey,
                onValueChange = { newKey = it },
                label = { Text("Add a backup key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoardFlowInlineAction(onClick = onDismiss, neutral = true, large = true) { Text("Close") }
                Spacer(Modifier.width(Spacing.sm))
                BoardFlowButton(
                    onClick = {
                        onChange(keys + newKey.trim())
                        newKey = ""
                    },
                    enabled = newKey.isNotBlank()
                ) { Text("Add key") }
            }
        }
    }
}
