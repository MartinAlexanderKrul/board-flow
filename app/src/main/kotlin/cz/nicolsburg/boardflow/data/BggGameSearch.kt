package cz.nicolsburg.boardflow.data

import cz.nicolsburg.boardflow.BuildConfig
import cz.nicolsburg.boardflow.model.BggGame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The one BoardGameGeek game search every game search in the app uses. Searches only look in
 * the collection by themselves; BGG is asked only when the user taps "Search BoardGameGeek"
 * (`SearchBggRow`), and the results show in `BggSearchSheet`. Each screen's ViewModel owns an
 * instance on its own scope.
 */
class BggGameSearch(
    private val repository: BggRepository,
    private val scope: CoroutineScope,
    private val token: String = BuildConfig.BGG_XML_API_TOKEN
) {
    data class State(
        val query: String = "",
        val loading: Boolean = false,
        val results: List<BggGame> = emptyList(),
        /** The result being prepared (e.g. details fetched) before the screen opens it. */
        val openingGameId: Int? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var job: Job? = null

    /**
     * Exact title matches first, then the rest by name, at most [MAX_RESULTS]. Says "Could not
     * reach BoardGameGeek" when both requests fail, and "Nothing found" when BGG answered empty.
     */
    fun search(query: String) {
        val q = query.trim()
        if (q.length < MIN_QUERY) return
        job?.cancel()
        _state.value = State(query = q, loading = true)
        job = scope.launch {
            val exactResult = repository.searchGames(q, token, exact = true)
            val looseResult = repository.searchGames(q, token, exact = false)
            val results = merge(exactResult.getOrDefault(emptyList()), looseResult.getOrDefault(emptyList()))
            val error = when {
                results.isNotEmpty() -> null
                exactResult.isFailure && looseResult.isFailure -> "Could not reach BoardGameGeek. Check the connection and try again."
                else -> "Nothing found on BoardGameGeek for \"$q\""
            }
            _state.value = State(query = q, results = results, error = error)
        }
    }

    fun setOpening(gameId: Int?) {
        _state.value = _state.value.copy(openingGameId = gameId)
    }

    fun clear() {
        job?.cancel()
        _state.value = State()
    }

    companion object {
        /** Shortest query that offers a BoardGameGeek search. */
        const val MIN_QUERY = 2
        const val MAX_RESULTS = 50

        fun merge(exact: List<BggGame>, loose: List<BggGame>): List<BggGame> =
            (exact + loose.sortedBy { it.name.lowercase() }).distinctBy { it.id }.take(MAX_RESULTS)
    }
}
