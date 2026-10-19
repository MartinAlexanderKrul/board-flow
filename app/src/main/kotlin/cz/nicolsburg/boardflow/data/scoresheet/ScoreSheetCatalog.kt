package cz.nicolsburg.boardflow.data.scoresheet

import android.content.Context
import android.util.Log
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.SheetOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** One sheet in `score-sheets/index.json`: its id, games, version and file. */
data class ScoreSheetIndexEntry(
    val id: String,
    val gameName: String,
    val gameIds: Set<Int>,
    val version: Int,
    val path: String
)

/**
 * The index of the repo-root `score-sheets/` folder: the sheets shared by BoardFlow users (merged
 * pull requests) and fixes to built-in sheets (same id as the built-in one). Pure, unit-tested.
 */
object ScoreSheetIndex {
    fun parse(json: String): List<ScoreSheetIndexEntry> {
        val arr = JSONObject(json).optJSONArray("sheets") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                ScoreSheetIndexEntry(
                    id = o.getString("id"),
                    gameName = o.getString("gameName"),
                    gameIds = o.getJSONArray("gameIds").let { ids -> (0 until ids.length()).map(ids::getInt).toSet() },
                    version = o.optInt("version", 1),
                    path = o.getString("path")
                )
            }.getOrNull()
        }
    }

    fun toJson(entries: List<ScoreSheetIndexEntry>): String = JSONObject().apply {
        put("schemaVersion", 1)
        put("sheets", JSONArray(entries.sortedBy { it.id }.map { e ->
            JSONObject().apply {
                put("id", e.id)
                put("gameName", e.gameName)
                put("gameIds", JSONArray(e.gameIds.sorted()))
                put("version", e.version)
                put("path", e.path)
            }
        }))
    }.toString(2)

    /** Reads a catalog sheet: it is shared and checked, so it shows as a community sheet. */
    fun parseSheet(json: String): ScoreSheet? = runCatching { ScoreSheetJson.parse(json) }.getOrNull()
        ?.copy(origin = SheetOrigin.COMMUNITY, reviewed = true)
        ?.takeIf { ScoreSheetValidator.validate(it).isEmpty() }
}

/**
 * The catalog's sheets on the phone: the copy bundled in the APK (`assets/score-sheets/`, the
 * repo folder, wired in `app/build.gradle.kts`) and newer ones downloaded from GitHub into
 * `filesDir/score-sheets/`. For each id the higher version wins.
 */
class ScoreSheetCatalog(
    context: Context,
    private val baseUrl: String = DEFAULT_BASE_URL
) {
    private val assets = context.applicationContext.assets
    private val cacheDir = File(context.applicationContext.filesDir, "score-sheets")
    private val prefs = context.applicationContext.getSharedPreferences("score_sheet_catalog", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Every valid catalog sheet, bundled or downloaded, newest version per id. */
    suspend fun sheets(): List<ScoreSheet> = withContext(Dispatchers.IO) {
        val bundled = runCatching { ScoreSheetIndex.parse(asset("index.json")) }.getOrDefault(emptyList())
            .mapNotNull { e -> runCatching { asset(e.path) }.getOrNull()?.let { e to it } }
        val downloaded = runCatching { ScoreSheetIndex.parse(File(cacheDir, "index.json").readText()) }.getOrDefault(emptyList())
            .mapNotNull { e -> runCatching { File(cacheDir, e.path).readText() }.getOrNull()?.let { e to it } }
        (bundled + downloaded)
            .groupBy { it.first.id }
            .mapNotNull { (_, copies) -> copies.sortedByDescending { it.first.version }.firstNotNullOfOrNull { ScoreSheetIndex.parseSheet(it.second) } }
    }

    /** Downloads the index and any sheet newer than the copy on the phone, at most once a day. Returns true when something changed. */
    suspend fun refreshIfStale(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!force && System.currentTimeMillis() - prefs.getLong(KEY_UPDATED_AT, 0) < MAX_AGE_MS) return@withContext false
        val indexText = fetch("index.json") ?: return@withContext false
        val remote = runCatching { ScoreSheetIndex.parse(indexText) }.getOrNull() ?: return@withContext false
        val known = runCatching { ScoreSheetIndex.parse(File(cacheDir, "index.json").readText()) }.getOrDefault(emptyList())
            .associateBy { it.id }
        cacheDir.mkdirs()
        var changed = false
        val kept = remote.mapNotNull { entry ->
            val have = known[entry.id]
            if (have != null && have.version >= entry.version && File(cacheDir, entry.path).exists()) return@mapNotNull entry
            val text = fetch(entry.path)?.takeIf { ScoreSheetIndex.parseSheet(it) != null } ?: return@mapNotNull null
            File(cacheDir, entry.path).writeText(text)
            changed = true
            entry
        }
        File(cacheDir, "index.json").writeText(ScoreSheetIndex.toJson(kept))
        prefs.edit().putLong(KEY_UPDATED_AT, System.currentTimeMillis()).apply()
        changed || kept.size != known.size
    }

    private fun asset(path: String): String =
        assets.open("$ASSET_DIR/$path").bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun fetch(path: String): String? = runCatching {
        client.newCall(Request.Builder().url(baseUrl + path).build()).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null.also { Log.w(TAG, "GET $path -> HTTP ${response.code}") }
        }
    }.onFailure { Log.w(TAG, "GET $path failed: ${it.message}") }.getOrNull()

    private companion object {
        const val TAG = "ScoreSheetCatalog"
        const val ASSET_DIR = "score-sheets"
        const val KEY_UPDATED_AT = "updated_at"
        const val MAX_AGE_MS = 24L * 60 * 60 * 1000
        const val DEFAULT_BASE_URL = "https://raw.githubusercontent.com/MartinAlexanderKrul/board-flow/master/score-sheets/"
    }
}
