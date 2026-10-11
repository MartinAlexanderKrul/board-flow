package cz.nicolsburg.boardflow.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * BGG thing details as stored in the `bgg_game_details` table, so a game outside the collection
 * opens its detail dialog without asking BGG again (see CanonicalCollectionStore.getBggGameDetails).
 */
object BggThingDetailJson {

    fun toJson(detail: BggApiClient.ThingDetail): String = JSONObject().apply {
        put("objectid", detail.objectid)
        put("name", detail.name)
        put("thumbnailUrl", detail.thumbnailUrl ?: JSONObject.NULL)
        put("avgweight", detail.avgweight)
        put("bggbestplayers", detail.bggbestplayers)
        put("bggrecplayers", detail.bggrecplayers)
        put("bggnotrecplayers", detail.bggnotrecplayers)
        put("bggrecagerange", detail.bggrecagerange)
        put("bgglanguagedependence", detail.bgglanguagedependence)
        put("yearpublished", detail.yearpublished)
        put("minplayers", detail.minplayers)
        put("maxplayers", detail.maxplayers)
        put("playingtime", detail.playingtime)
        put("minplaytime", detail.minplaytime)
        put("maxplaytime", detail.maxplaytime)
        put("type", detail.type)
        put("baseGames", JSONArray().apply {
            detail.baseGames.forEach { (id, name) -> put(JSONObject().put("id", id).put("name", name)) }
        })
    }.toString()

    fun fromJson(json: String): BggApiClient.ThingDetail? = runCatching {
        val obj = JSONObject(json)
        val bases = obj.optJSONArray("baseGames")
        BggApiClient.ThingDetail(
            objectid = obj.getString("objectid"),
            name = obj.optString("name"),
            thumbnailUrl = obj.optString("thumbnailUrl").takeIf { !obj.isNull("thumbnailUrl") && it.isNotBlank() },
            avgweight = obj.optString("avgweight"),
            bggbestplayers = obj.optString("bggbestplayers"),
            bggrecplayers = obj.optString("bggrecplayers"),
            bggnotrecplayers = obj.optString("bggnotrecplayers"),
            bggrecagerange = obj.optString("bggrecagerange"),
            bgglanguagedependence = obj.optString("bgglanguagedependence"),
            yearpublished = obj.optString("yearpublished"),
            minplayers = obj.optString("minplayers"),
            maxplayers = obj.optString("maxplayers"),
            playingtime = obj.optString("playingtime"),
            minplaytime = obj.optString("minplaytime"),
            maxplaytime = obj.optString("maxplaytime"),
            type = obj.optString("type"),
            baseGames = if (bases == null) emptyList() else (0 until bases.length()).map { i ->
                val base = bases.getJSONObject(i)
                base.getInt("id") to base.optString("name")
            }
        )
    }.getOrNull()
}
