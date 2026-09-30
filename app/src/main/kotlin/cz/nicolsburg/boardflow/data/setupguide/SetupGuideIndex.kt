package cz.nicolsburg.boardflow.data.setupguide

import org.json.JSONArray
import org.json.JSONObject

/**
 * One row of `setup-guides/index.json`. The index lets the app know a guide exists (and for which
 * BGG ids, including expansions) without downloading or parsing every guide file.
 */
data class SetupGuideIndexEntry(
    val gameId: Int,
    val gameName: String,
    /** Base id plus alias and module ids; any of these opens the guide. */
    val gameIds: Set<Int>,
    val version: Int,
    val schemaVersion: Int,
    val path: String
)

object SetupGuideIndex {
    fun parse(json: String): List<SetupGuideIndexEntry> =
        JSONObject(json).optJSONArray("guides").objects().mapNotNull { obj ->
            runCatching {
                val gameId = obj.getInt("gameId")
                SetupGuideIndexEntry(
                    gameId = gameId,
                    gameName = obj.getString("gameName"),
                    gameIds = (obj.optJSONArray("gameIds").ints() + gameId).toSet(),
                    version = obj.getInt("version"),
                    schemaVersion = obj.getInt("schemaVersion"),
                    path = obj.getString("path")
                )
            }.getOrNull()
        }

    fun toJson(entries: List<SetupGuideIndexEntry>): String = JSONObject().apply {
        put("schemaVersion", 1)
        put("guides", JSONArray(entries.sortedBy { it.gameId }.map { e ->
            JSONObject().apply {
                put("gameId", e.gameId)
                put("gameName", e.gameName)
                put("gameIds", JSONArray(e.gameIds.sorted()))
                put("version", e.version)
                put("schemaVersion", e.schemaVersion)
                put("path", e.path)
            }
        }))
    }.toString(2)
}
