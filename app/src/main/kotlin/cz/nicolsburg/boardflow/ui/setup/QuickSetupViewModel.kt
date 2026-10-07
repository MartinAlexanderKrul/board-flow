package cz.nicolsburg.boardflow.ui.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.net.Uri
import cz.nicolsburg.boardflow.data.setupguide.GuideDraftService
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideJson
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideRepository
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideResolver
import cz.nicolsburg.boardflow.model.LoadedSetupGuide
import cz.nicolsburg.boardflow.model.ResolvedSetup
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface QuickSetupUiState {
    data object Loading : QuickSetupUiState
    data object NotFound : QuickSetupUiState
    data class Ready(
        val loaded: LoadedSetupGuide,
        val playerCounts: List<Int>,
        val selectablePlayerCounts: List<Int>,
        val selectedModules: Set<String>,
        val lockedModules: Set<String>,
        val resolved: ResolvedSetup,
        val checkedStepIds: Set<String>
    ) : QuickSetupUiState {
        val totalSteps: Int get() = resolved.checkableStepIds.size
        val doneSteps: Int get() = resolved.checkableStepIds.count { it in checkedStepIds }
        val isComplete: Boolean get() = totalSteps > 0 && doneSteps == totalSteps
    }
}

/**
 * Holds one table-setup session. The checklist, player count and module choice live only in this
 * nav-scoped ViewModel's [SavedStateHandle]: they survive rotation and process death while the
 * phone sits on the table, but are never written back to the guide or to persistent storage.
 */
