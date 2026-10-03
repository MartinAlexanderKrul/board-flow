package cz.nicolsburg.boardflow.ui.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideJson
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideRepository
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideResolver
import cz.nicolsburg.boardflow.model.LoadedSetupGuide
import cz.nicolsburg.boardflow.model.ResolvedSetup
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val isOnline: () -> Boolean
) : ViewModel() {

    private val requestedGameId: Int = savedState.get<Int>(ARG_GAME_ID) ?: 0

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
                        lockedModules = SetupGuideResolver.lockedModules(g, players),
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
        if (moduleId in SetupGuideResolver.lockedModules(g, players)) return
        val current = modules.value.toSet()
        val module = g.modules.first { it.id == moduleId }
        val next = if (module.group != null) {
            // Single-choice group: picking one replaces its siblings; tapping the active one does nothing.
            if (moduleId in current) return
            current - g.modules.filter { it.group == module.group }.map { it.id }.toSet() + moduleId
        } else if (moduleId in current) {
            // Switching a module off also drops modules that depend on it.
            val dependents = g.modules.filter { moduleId in it.requires }.map { it.id }.toSet()
            current - moduleId - dependents
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

        fun factory(repository: SetupGuideRepository, isOnline: () -> Boolean): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { QuickSetupViewModel(createSavedStateHandle(), repository, isOnline) }
            }
    }
}
