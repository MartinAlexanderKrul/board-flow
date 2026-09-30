package cz.nicolsburg.boardflow.data.setupguide

import android.util.Log
import cz.nicolsburg.boardflow.data.CanonicalCollectionStore
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

/**
 * Combines the three guide layers:
 * - USER guides in Room always win (copy-on-write customisations; later phases),
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

    suspend fun refreshAvailability() {
        val bundledIndex = bundled.index().filter { it.isSupported() }
        val catalogIndex = store.getSetupGuideCatalog().filter { it.isSupported() }
        val stored = store.getSetupGuides()
        val offlineBaseIds = bundledIndex.map { it.gameId }.toSet() + stored.map { it.gameId }

        val result = mutableMapOf<Int, SetupGuideAvailability>()
        fun register(baseId: Int, ids: Set<Int>) {
            val availability = SetupGuideAvailability(baseId, offlineReady = baseId in offlineBaseIds)
            // A base game's own id always maps to itself, even if another guide lists it as a module.
            ids.forEach { id -> if (id == baseId || id !in result) result[id] = availability }
        }
        (bundledIndex + catalogIndex).forEach { register(it.gameId, it.gameIds) }
        stored.forEach { row ->
            SetupGuideJson.parseOrNull(row.guideJson)?.let { register(it.gameId, it.allGameIds) }
        }
        _availability.value = result
    }

    /**
     * Loads the guide for any id that maps to one. Downloads a newer catalog copy first when
     * online; falls back to whatever is local when offline or the download fails.
     */
    suspend fun loadGuide(anyGameId: Int, isOnline: Boolean): LoadedSetupGuide? {
        val baseId = _availability.value[anyGameId]?.baseGameId
            ?: run { refreshAvailability(); _availability.value[anyGameId]?.baseGameId }
            ?: return null

        store.getSetupGuide(baseId, SetupGuideSource.USER)
            ?.let { parseValid(it.guideJson, baseId) }
            ?.let { return LoadedSetupGuide(it, SetupGuideSource.USER) }

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

    /** Refreshes the cached remote index at most once per [CATALOG_MAX_AGE_MS] unless [force]d. */
    suspend fun refreshCatalogIfStale(isOnline: Boolean, force: Boolean = false) {
        if (isOnline) {
            mutex.withLock {
                val updatedAt = store.getSetupGuideCatalogUpdatedAt() ?: 0L
                if (force || System.currentTimeMillis() - updatedAt > CATALOG_MAX_AGE_MS) {
                    catalog.fetchIndex()?.let { store.replaceSetupGuideCatalog(it) }
                }
            }
        }
        refreshAvailability()
    }

    /** Makes catalog guides for these (e.g. owned) games available offline. */
    suspend fun prefetch(gameIds: Collection<Int>, isOnline: Boolean) {
        if (!isOnline || gameIds.isEmpty()) return
        val baseIds = gameIds.mapNotNull { _availability.value[it]?.baseGameId }.toSet()
        baseIds.forEach { downloadIfNewer(it) }
        if (baseIds.isNotEmpty()) refreshAvailability()
    }

    private suspend fun downloadIfNewer(baseId: Int) = mutex.withLock {
        val entry = store.getSetupGuideCatalog().firstOrNull { it.gameId == baseId && it.isSupported() }
            ?: return@withLock
        val localVersion = maxOf(
            store.getSetupGuide(baseId, SetupGuideSource.CATALOG)?.version ?: 0,
            bundled.index().firstOrNull { it.gameId == baseId }?.version ?: 0
        )
        if (entry.version <= localVersion) return@withLock
        val json = catalog.fetchGuideJson(entry) ?: return@withLock
        val guide = parseValid(json, baseId) ?: return@withLock
        store.saveSetupGuide(
            StoredSetupGuide(
                gameId = baseId,
                source = SetupGuideSource.CATALOG,
                guideJson = json,
                schemaVersion = guide.schemaVersion,
                version = guide.version
            )
        )
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
