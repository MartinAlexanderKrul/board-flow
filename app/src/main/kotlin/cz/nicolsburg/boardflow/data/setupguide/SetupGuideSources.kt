package cz.nicolsburg.boardflow.data.setupguide

import android.content.Context
import android.util.Log
import cz.nicolsburg.boardflow.model.SetupGuideSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Row shape of the `setup_guides` Room table. */
data class StoredSetupGuide(
    val gameId: Int,
    val source: SetupGuideSource,
    val guideJson: String,
    val schemaVersion: Int,
    val version: Int,
    val basedOnVersion: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Guides shipped inside the APK under `assets/setup-guides/`. The asset folder is the repo-root
 * `setup-guides/` directory (wired in `app/build.gradle.kts`), which is also what the remote
 * catalog serves, so one folder is both the bundled copy and the online source.
 */
class BundledSetupGuideSource(context: Context) {
    private val assets = context.applicationContext.assets

    suspend fun index(): List<SetupGuideIndexEntry> = withContext(Dispatchers.IO) {
        runCatching { SetupGuideIndex.parse(read("$ASSET_DIR/index.json")) }
            .onFailure { Log.w(TAG, "Bundled setup-guide index unreadable", it) }
            .getOrDefault(emptyList())
    }

    suspend fun guideJson(entry: SetupGuideIndexEntry): String? = withContext(Dispatchers.IO) {
        runCatching { read("$ASSET_DIR/${entry.path}") }
            .onFailure { Log.w(TAG, "Bundled setup guide ${entry.path} unreadable", it) }
            .getOrNull()
    }

    private fun read(path: String): String = assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private companion object {
        const val TAG = "SetupGuides"
        const val ASSET_DIR = "setup-guides"
    }
}

/**
 * Static remote catalog: the same `setup-guides/` folder served from the public GitHub repo.
 * No backend; new or corrected guides land by merging a pull request. Failures are quiet
 * (null), matching how optional BGG lookups behave.
 */
class SetupGuideCatalogClient(
    private val baseUrl: String = DEFAULT_BASE_URL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchIndex(): List<SetupGuideIndexEntry>? = fetch("index.json")?.let { body ->
        runCatching { SetupGuideIndex.parse(body) }
            .onFailure { Log.w(TAG, "Catalog index unparseable", it) }
            .getOrNull()
    }

    suspend fun fetchGuideJson(entry: SetupGuideIndexEntry): String? = fetch(entry.path)

    private suspend fun fetch(path: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url(baseUrl + path).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Catalog GET $path -> HTTP ${response.code}")
                    null
                } else {
                    response.body?.string()
                }
            }
        }.onFailure { Log.w(TAG, "Catalog GET $path failed: ${it.message}") }.getOrNull()
    }

    companion object {
        private const val TAG = "SetupGuides"
        const val DEFAULT_BASE_URL =
            "https://raw.githubusercontent.com/MartinAlexanderKrul/board-flow/master/setup-guides/"
    }
}
