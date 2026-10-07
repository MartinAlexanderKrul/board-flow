package cz.nicolsburg.boardflow.ui.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cz.nicolsburg.boardflow.data.setupguide.GuideEdits
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideJson
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideRepository
import cz.nicolsburg.boardflow.model.SetupGuide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GuideEditorUiState {
    data object Loading : GuideEditorUiState
    data object NotFound : GuideEditorUiState
    data class Editing(val guide: SetupGuide, val dirty: Boolean) : GuideEditorUiState
}

/**
 * Edits one guide in place. The draft lives in [SavedStateHandle] as the guide's JSON, so an edit
 * in progress survives rotation and process death; nothing is written until [save]. Saving
 * stores the result as the user's own version of the guide (the standard one stays untouched).
 */
class GuideEditorViewModel(
    private val savedState: SavedStateHandle,
    private val repository: SetupGuideRepository,
    private val isOnline: () -> Boolean
) : ViewModel() {

    private val gameId: Int = savedState.get<Int>(ARG_GAME_ID) ?: 0
    private val loadFinished = MutableStateFlow(savedState.get<String>(KEY_DRAFT) != null)
    private val draftJson = savedState.getStateFlow<String?>(KEY_DRAFT, null)

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    val uiState: StateFlow<GuideEditorUiState> = draftJson.map { json ->
        val guide = json?.let(SetupGuideJson::parseOrNull)
        when {
            guide != null -> GuideEditorUiState.Editing(guide, dirty = json != savedState.get<String>(KEY_ORIGINAL))
            !loadFinished.value -> GuideEditorUiState.Loading
            else -> GuideEditorUiState.NotFound
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GuideEditorUiState.Loading)

    init {
        if (savedState.get<String>(KEY_DRAFT) == null) {
            viewModelScope.launch {
                val loaded = repository.loadGuide(gameId, isOnline())
                loadFinished.value = true
                val json = loaded?.guide?.let(SetupGuideJson::toJsonString)
                savedState[KEY_ORIGINAL] = json
                // Setting the draft last makes the state recompute with loadFinished already true.
                savedState[KEY_DRAFT] = json ?: ""
            }
        }
    }

    private val current: SetupGuide? get() = draftJson.value?.let(SetupGuideJson::parseOrNull)

    private fun update(transform: (SetupGuide) -> SetupGuide) {
        val guide = current ?: return
        savedState[KEY_DRAFT] = SetupGuideJson.toJsonString(transform(guide))
    }

    fun setSectionTitle(sectionId: String, title: String) = update { GuideEdits.setSectionTitle(it, sectionId, title) }
    fun setStepText(sectionId: String, stepId: String, text: String) = update { GuideEdits.setStepText(it, sectionId, stepId, text) }
    fun setStepNote(sectionId: String, stepId: String, note: String) = update { GuideEdits.setStepNote(it, sectionId, stepId, note) }
    fun addStep(sectionId: String) = update { GuideEdits.addStep(it, sectionId).first }
    fun deleteStep(sectionId: String, stepId: String) = update { GuideEdits.deleteStep(it, sectionId, stepId) }
    fun moveStep(sectionId: String, stepId: String, delta: Int) = update { GuideEdits.moveStep(it, sectionId, stepId, delta) }
    fun addSection() = update { GuideEdits.addSection(it).first }
    fun deleteSection(sectionId: String) = update { GuideEdits.deleteSection(it, sectionId) }

    // Quantities and when a step (or, with stepId null, a section) shows.
    fun setAmountValue(sectionId: String, stepId: String, name: String, players: Int?, value: String) =
        update { GuideEdits.setAmountValue(it, sectionId, stepId, name, players, value) }
    fun setCaseValue(sectionId: String, stepId: String, name: String, caseIndex: Int, value: String) =
        update { GuideEdits.setCaseValue(it, sectionId, stepId, name, caseIndex, value) }
    fun deleteCase(sectionId: String, stepId: String, name: String, caseIndex: Int) =
        update { GuideEdits.deleteCase(it, sectionId, stepId, name, caseIndex) }
    fun setShownAtCounts(sectionId: String, stepId: String?, counts: Set<Int>, allCounts: List<Int>) =
        update { GuideEdits.setShownAtCounts(it, sectionId, stepId, counts, allCounts) }
    fun setModuleRule(sectionId: String, stepId: String?, moduleId: String, rule: GuideEdits.ModuleRule) =
        update { GuideEdits.setModuleRule(it, sectionId, stepId, moduleId, rule) }

    /**
     * Saves the draft as the user's version. Empty steps and sections are dropped first.
     * [onResult] gets null on success, or the first problem the validator found.
     */
    fun save(onResult: (problem: String?) -> Unit) {
        val guide = current ?: return
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            val problems = repository.saveEditedGuide(GuideEdits.tidy(guide))
            _saving.value = false
            if (problems.isEmpty()) savedState[KEY_ORIGINAL] = draftJson.value
            onResult(problems.firstOrNull())
        }
    }

    companion object {
        const val ARG_GAME_ID = "gameId"
        private const val KEY_DRAFT = "ge_draft"
        private const val KEY_ORIGINAL = "ge_original"

        fun factory(repository: SetupGuideRepository, isOnline: () -> Boolean): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { GuideEditorViewModel(createSavedStateHandle(), repository, isOnline) }
            }
    }
}
