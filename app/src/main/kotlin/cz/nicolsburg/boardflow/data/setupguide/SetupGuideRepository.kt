package cz.nicolsburg.boardflow.data.setupguide

import android.util.Log
import cz.nicolsburg.boardflow.data.CanonicalCollectionStore
import cz.nicolsburg.boardflow.model.GuideOrigin
import cz.nicolsburg.boardflow.model.LoadedSetupGuide
import cz.nicolsburg.boardflow.model.SetupGuide
import cz.nicolsburg.boardflow.model.SetupGuideSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Whether a guide exists for a BGG id, and whether it can be opened without a connection. */
data class SetupGuideAvailability(
    val baseGameId: Int,
    val offlineReady: Boolean
)

/** How many guides the app knows, and how many are downloaded or the user's own (Settings). */
data class SetupGuideCounts(val total: Int, val downloaded: Int, val yours: Int)

/** Outcome of importing a guide file. */
sealed interface GuideImportResult {
    data class Imported(val gameName: String, val replacedYours: Boolean) : GuideImportResult
    data class Invalid(val reason: String) : GuideImportResult
}

/** One guide as listed in the "All guides" view: its base game. */
data class SetupGuideSummary(
    val gameId: Int,
    val gameName: String
)

/**
 * Combines the three guide layers:
 * - USER guides in Room always win (imported or restored copies; never overwritten by upstream),
 * - otherwise the higher `version` of the BUNDLED asset and the downloaded CATALOG copy.
 *
 * Every document is parsed and validated before it is shown or stored; anything invalid is
 * treated as "no guide" rather than surfacing an error.
 */