class QuickSetupViewModel(
    private val savedState: SavedStateHandle,
    private val repository: SetupGuideRepository,
    private val isOnline: () -> Boolean,
    private val draftService: GuideDraftService? = null
) : ViewModel() {

    private val requestedGameId: Int = savedState.get<Int>(ARG_GAME_ID) ?: 0
    val gameId: Int get() = requestedGameId

    private val guide = MutableStateFlow<LoadedSetupGuide?>(null)
    private val loadFinished = MutableStateFlow(false)
    private val playerCount = savedState.getStateFlow(KEY_PLAYERS, 0)
    private val modules = savedState.getStateFlow(KEY_MODULES, arrayListOf<String>())
    private val checked = savedState.getStateFlow(KEY_CHECKED, arrayListOf<String>())

    val uiState: StateFlow<QuickSetupUiState> =
        combine(guide, loadFinished, playerCount, modules, checked) { loaded, finished, players, selected, done ->
            when {
                loaded == null && !finished -> QuickSetupUiState.Loading
                loaded == null -> QuickSetupUiState.NotFound
                players == 0 -> QuickSetupUiState.Loading
                else -> {
                    val g = loaded.guide
                    val selection = selected.toSet()
                    val resolved = SetupGuideResolver.resolve(g, players, selection)
                    QuickSetupUiState.Ready(
                        loaded = loaded,
                        playerCounts = SetupGuideResolver.allPlayerCounts(g),
                        selectablePlayerCounts = SetupGuideResolver.selectablePlayerCounts(g, resolved.enabledModules),
                        selectedModules = resolved.enabledModules,
                        lockedModules = SetupGuideResolver.lockedModules(g, players, resolved.enabledModules),
                        resolved = resolved,
                        checkedStepIds = done.toSet()
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickSetupUiState.Loading)

    init {
        viewModelScope.launch {
            val loaded = repository.loadGuide(requestedGameId, isOnline())
            // getStateFlow seeds the key with 0, so 0 (not key presence) means "not initialised yet".
            if (loaded != null && playerCount.value == 0) initSelection(loaded)
            guide.value = loaded
            loadFinished.value = true
        }
        // An edit saved from the guide editor (or an import) changes what this screen shows.
        viewModelScope.launch {
            var first = true
            repository.userGuideChanges.collect {
                if (first) first = false else reloadKeepingSelection()
            }
        }
    }

    private val _drafting = MutableStateFlow(false)
    /** True while a draft for this game runs (in the background; it survives leaving the screen). */
    val drafting: StateFlow<Boolean> = _drafting.asStateFlow()

    private val _draftFailures = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** A draft that failed while this screen was open; shown once. */
    val draftFailures: SharedFlow<String> = _draftFailures.asSharedFlow()

    init {
        draftService?.let { service ->
            viewModelScope.launch {
                var sawRunning = false
                service.state(requestedGameId).collect { state ->
                    _drafting.value = state is GuideDraftService.DraftState.Running
                    if (state is GuideDraftService.DraftState.Running) {
                        sawRunning = true
                    } else {
                        // Only an outcome seen happen here is reported; an old failure is not.
                        if (sawRunning && state is GuideDraftService.DraftState.Failed) _draftFailures.tryEmit(state.message)
                        // The worker saved through the shared repository, so the guide shows by itself.
                        sawRunning = false
                    }
                }
            }
        }
    }

    /** Whether a standard guide exists for this game (it may just not be downloaded yet). */
    val hasStandardGuide: Boolean get() = requestedGameId in repository.availability.value

    val canDraft: Boolean get() = draftService?.hasGeminiKey() == true

    /**
     * Starts drafting a guide for this game from a rulebook PDF, in the background: the user can
     * leave and gets a notification. [onResult] gets null once it is queued, or a message.
     */
    fun draftFromRulebook(pdf: Uri, gameName: String, onResult: (String?) -> Unit) {
        val service = draftService ?: return
        if (_drafting.value) return
        if (!isOnline()) return onResult("Drafting a guide needs a connection")
        viewModelScope.launch {
            val problem = service.start(pdf, requestedGameId, gameName)
            if (problem == null) _drafting.value = true
            onResult(problem)
        }
    }

    /** Marks an AI draft as checked against the rulebook. */
    fun markReviewed() {
        val current = guide.value ?: return
        viewModelScope.launch { repository.markReviewed(current.guide.gameId) }
    }

    private suspend fun reloadKeepingSelection() {
        val loaded = repository.loadGuide(requestedGameId, isOnline()) ?: return
        val g = loaded.guide
        val players = playerCount.value.takeIf { it > 0 } ?: return run { initSelection(loaded); guide.value = loaded }
        val known = g.modules.map { it.id }.toSet()
        val selected = modules.value.filter { it in known }.toSet()
        savedState[KEY_MODULES] = ArrayList(SetupGuideResolver.effectiveModules(g, players, selected))
        guide.value = loaded
    }

    private fun initSelection(loaded: LoadedSetupGuide) {
        val g = loaded.guide
        // Opening an expansion's detail page preselects that expansion's module.
        val openedModule = g.modules.firstOrNull { it.bggId == requestedGameId && requestedGameId != g.gameId }
        val selected = (g.modules.filter { it.defaultEnabled }.map { it.id } + listOfNotNull(openedModule?.id)).toSet()
        val range = SetupGuideResolver.selectablePlayerCounts(g, selected)
        val players = SetupGuideResolver.nearestPlayerCount(range, DEFAULT_PLAYERS)
        savedState[KEY_MODULES] = ArrayList(SetupGuideResolver.effectiveModules(g, players, selected))
        savedState[KEY_PLAYERS] = players
    }

    fun selectPlayerCount(count: Int) {
        val g = guide.value?.guide ?: return
        savedState[KEY_PLAYERS] = count
        savedState[KEY_MODULES] = ArrayList(SetupGuideResolver.effectiveModules(g, count, modules.value.toSet()))
    }

    fun toggleModule(moduleId: String) {
        val g = guide.value?.guide ?: return
        val players = playerCount.value
        val enabledNow = SetupGuideResolver.effectiveModules(g, players, modules.value.toSet())
        if (moduleId in SetupGuideResolver.lockedModules(g, players, enabledNow)) return
        val current = modules.value.toSet()
        val module = g.modules.first { it.id == moduleId }
        val next = if (module.group != null) {
            // Single-choice group: picking one replaces its siblings, and drops what needed them
            // (or the requirement would switch the old sibling back on). Tapping the active one does nothing.
            if (moduleId in current) return
            val siblings = g.modules.filter { it.group == module.group && it.id != moduleId }.map { it.id }.toSet()
            current - SetupGuideResolver.withDependents(g, siblings) + moduleId
        } else if (moduleId in current) {
            // Switching a module off also drops modules that depend on it.
            current - SetupGuideResolver.withDependents(g, setOf(moduleId))
        } else {
            current - module.excludes.toSet() - g.modules.filter { moduleId in it.excludes }.map { it.id }.toSet() + moduleId
        }
        val range = SetupGuideResolver.selectablePlayerCounts(g, next)
        val clamped = if (players in range) players else SetupGuideResolver.nearestPlayerCount(range, players)
        savedState[KEY_MODULES] = ArrayList(SetupGuideResolver.effectiveModules(g, clamped, next))
        if (clamped != players) savedState[KEY_PLAYERS] = clamped
    }

    fun toggleStep(stepId: String) {
        val current = checked.value
        savedState[KEY_CHECKED] = ArrayList(if (stepId in current) current - stepId else current + stepId)
    }

    fun resetChecklist() {
        savedState[KEY_CHECKED] = arrayListOf<String>()
    }

    /** The guide on screen as a shareable JSON document (the same format as the catalog). */
    fun exportJson(): String? = guide.value?.guide?.let(SetupGuideJson::toJsonString)

    /** Removes the user's own version and shows the standard guide again. */
    fun useStandardGuide() {
        val current = guide.value ?: return
        viewModelScope.launch {
            repository.deleteUserGuide(current.guide.gameId)
            reload()
        }
    }

    /** Keeps the user's version after the standard guide was updated; hides the update note. */
    fun keepUserGuide() {
        val current = guide.value ?: return
        viewModelScope.launch {
            repository.keepUserGuide(current.guide.gameId)
            guide.value = current.copy(basedOnVersion = current.upstreamVersion)
        }
    }

    private suspend fun reload() {
        val loaded = repository.loadGuide(requestedGameId, isOnline())
        // Module ids can differ between versions: start the selection again, keep the ticks
        // that still match a visible step.
        if (loaded != null) initSelection(loaded)
        guide.value = loaded
    }

    companion object {
        const val ARG_GAME_ID = "gameId"
        /** Every guide opens at 2 players (clamped to the guide's range). */
        private const val DEFAULT_PLAYERS = 2
        private const val KEY_PLAYERS = "qs_players"
        private const val KEY_MODULES = "qs_modules"
        private const val KEY_CHECKED = "qs_checked"

        fun factory(
            repository: SetupGuideRepository,
            isOnline: () -> Boolean,
            draftService: GuideDraftService? = null
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { QuickSetupViewModel(createSavedStateHandle(), repository, isOnline, draftService) }
            }
    }
}
