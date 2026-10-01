package cz.nicolsburg.boardflow.data

import android.util.Log
import cz.nicolsburg.boardflow.BuildConfig
import cz.nicolsburg.boardflow.model.BggCollectionEntry
import cz.nicolsburg.boardflow.model.BggCollectionStatus
import cz.nicolsburg.boardflow.model.BggGame
import cz.nicolsburg.boardflow.model.BggPrivateInfo
import cz.nicolsburg.boardflow.model.BggSleeveMarker
import cz.nicolsburg.boardflow.model.SleeveTrackingState
import cz.nicolsburg.boardflow.model.BggCredentials
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.PlayerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.time.LocalDate

class BggRepository {

    companion object {
        private const val TAG = "BggRepository"
    }

    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val valid = cookies.filter { it.expiresAt > System.currentTimeMillis() }
            cookieStore.getOrPut(url.host) { mutableListOf() }.apply {
                valid.forEach { newCookie ->
                    removeAll { it.name == newCookie.name }
                    add(newCookie)
                }
            }
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            cookieStore[url.host] ?: emptyList()
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(
            HttpLoggingInterceptor { Log.d(TAG, redactBggPassword(it).replace('\n', ' ')) }.apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()

    suspend fun searchGames(query: String, xmlApiToken: String, exact: Boolean = false): Result<List<BggGame>> = withContext(Dispatchers.IO) {
        runCatching {
            if (xmlApiToken.isBlank()) {
                return@runCatching emptyList()
            }
            val url = "https://boardgamegeek.com/xmlapi2/search?query=${
                java.net.URLEncoder.encode(query, "UTF-8")
            }&type=boardgame,boardgameexpansion&exact=${if (exact) 1 else 0}"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $xmlApiToken")
                .build()
            val response = client.newCall(request).execute()
            if (response.code == 401) {
                return@runCatching emptyList()
            }
            if (!response.isSuccessful) {
                return@runCatching emptyList()
            }
            val body = response.body?.string() ?: return@runCatching emptyList()
            runCatching { parseSearchResults(body) }.getOrDefault(emptyList())
        }
    }

    suspend fun getUserCollection(username: String): Result<List<BggGame>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://boardgamegeek.com/xmlapi2/collection?username=${
                java.net.URLEncoder.encode(username, "UTF-8")
            }&own=1&subtype=boardgame&stats=1&excludesubtype=boardgameexpansion"
            var attempts = 0
            val maxAttempts = 3
            while (attempts < maxAttempts) {
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                val body = response.body?.string() ?: return@runCatching emptyList()
                when (response.code) {
                    200 -> return@runCatching parseCollectionResults(body)
                    202 -> { attempts++; if (attempts < maxAttempts) kotlinx.coroutines.delay(2000) else throw Exception("Collection still processing. Please try again in a moment.") }
                    401 -> throw Exception("Cannot access collection for '$username'. The profile may be private.")
                    404 -> throw Exception("User '$username' not found on BGG.")
                    else -> throw Exception("Failed to load collection: HTTP ${response.code}")
                }
            }
            emptyList()
        }
    }

    suspend fun getUserCollectionAuthenticated(credentials: BggCredentials): Result<List<BggGame>> = withContext(Dispatchers.IO) {
        runCatching {
            login(credentials).getOrThrow()
            val url = "https://boardgamegeek.com/xmlapi2/collection?username=${
                java.net.URLEncoder.encode(credentials.username, "UTF-8")
            }&own=1&subtype=boardgame"
            var attempts = 0
            val maxAttempts = 3
            while (attempts < maxAttempts) {
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                val body = response.body?.string() ?: return@runCatching emptyList()
                when (response.code) {
                    200 -> return@runCatching parseCollectionResults(body)
                    202 -> { attempts++; if (attempts < maxAttempts) kotlinx.coroutines.delay(2000) else throw Exception("Collection still processing. Please try again in a moment.") }
                    401 -> throw Exception("Authentication failed. Please check your BGG credentials in Settings.")
                    404 -> throw Exception("User '${credentials.username}' not found on BGG.")
                    else -> throw Exception("Failed to load collection: HTTP ${response.code}")
                }
            }
            emptyList()
        }
    }

    suspend fun login(credentials: BggCredentials): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            val json = JSONObject()
                .put(
                    "credentials",
                    JSONObject()
                        .put("username", credentials.username)
                        .put("password", credentials.password)
                )
                .toString()
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://boardgamegeek.com/login/api/v1")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Login failed: HTTP ${response.code}")
            val hasCookies = cookieStore["boardgamegeek.com"]?.any { it.name == "SessionID" } == true
            if (!hasCookies) throw Exception("Login failed: no session cookie received")
            Log.i(TAG, "Login success for ${credentials.username}")
        }
    }

    suspend fun logPlay(
        gameId: Int,
        date: LocalDate,
        players: List<PlayerResult>,
        playerBggUsernames: Map<Int, String> = emptyMap(),
        durationMinutes: Int = 0,
        location: String = "",
        comments: String = "",
        quantity: Int = 1,
        incomplete: Boolean = false,
        nowInStats: Boolean = true,
        playId: String? = null
    ): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val formBody = FormBody.Builder().apply {
                add("ajax", "1"); add("action", "save"); add("version", "2"); add("objecttype", "thing")
                add("objectid", gameId.toString()); add("playdate", date.toString())
                add("dateinput", date.toString()); add("length", durationMinutes.toString())
                add("location", location); add("comments", comments)
                add("quantity", quantity.coerceAtLeast(1).toString())
                add("incomplete", if (incomplete) "1" else "0")
                add("nowinstats", if (nowInStats) "1" else "0")
                if (playId != null) add("playid", playId)
                players.forEachIndexed { index, player ->
                    val position = index + 1
                    val bggUsername = playerBggUsernames[index]
                    add("players[$position][name]", player.name)
                    add("players[$position][position]", position.toString())
                    add("players[$position][score]", player.score)
                    add("players[$position][color]", player.color)
                    add("players[$position][win]", if (player.isWinner) "1" else "0")
                    add("players[$position][new]", if (player.isNew) "1" else "0")
                    add("players[$position][rating]", player.rating.takeUnless { it.isBlank() || it == "N/A" } ?: "0")
                    add("players[$position][selected]", if (bggUsername.isNullOrBlank()) "0" else "1")
                    if (!bggUsername.isNullOrBlank()) add("players[$position][username]", bggUsername)
                }
            }.build()
            val request = Request.Builder()
                .url("https://boardgamegeek.com/geekplay.php")
                .post(formBody)
                .addHeader("Referer", "https://boardgamegeek.com")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) throw Exception("Failed to log play: HTTP ${response.code}")
            val savedPlayId = extractSavedPlayId(responseBody) ?: playId
            if (savedPlayId == null && !looksLikePlaySaveAccepted(responseBody)) {
                throw Exception("Unexpected BGG response: $responseBody")
            }
            Log.i(TAG, "Play logged: gameId=$gameId date=$date players=${players.size} savedPlayId=$savedPlayId")
            savedPlayId
        }
    }

    suspend fun deletePlay(playId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val deleteRequest = linkedMapOf(
                "ajax" to "1",
                "action" to "delete",
                "version" to "2",
                "objecttype" to "thing",
                "playid" to playId
            )

            val initialBody = executeGeekplayPost(deleteRequest)
            Log.i(
                TAG,
                "Delete play confirmation step: body=${initialBody.take(200)}"
            )

            if (initialBody.contains("Play date required", ignoreCase = true)) {
                throw Exception("BGG requested additional play data before delete confirmation")
            }

            val confirmFields = parseHiddenInputs(initialBody).toMutableMap()
            if (confirmFields.isEmpty() && looksLikeDeleteAccepted(initialBody)) {
                return@runCatching
            }
            if (confirmFields.isEmpty()) {
                confirmFields["action"] = "delete"
                confirmFields["playid"] = playId
                confirmFields["final"] = "1"
            }
            confirmFields["ajax"] = "1"
            confirmFields["final"] = "1"
            confirmFields.putIfAbsent("action", "delete")
            confirmFields.putIfAbsent("playid", playId)

            val confirmBody = executeGeekplayPost(confirmFields)
            Log.i(
                TAG,
                "Delete play confirm step: body=${confirmBody.take(200)}"
            )
            val accepted = looksLikeDeleteAccepted(confirmBody)
            if (!accepted) {
                throw Exception("Unexpected BGG confirm-delete response: ${confirmBody.take(160)}")
            }
        }
    }

    private fun executeGeekplayPost(fields: Map<String, String>): String {
        val request = Request.Builder()
            .url("https://boardgamegeek.com/geekplay.php")
            .post(
                FormBody.Builder().apply {
                    fields.forEach { (key, value) -> add(key, value) }
                }.build()
            )
            .addHeader("Referer", "https://boardgamegeek.com")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw Exception("BGG geekplay request failed: HTTP ${response.code}")
        }
        return responseBody
    }

    private fun parseHiddenInputs(html: String): Map<String, String> {
        val htmlCandidates = buildList {
            add(html)
            decodeEmbeddedHtml(html)?.let { add(it) }
        }
        val matches = Regex(
            """<input[^>]*type=["']hidden["'][^>]*name=["']([^"']+)["'][^>]*value=["']([^"']*)["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )
        return buildMap {
            htmlCandidates.forEach { candidate ->
                matches.findAll(candidate).forEach { match ->
                    put(match.groupValues[1], htmlEntityDecode(match.groupValues[2]))
                }
            }
        }
    }

    private fun decodeEmbeddedHtml(body: String): String? {
        val trimmed = body.trim()
        if (!trimmed.startsWith("{")) return null
        return runCatching {
            val json = JSONObject(trimmed)
            sequenceOf("html", "content", "form", "dialog", "markup")
                .mapNotNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
                .firstOrNull()
                ?.let(::htmlEntityDecode)
        }.getOrNull()
    }

    private fun htmlEntityDecode(value: String): String {
        return value
            .replace("\\/", "/")
            .replace("\\\"", "\"")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun looksLikeDeleteAccepted(body: String): Boolean {
        return body.contains("\"error\":null")
            || body.contains("\"error\": false")
            || body.contains("deleted", ignoreCase = true)
            || body.contains("success", ignoreCase = true)
            || body.contains("play has been deleted", ignoreCase = true)
            || body.isBlank()
    }

    private fun looksLikePlaySaveAccepted(body: String): Boolean {
        return body.contains("\"error\":null")
            || body.contains("\"error\": null")
            || body.contains("\"error\":false")
            || body.contains("\"error\": false")
            || body.contains("saved", ignoreCase = true)
            || body.isBlank()
    }

    private fun extractSavedPlayId(body: String): String? {
        runCatching {
            val json = JSONObject(body.trim())
            listOf("playid", "playId", "id").forEach { key ->
                json.optString(key).takeIf { it.isNotBlank() && it != "0" }?.let { return it }
            }
        }

        Regex(
            """"(?:playid|playId|id)"\s*:\s*"?([0-9]+)"?""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.getOrNull(1)?.let { return it }

        Regex(
            """name=["']playid["'][^>]*value=["']([0-9]+)["']""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.getOrNull(1)?.let { return it }

        Regex(
            """\bplayid\b\s*[:=]\s*["']?([0-9]+)""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.getOrNull(1)?.let { return it }

        return null
    }

    suspend fun getPlays(username: String): Result<List<LoggedPlay>> = withContext(Dispatchers.IO) {
        runCatching {
            val allPlays = mutableListOf<LoggedPlay>()
            var page = 1; val maxPages = 150
            while (page <= maxPages) {
                val url = "https://boardgamegeek.com/xmlapi2/plays?username=${
                    java.net.URLEncoder.encode(username, "UTF-8")
                }&type=thing&page=$page"
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                if (!response.isSuccessful) {
                    if (page == 1) throw Exception("Failed to fetch plays: HTTP ${response.code}")
                    break
                }
                val body = response.body?.string() ?: break
                val (plays, total) = parsePlays(body)
                allPlays.addAll(plays)
                if (plays.isEmpty() || allPlays.size >= total) break
                page++
            }
            allPlays
        }
    }

    private fun parsePlays(xml: String): Pair<List<LoggedPlay>, Int> {
        val plays = mutableListOf<LoggedPlay>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))
        var total = 0; var playId: String? = null; var date = ""; var length = 0
        var location = ""; var gameName: String? = null; var gameId: Int? = null
        var quantity = 1; var incomplete = false; var nowInStats = true
        var comments = ""; var insideComments = false
        var players = mutableListOf<PlayerResult>(); var insidePlayers = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "plays"    -> total = parser.getAttributeValue(null, "total")?.toIntOrNull() ?: 0
                    "play"     -> {
                        playId = parser.getAttributeValue(null, "id")
                        date = parser.getAttributeValue(null, "date") ?: ""
                        length = parser.getAttributeValue(null, "length")?.toIntOrNull() ?: 0
                        location = parser.getAttributeValue(null, "location") ?: ""
                        quantity = parser.getAttributeValue(null, "quantity")?.toIntOrNull() ?: 1
                        incomplete = parser.getAttributeValue(null, "incomplete") == "1"
                        nowInStats = parser.getAttributeValue(null, "nowinstats") != "0"
                        gameName = null; gameId = null; players = mutableListOf(); comments = ""
                    }
                    "item"     -> { gameName = parser.getAttributeValue(null, "name"); gameId = parser.getAttributeValue(null, "objectid")?.toIntOrNull() }
                    "players"  -> insidePlayers = true
                    "player"   -> if (insidePlayers) {
                        val name = parser.getAttributeValue(null, "name") ?: ""
                        val score = parser.getAttributeValue(null, "score") ?: ""
                        val win = parser.getAttributeValue(null, "win") == "1"
                        val color = parser.getAttributeValue(null, "color") ?: ""
                        val rating = parser.getAttributeValue(null, "rating") ?: ""
                        val isNew = parser.getAttributeValue(null, "new") == "1"
                        if (name.isNotBlank()) players.add(PlayerResult(name, score, win, color, rating, isNew))
                    }
                    "comments" -> insideComments = true
                }
                XmlPullParser.TEXT -> if (insideComments) comments += parser.text
                XmlPullParser.END_TAG -> when (parser.name) {
                    "players"  -> insidePlayers = false
                    "comments" -> insideComments = false
                    "play"     -> {
                        val parsedPlayId = playId
                        val parsedGameName = gameName
                        val parsedGameId = gameId
                        if (parsedPlayId != null && parsedGameName != null && parsedGameId != null) {
                        plays.add(LoggedPlay(
                            id = parsedPlayId, gameId = parsedGameId, gameName = parsedGameName, date = date,
                            players = players.toList(), durationMinutes = length, location = location,
                            postedToBgg = true, comments = comments.trim(),
                            quantity = quantity, incomplete = incomplete, nowInStats = nowInStats
                        ))
                        }
                    }
                }
            }
            event = parser.next()
        }
        return Pair(plays, total)
    }

    private fun parseSearchResults(xml: String): List<BggGame> {
        val games = mutableListOf<BggGame>()
        val factory = XmlPullParserFactory.newInstance(); val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))
        var currentId: Int? = null; var currentName: String? = null; var currentYear: String? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> { currentId = parser.getAttributeValue(null, "id")?.toIntOrNull(); currentName = null; currentYear = null }
                    "name" -> { if (parser.getAttributeValue(null, "type") == "primary") currentName = parser.getAttributeValue(null, "value") }
                    "yearpublished" -> { currentYear = parser.getAttributeValue(null, "value") }
                }
                XmlPullParser.END_TAG -> if (parser.name == "item") {
                    val parsedId = currentId
                    val parsedName = currentName
                    if (parsedId != null && parsedName != null) {
                        games.add(BggGame(id = parsedId, name = parsedName, yearPublished = currentYear, thumbnailUrl = null))
                    }
                }
            }
            event = parser.next()
        }
        return games.sortedBy { it.name.lowercase() }
    }

    suspend fun rateGame(gameId: Int, rating: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            require(rating in 1..10) { "Rating must be between 1 and 10" }
            val formBody = FormBody.Builder()
                .add("ajax", "1")
                .add("action", "rating")
                .add("objecttype", "thing")
                .add("objectid", gameId.toString())
                .add("rating", rating.toString())
                .build()
            val request = Request.Builder()
                .url("https://boardgamegeek.com/geekcollection.php")
                .post(formBody)
                .addHeader("Referer", "https://boardgamegeek.com/boardgame/$gameId")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) throw Exception("Failed to rate game: HTTP ${response.code}")
            Log.i(TAG, "Rated game $gameId with $rating: ${body.take(100)}")
        }
    }

    /**
     * BGG exposes no documented write API for collections; this drives the same internal
     * geekcollection.php endpoint the website's own status checkboxes post to, so it can
     * break without notice. Requires a prior [login].
     *
     * Pass the [collectionId] (BGG's `collid`, see [getCollectionId]) for a game already in
     * the collection; without it BGG creates a new entry.
     *
     * Clearing every flag does NOT remove the game: verified against a live account, the entry
     * survives with every flag set to 0. Use [deleteCollectionEntry] to remove it.
     *
     * Returns the entry's `collid`, but only the one passed in: a successful save answers with
     * an HTML fragment of the new status labels that carries no id, so a freshly created entry
     * returns null and has to be re-resolved with [getCollectionId].
     */
    suspend fun setCollectionStatus(
        gameId: Int,
        status: BggCollectionStatus,
        collectionId: String? = null
    ): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            require(gameId > 0) { "Invalid game id: $gameId" }
            val formBody = FormBody.Builder()
                .add("ajax", "1")
                .add("action", "savedata")
                .add("objecttype", "thing")
                .add("objectid", gameId.toString())
                .add("collid", collectionId.orEmpty())
                .add("fieldname", "status")
                .add("own", status.own.asBggFlag())
                .add("prevowned", status.previouslyOwned.asBggFlag())
                .add("fortrade", status.forTrade.asBggFlag())
                .add("want", status.wantInTrade.asBggFlag())
                .add("wanttoplay", status.wantToPlay.asBggFlag())
                .add("wanttobuy", status.wantToBuy.asBggFlag())
                .add("wishlist", status.wishlist.asBggFlag())
                .add("preordered", status.preordered.asBggFlag())
                .apply {
                    if (status.wishlist) add("wishlistpriority", status.wishlistPriority.coerceIn(1, 5).toString())
                }
                .build()
            val request = Request.Builder()
                .url("https://boardgamegeek.com/geekcollection.php")
                .post(formBody)
                .addHeader("Referer", "https://boardgamegeek.com/boardgame/$gameId")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("Failed to update collection: HTTP ${response.code}")
            extractCollectionError(body)?.let { throw Exception("BGG rejected collection update: $it") }
            val savedCollectionId = extractCollectionId(body) ?: collectionId
            Log.i(TAG, "Collection status saved: gameId=$gameId collid=$savedCollectionId body=${body.take(120)}")
            savedCollectionId
        }
    }

    /**
     * Removes a collection entry outright. Clearing every status flag only zeroes the entry and
     * leaves it in the collection, so removal needs this separate `action=delete` call.
     * Requires a prior [login] and the entry's [collectionId] (see [getCollectionId]).
     *
     * A successful delete answers with HTTP 200 and an empty body.
     */
    suspend fun deleteCollectionEntry(gameId: Int, collectionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            require(gameId > 0) { "Invalid game id: $gameId" }
            require(collectionId.isNotBlank()) { "A collection entry id is required to remove an entry" }
            val formBody = FormBody.Builder()
                .add("ajax", "1")
                .add("action", "delete")
                .add("objecttype", "thing")
                .add("objectid", gameId.toString())
                .add("collid", collectionId)
                .build()
            val request = Request.Builder()
                .url("https://boardgamegeek.com/geekcollection.php")
                .post(formBody)
                .addHeader("Referer", "https://boardgamegeek.com/boardgame/$gameId")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw Exception("Failed to remove collection entry: HTTP ${response.code}")
            extractCollectionError(body)?.let { throw Exception("BGG rejected collection removal: $it") }
            Log.i(TAG, "Collection entry removed: gameId=$gameId collid=$collectionId")
        }
    }

    /** Looks up BGG's `collid` for a game already in [username]'s collection, or null if absent. */
    suspend fun getCollectionId(username: String, gameId: Int): Result<String?> =
        getCollectionEntry(username, gameId).map { it?.collectionId }

    /**
     * Reads [username]'s existing collection entry for a game, or null if the game is not in the
     * collection. `brief=1` carries both the `collid` and the status flags, so this one read backs
     * both [setCollectionStatus]'s id and a status editor. Needs a prior [login] for a private
     * collection; unauthenticated reads of a real user come back 401.
     */
    suspend fun getCollectionEntry(username: String, gameId: Int): Result<BggCollectionEntry?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://boardgamegeek.com/xmlapi2/collection?username=${
                java.net.URLEncoder.encode(username, "UTF-8")
            }&id=$gameId&brief=1"
            val maxAttempts = 3
            repeat(maxAttempts) { attempt ->
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                val body = response.body?.string().orEmpty()
                when (response.code) {
                    // A failed lookup must not fall through as "no collid": that would make
                    // setCollectionStatus create a duplicate entry instead of updating.
                    200 -> {
                        extractXmlApiError(body)?.let { throw Exception("Failed to look up collection entry: $it") }
                        return@runCatching parseCollectionEntry(body)
                    }
                    202 -> if (attempt < maxAttempts - 1) {
                        kotlinx.coroutines.delay(2000)
                    } else {
                        throw Exception("Collection still processing. Please try again in a moment.")
                    }
                    401 -> throw Exception("Cannot read collection for '$username'. Please check your BGG credentials in Settings.")
                    else -> throw Exception("Failed to look up collection entry: HTTP ${response.code}")
                }
            }
            null
        }
    }

    /**
     * Reads the Private Info block of the logged-in user's collection entry for a game, or null
     * if the game is not in the collection. Requires a prior [login].
     *
     * This uses the site's own JSON endpoint rather than xmlapi2: the XML collection is cached and
     * keeps serving the old private info for a while after a write, and since a save replaces the
     * whole block, posting back a stale read would undo the previous edit. The JSON keys are also
     * exactly the form field names the save expects.
     */
    suspend fun getPrivateInfo(gameId: Int): Result<BggPrivateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = currentUserId()
            val body = getJson("https://boardgamegeek.com/api/collections?objectid=$gameId&objecttype=thing&userid=$userId")
            val item = JSONObject(body).optJSONArray("items")?.optJSONObject(0) ?: return@runCatching null
            val collectionId = item.optString("collid").takeIf { it.isNotBlank() && it != "null" }
                ?: throw Exception("BGG returned a collection entry without an id")
            fun value(name: String) = if (item.isNull(name)) "" else item.optString(name)
            BggPrivateInfo(
                collectionId = collectionId,
                fields = PRIVATE_INFO_FIELDS.associateWith(::value),
                comment = value("privatecomment")
            )
        }
    }

    /**
     * Writes the sleeve marker (see [BggSleeveMarker]) into the private comment of an existing
     * collection entry. Returns false without writing when the game has no entry: sleeve backup
     * never creates collection entries. Requires a prior [login].
     *
     * Private Info is saved as one block (`fieldname=ownership`) and BGG blanks any field left
     * out, so the current values are read first and posted back unchanged. Verified against a
     * live account: every field round-trips and the status flags are untouched.
     */
    suspend fun saveSleeveMarker(gameId: Int, state: SleeveTrackingState): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(gameId > 0) { "Invalid game id: $gameId" }
                val info = getPrivateInfo(gameId).getOrThrow() ?: return@runCatching false
                val comment = BggSleeveMarker.apply(info.comment, state)
                if (comment == info.comment) return@runCatching true
                val formBody = FormBody.Builder()
                    .add("ajax", "1")
                    .add("action", "savedata")
                    .add("objecttype", "thing")
                    .add("objectid", gameId.toString())
                    .add("collid", info.collectionId)
                    .add("fieldname", "ownership")
                    .apply { info.fields.forEach { (name, value) -> add(name, value) } }
                    .add("privatecomment", comment)
                    .build()
                val request = Request.Builder()
                    .url("https://boardgamegeek.com/geekcollection.php")
                    .post(formBody)
                    .addHeader("Referer", "https://boardgamegeek.com/boardgame/$gameId")
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw Exception("Failed to save sleeve marker: HTTP ${response.code}")
                extractCollectionError(body)?.let { throw Exception("BGG rejected sleeve marker: $it") }
                Log.i(TAG, "Sleeve marker saved: gameId=$gameId collid=${info.collectionId} state=$state")
                true
            }
        }

    // The Private Info fields other than the comment, named as both the save form and the JSON read name them.
    private val PRIVATE_INFO_FIELDS = listOf(
        "pricepaid", "pp_currency", "currvalue", "cv_currency", "quantity",
        "acquisitiondate", "acquiredfrom", "invdate", "invlocation"
    )

    private fun currentUserId(): String {
        val user = JSONObject(getJson("https://boardgamegeek.com/api/users/current"))
        return user.optString("userid").takeIf { user.optBoolean("loggedIn") && it.isNotBlank() && it != "0" }
            ?: throw Exception("Not logged in to BGG. Please check your BGG credentials in Settings.")
    }

    private fun getJson(url: String): String {
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw Exception("BGG request failed: HTTP ${response.code}")
        return body
    }

    private val MESSAGEBOX_ERROR = "messagebox error"

    private fun Boolean.asBggFlag(): String = if (this) "1" else "0"

    /** BGG answers some bad collection queries with HTTP 200 and an `<errors>` document. */
    private fun extractXmlApiError(xml: String): String? {
        if (!xml.contains("<errors")) return null
        return Regex("<message>(.*?)</message>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
            ?: "unknown error"
    }

    private fun extractCollectionError(body: String): String? {
        val trimmed = body.trim()
        // geekcollection.php reports failures as HTTP 200 with an HTML error box rather than
        // an error status, e.g. "You must login to use the collection utilities." for an
        // expired session, so a successful HTTP call is not on its own a successful write.
        if (trimmed.contains(MESSAGEBOX_ERROR, ignoreCase = true)) {
            val message = Regex(
                """<div[^>]*class=['"][^'"]*messagebox error[^'"]*['"][^>]*>(.*?)</div>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            ).find(trimmed)?.groupValues?.get(1)
                ?.replace(Regex("<[^>]+>"), "")
                ?.replace(Regex("""\s+"""), " ")
                ?.trim()
            return message?.takeIf { it.isNotBlank() } ?: "collection update was rejected"
        }
        if (!trimmed.startsWith("{")) return null
        val json = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null
        return sequenceOf("error", "errors", "message")
            .map { json.optString(it) }
            .firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    private fun extractCollectionId(body: String): String? {
        val trimmed = body.trim()
        if (trimmed.startsWith("{")) {
            runCatching { JSONObject(trimmed) }.getOrNull()?.let { json ->
                json.optString("collid").takeIf { it.isNotBlank() }?.let { return it }
                json.optJSONObject("item")?.optString("collid")?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return Regex("""collid["'\s:=]+(\d+)""").find(trimmed)?.groupValues?.get(1)
    }

    private fun parseCollectionEntry(xml: String): BggCollectionEntry? {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))
        var event = parser.eventType
        var collectionId: String? = null
        var seenItem = false
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "item" -> {
                        seenItem = true
                        collectionId = parser.getAttributeValue(null, "collid")?.takeIf { it.isNotBlank() }
                    }
                    "status" -> if (seenItem) {
                        fun flag(name: String) = parser.getAttributeValue(null, name) == "1"
                        return BggCollectionEntry(
                            collectionId = collectionId,
                            status = BggCollectionStatus(
                                own = flag("own"),
                                previouslyOwned = flag("prevowned"),
                                forTrade = flag("fortrade"),
                                wantInTrade = flag("want"),
                                wantToPlay = flag("wanttoplay"),
                                wantToBuy = flag("wanttobuy"),
                                wishlist = flag("wishlist"),
                                wishlistPriority = parser.getAttributeValue(null, "wishlistpriority")
                                    ?.toIntOrNull()?.coerceIn(1, 5) ?: 3,
                                preordered = flag("preordered")
                            )
                        )
                    }
                }
            }
            event = parser.next()
        }
        // An item without a <status> child still means the game is in the collection.
        return if (seenItem) BggCollectionEntry(collectionId, BggCollectionStatus()) else null
    }

    private fun parseCollectionResults(xml: String): List<BggGame> {
        val games = mutableListOf<BggGame>()
        val factory = XmlPullParserFactory.newInstance(); val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))
        var currentId: Int? = null; var currentName: String? = null; var currentYear: String? = null; var insideName = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> { currentId = parser.getAttributeValue(null, "objectid")?.toIntOrNull(); currentName = null; currentYear = null }
                    "name" -> { insideName = true }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim()
                    if (text?.isNotBlank() == true) {
                        if (insideName && currentName == null) currentName = text
                        else if (currentYear == null && text.toIntOrNull() != null) currentYear = text
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "name" -> insideName = false
                    "item" -> {
                        val parsedId = currentId
                        val parsedName = currentName
                        if (parsedId != null && parsedName != null) {
                            games.add(BggGame(id = parsedId, name = parsedName, yearPublished = currentYear, thumbnailUrl = null))
                        }
                        currentId = null; currentName = null; currentYear = null
                    }
                }
            }
            event = parser.next()
        }
        return games
    }
}
