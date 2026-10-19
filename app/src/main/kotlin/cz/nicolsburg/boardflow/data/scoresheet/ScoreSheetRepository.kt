package cz.nicolsburg.boardflow.data.scoresheet

import cz.nicolsburg.boardflow.data.CanonicalCollectionStore
import cz.nicolsburg.boardflow.data.StoredScoreSheet
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.SheetOrigin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Every score sheet the app knows: the user's own (AI drafts and imports, in Room as JSON) and the
 * built-in ones in [ScoreSheets]. For a game the user's sheet wins, like a user's Quick Setup guide.
 * One per process (in `AppContainer`), so a sheet a background draft saves shows on an open form.
 */
class ScoreSheetRepository(private val store: CanonicalCollectionStore) {
    private val _userSheets = MutableStateFlow<List<ScoreSheet>>(emptyList())
    val userSheets: StateFlow<List<ScoreSheet>> = _userSheets.asStateFlow()
    private val lock = Mutex()
    private var loaded = false

    suspend fun load() = lock.withLock {
        if (loaded) return@withLock
        _userSheets.value = store.getUserScoreSheets().mapNotNull { stored ->
            runCatching { ScoreSheetJson.parse(stored.sheetJson) }.getOrNull()
        }
        loaded = true
    }

    /** The sheet for the first id that has one: the game first, then its logged expansions. */
    fun forGames(gameIds: List<Int>): ScoreSheet? = gameIds.firstNotNullOfOrNull { id ->
        _userSheets.value.firstOrNull { id in it.gameIds } ?: ScoreSheets.all.firstOrNull { id in it.gameIds }
    }

    fun byId(id: String): ScoreSheet? = _userSheets.value.firstOrNull { it.id == id } ?: ScoreSheets.byId(id)

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

    suspend fun markReviewed(id: String) {
        val sheet = _userSheets.value.firstOrNull { it.id == id } ?: return
        save(sheet.copy(reviewed = true))
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
        save(if (sheet.origin == SheetOrigin.BUILT_IN) sheet.copy(origin = SheetOrigin.USER) else sheet).isEmpty()
    }

    companion object {
        /** The id of the user's sheet for a game. */
        fun userSheetId(gameId: Int) = "user-$gameId"
    }
}
