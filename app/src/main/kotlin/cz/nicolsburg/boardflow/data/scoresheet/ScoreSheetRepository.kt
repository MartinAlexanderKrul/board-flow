package cz.nicolsburg.boardflow.data.scoresheet

import cz.nicolsburg.boardflow.data.CanonicalCollectionStore
import cz.nicolsburg.boardflow.data.StoredNoScoreSheet
import cz.nicolsburg.boardflow.data.StoredScoreSheet
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.SheetOrigin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Every score sheet the app knows, in the order they win for a game: the user's own (AI drafts and
 * imports, in Room as JSON), the catalog's (shared sheets and fixes from the repo's `score-sheets/`,
 * see [ScoreSheetCatalog]) and the built-in ones in [ScoreSheets]. One per process (in
 * `AppContainer`), so a sheet a background draft saves shows on an open form. A sheet the user
 * marks as reviewed goes to [onReviewed], which shares it as a pull request.
 */
class ScoreSheetRepository(
    private val store: CanonicalCollectionStore,
    private val catalog: ScoreSheetCatalog? = null,
    private val onReviewed: (ScoreSheet) -> Unit = {}
) {
    private val _userSheets = MutableStateFlow<List<ScoreSheet>>(emptyList())
    val userSheets: StateFlow<List<ScoreSheet>> = _userSheets.asStateFlow()
    private val _catalogSheets = MutableStateFlow<List<ScoreSheet>>(emptyList())
    /** The catalog's sheets; a form showing a game follows them like [userSheets]. */
    val catalogSheets: StateFlow<List<ScoreSheet>> = _catalogSheets.asStateFlow()
    private val _noSheetMarks = MutableStateFlow<Map<Int, StoredNoScoreSheet>>(emptyMap())
    /** Games the user or an AI draft marked as needing no sheet; built-in ones are [NoScoreSheetGames]. */
    val noSheetMarks: StateFlow<Map<Int, StoredNoScoreSheet>> = _noSheetMarks.asStateFlow()
    private val lock = Mutex()
    private var loaded = false

    suspend fun load() = lock.withLock {
        if (loaded) return@withLock
        _userSheets.value = store.getUserScoreSheets().mapNotNull { stored ->
            runCatching { ScoreSheetJson.parse(stored.sheetJson) }.getOrNull()
        }
        _noSheetMarks.value = store.getNoScoreSheetMarks().associateBy { it.gameId }
        _catalogSheets.value = catalog?.sheets().orEmpty()
        loaded = true
    }

    /** Downloads new and updated catalog sheets (at most once a day unless [force]). */
    /** Returns how many sheets were new or updated. */
    suspend fun refreshCatalog(isOnline: Boolean, force: Boolean = false): Int {
        val catalog = catalog ?: return 0
        if (!isOnline) return 0
        load()
        val changed = catalog.refreshIfStale(force)
        if (changed > 0) _catalogSheets.value = catalog.sheets()
        return changed
    }

    /** Total sheets the app knows (user's own, catalog, built-in), for a Settings summary. */
    fun counts(): Int = (ScoreSheets.all.map { it.id } + _catalogSheets.value.map { it.id } + _userSheets.value.map { it.id }).distinct().size

    /** Why [gameId] needs no score sheet, or null when it may get one (drafted by AI). */
    fun noSheetReason(gameId: Int): String? =
        NoScoreSheetGames.reasonFor(gameId)?.message ?: _noSheetMarks.value[gameId]?.reason

    /** Remembers that [gameId] needs no sheet; [source] is [SOURCE_AI] or [SOURCE_USER]. */
    suspend fun markNoSheet(gameId: Int, reason: String, source: String) {
        load()
        val mark = StoredNoScoreSheet(gameId, reason, source, System.currentTimeMillis())
        store.saveNoScoreSheetMark(mark)
        lock.withLock { _noSheetMarks.value = _noSheetMarks.value + (gameId to mark) }
    }

    suspend fun clearNoSheet(gameId: Int) {
        load()
        store.deleteNoScoreSheetMark(gameId)
        lock.withLock { _noSheetMarks.value = _noSheetMarks.value - gameId }
    }

    /** The marks as JSON objects, for backups. */
    suspend fun exportNoSheetMarks(): List<String> {
        load()
        return _noSheetMarks.value.values.map {
            org.json.JSONObject().put("gameId", it.gameId).put("reason", it.reason).put("source", it.source).toString()
        }
    }

    /** Restores marks from a backup, adding or replacing them by game. */
    suspend fun importNoSheetMarks(documents: List<String>) {
        documents.forEach { text ->
            val json = runCatching { org.json.JSONObject(text) }.getOrNull() ?: return@forEach
            val gameId = json.optInt("gameId", 0).takeIf { it > 0 } ?: return@forEach
            markNoSheet(gameId, json.optString("reason"), json.optString("source", SOURCE_USER))
        }
    }

    /** The sheet for the first id that has one: the game first, then its logged expansions. */
    fun forGames(gameIds: List<Int>): ScoreSheet? = gameIds.firstNotNullOfOrNull { id ->
        _userSheets.value.firstOrNull { id in it.gameIds }
            ?: _catalogSheets.value.firstOrNull { id in it.gameIds }
            ?: ScoreSheets.all.firstOrNull { id in it.gameIds }
    }

    fun byId(id: String): ScoreSheet? =
        _userSheets.value.firstOrNull { it.id == id } ?: _catalogSheets.value.firstOrNull { it.id == id } ?: ScoreSheets.byId(id)

    /** Stores the user's own sheet for [ScoreSheet.gameIds]'s first game. Returns the validator's problems; empty when saved. */
    suspend fun save(sheet: ScoreSheet): List<String> {
        val problems = ScoreSheetValidator.validate(sheet)
        if (problems.isNotEmpty()) return problems
        load()
        val gameId = sheet.gameIds.first()
        store.saveUserScoreSheet(
            StoredScoreSheet(sheet.id, gameId, ScoreSheetJson.write(sheet).toString(), System.currentTimeMillis())
        )
        lock.withLock { _userSheets.value = _userSheets.value.filterNot { it.id == sheet.id } + sheet }
        return emptyList()
    }

    /** The user checked the sheet against the rulebook: it is trusted now, and shared with everyone. */
    suspend fun markReviewed(id: String) {
        val sheet = _userSheets.value.firstOrNull { it.id == id } ?: return
        val reviewed = sheet.copy(reviewed = true)
        if (save(reviewed).isEmpty()) onReviewed(reviewed)
    }

    suspend fun delete(id: String) {
        load()
        store.deleteUserScoreSheet(id)
        lock.withLock { _userSheets.value = _userSheets.value.filterNot { it.id == id } }
    }

    /** The user's sheets as JSON documents, for backups. */
    suspend fun exportJson(): List<String> {
        load()
        return _userSheets.value.map { ScoreSheetJson.write(it).toString() }
    }

    /** Restores sheets from a backup, adding or replacing them by id; returns how many were valid. */
    suspend fun importJson(documents: List<String>): Int = documents.count { text ->
        val sheet = runCatching { ScoreSheetJson.parse(text) }.getOrNull() ?: return@count false
        val own = sheet.origin == SheetOrigin.BUILT_IN || sheet.origin == SheetOrigin.COMMUNITY
        save(if (own) sheet.copy(origin = SheetOrigin.USER) else sheet).isEmpty()
    }

    companion object {
        /** The id of the user's sheet for a game. */
        fun userSheetId(gameId: Int) = "user-$gameId"

        const val SOURCE_AI = "AI"
        const val SOURCE_USER = "USER"
    }
}
