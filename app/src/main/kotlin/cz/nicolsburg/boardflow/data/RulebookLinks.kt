package cz.nicolsburg.boardflow.data

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONObject

/**
 * Links from a BGG id to its rulebook in the public `boardgame-rulebooks` GitHub repo.
 * The map is the bundled `assets/rulebooks.json` (BGG id -> a PDF path, or a folder when the
 * game has several files), so a game only gets a link once its rulebook is in that repo.
 */
object RulebookLinks {
    private const val TAG = "RulebookLinks"
    private const val ASSET = "rulebooks.json"
    private const val REPO_URL = "https://github.com/MartinAlexanderKrul/boardgame-rulebooks"

    @Volatile private var paths: Map<Int, String>? = null

    fun urlFor(context: Context, gameId: Int?): String? {
        val path = gameId?.let { load(context)[it] } ?: return null
        val kind = if (path.endsWith(".pdf", ignoreCase = true)) "blob" else "tree"
        return "$REPO_URL/$kind/main/" + path.split('/').joinToString("/") { Uri.encode(it) }
    }

    private fun load(context: Context): Map<Int, String> = paths ?: synchronized(this) {
        paths ?: runCatching {
            val json = JSONObject(
                context.applicationContext.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
            )
            buildMap {
                json.keys().forEach { key -> key.toIntOrNull()?.let { put(it, json.getString(key)) } }
            }
        }.onFailure { Log.w(TAG, "Bundled rulebook links unreadable", it) }
            .getOrDefault(emptyMap())
            .also { paths = it }
    }
}