class SetupGuideRepository(
    private val store: CanonicalCollectionStore,
    private val bundled: BundledSetupGuideSource,
    private val catalog: SetupGuideCatalogClient
) {
    private val mutex = Mutex()
    private val _availability = MutableStateFlow<Map<Int, SetupGuideAvailability>>(emptyMap())

    /** Keyed by every BGG id that opens a guide (base, aliases, expansion modules). */
    val availability: StateFlow<Map<Int, SetupGuideAvailability>> = _availability.asStateFlow()

    private val _guides = MutableStateFlow<List<SetupGuideSummary>>(emptyList())

    private val _userGuideChanges = MutableStateFlow(0)

    /** Bumps whenever a user guide is saved, imported, restored or removed; open guides reload on it. */
    val userGuideChanges: StateFlow<Int> = _userGuideChanges.asStateFlow()

    /** Every known guide (bundled, catalog, user), one per base game, sorted by name. */
    val guides: StateFlow<List<SetupGuideSummary>> = _guides.asStateFlow()

    suspend fun refreshAvailability() {
        val bundledIndex = bundled.index().filter { it.isSupported() }
        val catalogIndex = store.getSetupGuideCatalog().filter { it.isSupported() }
        val stored = store.getSetupGuides()
        val offlineBaseIds = bundledIndex.map { it.gameId }.toSet() + stored.map { it.gameId }

        val result = mutableMapOf<Int, SetupGuideAvailability>()
        val names = linkedMapOf<Int, String>()
        fun register(baseId: Int, ids: Set<Int>) {
            val availability = SetupGuideAvailability(baseId, offlineReady = baseId in offlineBaseIds)
            // A base game's own id always maps to itself, even if another guide lists it as a module.
            ids.forEach { id -> if (id == baseId || id !in result) result[id] = availability }
        }
        (bundledIndex + catalogIndex).forEach {
            register(it.gameId, it.gameIds)
            names.putIfAbsent(it.gameId, it.gameName)
        }
        stored.forEach { row ->
            SetupGuideJson.parseOrNull(row.guideJson)?.let {
                register(it.gameId, it.allGameIds)
                names.putIfAbsent(it.gameId, it.gameName)
            }
        }
        _availability.value = result
        _guides.value = names.map { (id, name) -> SetupGuideSummary(id, name) }
            .sortedBy { it.gameName.lowercase() }
    }

    /**
     * Loads the guide for any id that maps to one. Downloads a newer catalog copy first when
     * online; falls back to whatever is local when offline or the download fails.
     */
    suspend fun loadGuide(anyGameId: Int, isOnline: Boolean): LoadedSetupGuide? {
        val baseId = _availability.value[anyGameId]?.baseGameId
            ?: run { refreshAvailability(); _availability.value[anyGameId]?.baseGameId }
            ?: return null

        store.getSetupGuide(baseId, SetupGuideSource.USER)?.let { row ->
            parseValid(row.guideJson, baseId)?.let { guide ->
                return LoadedSetupGuide(
                    guide = guide,
                    source = SetupGuideSource.USER,
                    upstreamVersion = upstreamVersion(baseId),
                    basedOnVersion = row.basedOnVersion
                )
            }
        }

        if (isOnline) downloadIfNewer(baseId)

        val bundledGuide = bundled.index().firstOrNull { it.gameId == baseId && it.isSupported() }
            ?.let { bundled.guideJson(it) }
            ?.let { parseValid(it, baseId) }
        val catalogGuide = store.getSetupGuide(baseId, SetupGuideSource.CATALOG)
            ?.let { parseValid(it.guideJson, baseId) }

        return when {
            catalogGuide != null && (bundledGuide == null || catalogGuide.version > bundledGuide.version) ->
                LoadedSetupGuide(catalogGuide, SetupGuideSource.CATALOG)
            bundledGuide != null -> LoadedSetupGuide(bundledGuide, SetupGuideSource.BUNDLED)
            else -> null
        }
    }

    /**
     * Refreshes the cached remote index at most once per [CATALOG_MAX_AGE_MS] unless [force]d,
     * then downloads every catalog guide that is newer than the local copy, so guides published
     * after this APK was built work offline too. Guides are small (a few KB each), and only
     * guides whose catalog version beats the bundled or downloaded one are fetched; a failed
     * download is retried on the next call.
     */
    suspend fun refreshCatalogIfStale(isOnline: Boolean, force: Boolean = false): Int {
        var downloaded = 0
        if (isOnline) {
            mutex.withLock {
                val updatedAt = store.getSetupGuideCatalogUpdatedAt() ?: 0L
                if (force || System.currentTimeMillis() - updatedAt > CATALOG_MAX_AGE_MS) {
                    catalog.fetchIndex()?.let { store.replaceSetupGuideCatalog(it) }
                }
            }
            downloaded = store.getSetupGuideCatalog()
                .filter { it.isSupported() }
                .count { downloadIfNewer(it.gameId) }
        }
        refreshAvailability()
        return downloaded
    }

    suspend fun counts(): SetupGuideCounts {
        val stored = store.getSetupGuides()
        return SetupGuideCounts(
            total = _guides.value.size,
            downloaded = stored.count { it.source == SetupGuideSource.CATALOG },
            yours = stored.count { it.source == SetupGuideSource.USER }
        )
    }

    /**
     * Saves a guide file as the user's own version of that game's guide. It wins over the
     * bundled and downloaded copies and is never overwritten by catalog updates; when the
     * standard guide gets a newer version, Quick Setup offers to switch back.
     */
    suspend fun importUserGuide(json: String): GuideImportResult {
        val guide = SetupGuideJson.parseOrNull(json)
            ?: return GuideImportResult.Invalid("This file is not a BoardFlow setup guide")
        val problems = SetupGuideValidator.validate(guide)
        if (problems.isNotEmpty()) return GuideImportResult.Invalid(problems.first())
        val replaced = store.getSetupGuide(guide.gameId, SetupGuideSource.USER) != null
        saveUserGuide(guide, basedOnVersion = upstreamVersion(guide.gameId))
        refreshAvailability()
        _userGuideChanges.value++
        return GuideImportResult.Imported(guide.gameName, replaced)
    }

    /**
     * Saves a guide edited in the app as the user's own version. Returns the validator's problems
     * (nothing is saved then), or an empty list on success. Keeps the standard version an
     * existing user copy was based on, so a pending "updated guide" note does not disappear.
     */
    suspend fun saveEditedGuide(edited: SetupGuide): List<String> {
        val guide = edited.copy(
            provenance = edited.provenance.copy(origin = GuideOrigin.USER, reviewed = true)
        )
        val problems = SetupGuideValidator.validate(guide)
        if (problems.isNotEmpty()) return problems
        val existing = store.getSetupGuide(guide.gameId, SetupGuideSource.USER)
        saveUserGuide(guide, basedOnVersion = existing?.basedOnVersion ?: upstreamVersion(guide.gameId))
        refreshAvailability()
        _userGuideChanges.value++
        return emptyList()
    }

    /** Removes the user's own version; the bundled or downloaded guide shows again. */
    suspend fun deleteUserGuide(gameId: Int) {
        store.deleteSetupGuide(gameId, SetupGuideSource.USER)
        refreshAvailability()
        _userGuideChanges.value++
    }

    /** Keeps the user's version after an upstream update, so the update note stops showing. */
    suspend fun keepUserGuide(gameId: Int) {
        val row = store.getSetupGuide(gameId, SetupGuideSource.USER) ?: return
        store.saveSetupGuide(row.copy(basedOnVersion = upstreamVersion(gameId), updatedAt = System.currentTimeMillis()))
    }

    /** Deletes downloaded catalog copies; bundled guides and the user's own guides stay. */
    suspend fun clearDownloadedGuides() {
        store.clearSetupGuides(SetupGuideSource.CATALOG)
        refreshAvailability()
    }

    /** The user's own guides as JSON documents, for backups. */
    suspend fun userGuidesJson(): List<String> =
        store.getSetupGuides().filter { it.source == SetupGuideSource.USER }.map { it.guideJson }

    /** Restores the user's own guides from a backup; invalid documents are skipped. Returns the count. */
    suspend fun restoreUserGuides(documents: List<String>): Int {
        var restored = 0
        documents.forEach { json ->
            val guide = SetupGuideJson.parseOrNull(json) ?: return@forEach
            if (SetupGuideValidator.validate(guide).isNotEmpty()) return@forEach
            saveUserGuide(guide, basedOnVersion = upstreamVersion(guide.gameId))
            restored++
        }
        refreshAvailability()
        if (restored > 0) _userGuideChanges.value++
        return restored
    }

    private suspend fun saveUserGuide(guide: SetupGuide, basedOnVersion: Int?) {
        store.saveSetupGuide(
            StoredSetupGuide(
                gameId = guide.gameId,
                source = SetupGuideSource.USER,
                guideJson = SetupGuideJson.toJsonString(guide),
                schemaVersion = guide.schemaVersion,
                version = guide.version,
                basedOnVersion = basedOnVersion
            )
        )
    }

    /** Newest standard (bundled or downloaded) version of a game's guide, or null if it has none. */
    private suspend fun upstreamVersion(baseId: Int): Int? = listOfNotNull(
        bundled.index().firstOrNull { it.gameId == baseId && it.isSupported() }?.version,
        store.getSetupGuide(baseId, SetupGuideSource.CATALOG)?.version
    ).maxOrNull()

    /** Downloads the catalog copy when it is newer than the local one; true if one was saved. */
    private suspend fun downloadIfNewer(baseId: Int): Boolean = mutex.withLock {
        val entry = store.getSetupGuideCatalog().firstOrNull { it.gameId == baseId && it.isSupported() }
            ?: return@withLock false
        val localVersion = maxOf(
            store.getSetupGuide(baseId, SetupGuideSource.CATALOG)?.version ?: 0,
            bundled.index().firstOrNull { it.gameId == baseId }?.version ?: 0
        )
        if (entry.version <= localVersion) return@withLock false
        val json = catalog.fetchGuideJson(entry) ?: return@withLock false
        val guide = parseValid(json, baseId) ?: return@withLock false
        store.saveSetupGuide(
            StoredSetupGuide(
                gameId = baseId,
                source = SetupGuideSource.CATALOG,
                guideJson = json,
                schemaVersion = guide.schemaVersion,
                version = guide.version
            )
        )
        true
    }

    private fun parseValid(json: String, expectedGameId: Int): SetupGuide? {
        val guide = SetupGuideJson.parseOrNull(json) ?: return null.also { Log.w(TAG, "Guide $expectedGameId unparseable") }
        val problems = SetupGuideValidator.validate(guide)
        if (guide.gameId != expectedGameId || problems.isNotEmpty()) {
            Log.w(TAG, "Guide $expectedGameId rejected: ${problems.joinToString()}")
            return null
        }
        return guide
    }

    private fun SetupGuideIndexEntry.isSupported() = schemaVersion <= SetupGuide.CURRENT_SCHEMA_VERSION

    private companion object {
        const val TAG = "SetupGuides"
        const val CATALOG_MAX_AGE_MS = 24L * 60 * 60 * 1000
    }
}
