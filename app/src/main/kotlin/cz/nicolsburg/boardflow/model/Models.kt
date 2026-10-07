package cz.nicolsburg.boardflow.model

data class BggGame(
    val id: Int,
    val name: String,
    val yearPublished: String?,
    val thumbnailUrl: String?
)

data class PlayerResult(
    val name: String,
    val score: String,
    val isWinner: Boolean,
    val color: String = "",
    val rating: String = "",
    val isNew: Boolean = false
)

data class ExtractedPlay(
    val players: List<PlayerResult>,
    val rawText: String,
    val date: String? = null,
    val detectedGameTitle: String? = null,
    val detectedGameConfidence: Float? = null,
    val detectedScoringCategories: List<String> = emptyList(),
    val gameDetectionEvidence: String? = null,
    val isMalformed: Boolean = false,
    val modelUsed: String? = null
)

/** A ranked match produced by GameRecognitionEngine against the local collection. */
data class GameCandidate(
    val game: BggGame,
    val score: Float,
    val matchReason: String,
    /** "title", "category-template", or "none" — which signal drove the score. */
    val primarySignal: String = "title",
    /** Number of saved template categories that matched detected categories. */
    val templateOverlap: Int = 0
)

data class RecommendationPick(
    val game: BggGame,
    val reason: String
)

data class RecommendationLane(
    val id: String,
    val title: String,
    val subtitle: String,
    val picks: List<RecommendationPick>
)

data class BggCredentials(
    val username: String,
    val password: String
)

/**
 * State for the collection-status editor. [loaded] means BGG has answered, so the editor can show
 * flags rather than a spinner; [inCollection] tells an existing entry apart from a new one, which
 * is what decides between updating and creating.
 */
data class CollectionStatusUiState(
    /** The game this state belongs to, so a previous game's status is never shown for another. */
    val gameId: Int? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val loaded: Boolean = false,
    val inCollection: Boolean = false,
    val collectionId: String? = null,
    val status: BggCollectionStatus = BggCollectionStatus(),
    val error: String? = null
)

/**
 * A collection status to store on the local snapshot: [entry] null means not in the collection.
 * [userEdit] is set for a save or removal from the editor, which also updates own / wishlist.
 */
data class CollectionStatusUpdate(
    val gameId: Int,
    val entry: BggCollectionEntry?,
    val userEdit: Boolean
)

/** A game's existing collection entry: its `collid` and the status flags currently set. */
data class BggCollectionEntry(
    val collectionId: String?,
    val status: BggCollectionStatus,
    /** The Private Info comment; null when the read did not include private info. */
    val privateComment: String? = null
)

/**
 * A collection entry's Private Info block. [fields] holds every field other than the comment,
 * keyed by its `geekcollection.php` form name: the block is saved as a whole, so they all have
 * to be posted back with a comment change.
 */
data class BggPrivateInfo(
    val collectionId: String,
    val fields: Map<String, String>,
    val comment: String
)

/**
 * Sleeve tracking is backed up to BGG as a `[sleeves:...]` token inside a collection entry's
 * private comment. The rest of the comment is the user's own text and is left untouched.
 */
object BggSleeveMarker {
    private val PATTERN = Regex("""\[sleeves:\s*([a-z-]+)\s*]""", RegexOption.IGNORE_CASE)

    fun parse(comment: String?): SleeveTrackingState =
        when (PATTERN.find(comment.orEmpty())?.groupValues?.get(1)?.lowercase()) {
            "sleeved" -> SleeveTrackingState.SLEEVED
            "to-sleeve" -> SleeveTrackingState.TO_SLEEVE
            "possible" -> SleeveTrackingState.POSSIBLE
            "no" -> SleeveTrackingState.NOT_SLEEVING
            else -> SleeveTrackingState.UNKNOWN
        }

    /** [comment] with its marker set to [state]; unchanged for [SleeveTrackingState.UNKNOWN]. */
    fun apply(comment: String, state: SleeveTrackingState): String {
        val token = when (state) {
            SleeveTrackingState.SLEEVED -> "[sleeves:sleeved]"
            SleeveTrackingState.TO_SLEEVE -> "[sleeves:to-sleeve]"
            SleeveTrackingState.POSSIBLE -> "[sleeves:possible]"
            SleeveTrackingState.NOT_SLEEVING -> "[sleeves:no]"
            SleeveTrackingState.UNKNOWN -> return comment
        }
        return when {
            PATTERN.containsMatchIn(comment) -> PATTERN.replaceFirst(comment, Regex.escapeReplacement(token))
            comment.isBlank() -> token
            else -> comment.trimEnd() + "\n" + token
        }
    }
}

/**
 * The eight status checkboxes BGG's own collection UI exposes. Listed here in the order and
 * wording the site uses, alongside the `geekcollection.php` form field each one posts as:
 *
 * | Property           | Form field   | BGG label     |
 * |--------------------|--------------|---------------|
 * | [own]              | `own`        | Own           |
 * | [previouslyOwned]  | `prevowned`  | Prev. Owned   |
 * | [forTrade]         | `fortrade`   | For Trade     |
 * | [wantToPlay]       | `wanttoplay` | Want to Play  |
 * | [wantInTrade]      | `want`       | Want in Trade |
 * | [wantToBuy]        | `wanttobuy`  | Want to Buy   |
 * | [preordered]       | `preordered` | Pre-ordered   |
 * | [wishlist]         | `wishlist`   | Wishlist      |
 *
 * Note that BGG's bare `want` field is "Want in Trade", not a general "want" - it is separate
 * from both [wantToBuy] and [wishlist]. [wishlistPriority] (1-5) only applies when [wishlist].
 */
data class BggCollectionStatus(
    val own: Boolean = false,
    val previouslyOwned: Boolean = false,
    val forTrade: Boolean = false,
    val wantInTrade: Boolean = false,
    val wantToPlay: Boolean = false,
    val wantToBuy: Boolean = false,
    val wishlist: Boolean = false,
    val wishlistPriority: Int = 3,
    val preordered: Boolean = false
)

/** The BGG labels for the flags currently set, in the order BGG's own UI lists them. */
fun BggCollectionStatus.activeLabels(): List<String> = buildList {
    if (own) add("Own")
    if (previouslyOwned) add("Prev. Owned")
    if (forTrade) add("For Trade")
    if (wantToPlay) add("Want to Play")
    if (wantInTrade) add("Want in Trade")
    if (wantToBuy) add("Want to Buy")
    if (preordered) add("Pre-ordered")
    if (wishlist) add("Wishlist")
}

/**
 * The collection status sync writes each game's BGG status into [GameItem.bggValues] under these
 * keys (the xmlapi2 attribute names). [SYNCED] marks a record whose status came from that sync, so
 * a blank [COLLID] then really means "not in the collection" rather than "never read".
 */
private object CollectionStatusKeys {
    const val SYNCED = "collectionstatussynced"
    const val COLLID = "collid"
    const val OWN = "own"
    const val PREV_OWNED = "prevowned"
    const val FOR_TRADE = "fortrade"
    const val WANT = "want"
    const val WANT_TO_PLAY = "wanttoplay"
    const val WANT_TO_BUY = "wanttobuy"
    const val WISHLIST = "wishlist"
    const val WISHLIST_PRIORITY = "wishlistpriority"
    const val PREORDERED = "preordered"
    const val SLEEVE_MARKER = "sleevemarker"
}

/** The sleeve state last seen in (or written to) this game's BGG private comment. */
val GameItem.syncedSleeveMarker: SleeveTrackingState
    get() = runCatching { SleeveTrackingState.valueOf(bggValues[CollectionStatusKeys.SLEEVE_MARKER].orEmpty()) }
        .getOrDefault(SleeveTrackingState.UNKNOWN)

fun GameItem.withSyncedSleeveMarker(state: SleeveTrackingState): GameItem =
    copy(sources = sources.copy(bggValues = bggValues + (CollectionStatusKeys.SLEEVE_MARKER to state.name)))

/** The sleeve tracking state the app shows, kept in the spreadsheet's `sleeved` column value. */
val GameItem.sleeveTracking: SleeveTrackingState
    get() = SleeveTrackingState.fromSheetValue(
        spreadsheetValues.entries.firstOrNull { (key, _) -> key.equals("sleeved", ignoreCase = true) }?.value
    )

/** True once the collection status sync has recorded this game's status. */
val GameItem.hasSyncedCollectionStatus: Boolean
    get() = bggValues[CollectionStatusKeys.SYNCED] == "1"

/** The synced collection entry, or null when the game is not in the collection (or was never synced). */
fun GameItem.syncedCollectionEntry(): BggCollectionEntry? {
    if (!hasSyncedCollectionStatus) return null
    val values = bggValues
    val collectionId = values[CollectionStatusKeys.COLLID]?.takeIf { it.isNotBlank() } ?: return null
    fun flag(key: String) = values[key] == "1"
    return BggCollectionEntry(
        collectionId = collectionId,
        status = BggCollectionStatus(
            own = flag(CollectionStatusKeys.OWN),
            previouslyOwned = flag(CollectionStatusKeys.PREV_OWNED),
            forTrade = flag(CollectionStatusKeys.FOR_TRADE),
            wantInTrade = flag(CollectionStatusKeys.WANT),
            wantToPlay = flag(CollectionStatusKeys.WANT_TO_PLAY),
            wantToBuy = flag(CollectionStatusKeys.WANT_TO_BUY),
            wishlist = flag(CollectionStatusKeys.WISHLIST),
            wishlistPriority = values[CollectionStatusKeys.WISHLIST_PRIORITY]?.toIntOrNull()?.coerceIn(1, 5) ?: 3,
            preordered = flag(CollectionStatusKeys.PREORDERED)
        )
    )
}

/**
 * Records [entry] as this game's synced collection status; null means not in the collection.
 * With [mirrorOwnership], own and wishlist are also copied into [GameItem.ownership] so the shelf
 * filters follow a status edit. The sync leaves ownership to the owned/wishlist fetches, which also
 * keeps games tracked only in the spreadsheet from being flipped to not owned.
 */
fun GameItem.withSyncedCollectionEntry(entry: BggCollectionEntry?, mirrorOwnership: Boolean = false): GameItem {
    val status = entry?.status ?: BggCollectionStatus()
    fun Boolean.flag() = if (this) "1" else "0"
    val values = bggValues + mapOf(
        CollectionStatusKeys.SYNCED to "1",
        CollectionStatusKeys.COLLID to entry?.collectionId.orEmpty(),
        CollectionStatusKeys.OWN to status.own.flag(),
        CollectionStatusKeys.PREV_OWNED to status.previouslyOwned.flag(),
        CollectionStatusKeys.FOR_TRADE to status.forTrade.flag(),
        CollectionStatusKeys.WANT to status.wantInTrade.flag(),
        CollectionStatusKeys.WANT_TO_PLAY to status.wantToPlay.flag(),
        CollectionStatusKeys.WANT_TO_BUY to status.wantToBuy.flag(),
        CollectionStatusKeys.WISHLIST to status.wishlist.flag(),
        CollectionStatusKeys.WISHLIST_PRIORITY to status.wishlistPriority.toString(),
        CollectionStatusKeys.PREORDERED to status.preordered.flag()
    ).let { synced ->
        // A null comment means private info was not read, so the last known marker stands.
        val comment = entry?.privateComment
        when {
            entry == null -> synced - CollectionStatusKeys.SLEEVE_MARKER
            comment == null -> synced
            else -> synced + (CollectionStatusKeys.SLEEVE_MARKER to BggSleeveMarker.parse(comment).name)
        }
    }
    return copy(
        ownership = if (mirrorOwnership) ownership.copy(isOwned = status.own, isWishlisted = status.wishlist) else ownership,
        sources = sources.copy(bggValues = values)
    )
}

data class SessionMemory(
    val moods: List<String> = emptyList(),
    val momentType: String = "",
    val note: String = "",
    val quote: String = "",
    val chronicleLine: String = "",
    val chronicleSourceKey: String = "",
    val chronicleCreatedAt: Long? = null
)

data class LoggedPlay(
    val id: String,
    val gameId: Int,
    val gameName: String,
    val date: String,
    val playedAt: Long? = null,
    val sessionId: String? = null,
    val players: List<PlayerResult>,
    val durationMinutes: Int,
    val location: String,
    val postedToBgg: Boolean,
    val comments: String = "",
    val quantity: Int = 1,
    val incomplete: Boolean = false,
    val nowInStats: Boolean = true,
    val memory: SessionMemory? = null
)

data class PlaySession(
    val id: String,
    val startedAt: Long,
    val endedAt: Long,
    val sessionDate: String,
    val location: String,
    val title: String = ""
)

data class SessionHubWinner(
    val playerName: String,
    val wins: Int
)

data class SessionHub(
    val anchorPlayId: String,
    val sessionId: String?,
    val title: String?,
    val date: String,
    val location: String,
    val plays: List<LoggedPlay>,
    val totalLoggedPlays: Int,
    val uniqueGames: Int,
    val uniquePlayerNames: List<String>,
    val totalDurationMinutes: Int,
    val winners: List<SessionHubWinner>,
    val moods: List<String>,
    val quotes: List<String>
) {
    val anchorPlay: LoggedPlay
        get() = plays.firstOrNull { it.id == anchorPlayId } ?: plays.first()
}

fun List<LoggedPlay>.deriveSessionHub(anchor: LoggedPlay, sessionTitle: String? = null): SessionHub {
    val explicitSessionId = anchor.sessionId?.takeIf { it.isNotBlank() }
    val anchorDate = anchor.date
    val anchorLocation = anchor.location.normalizeSessionLocation()
    val anchorPlayers = anchor.normalizedSessionPlayers()

    val sessionPlays = filter { play ->
        when {
            explicitSessionId != null -> play.sessionId == explicitSessionId
            else -> play.date == anchorDate && play.belongsToDerivedSession(
                anchorId = anchor.id,
                anchorLocation = anchorLocation,
                anchorPlayers = anchorPlayers
            )
        }
    }.ifEmpty { listOf(anchor) }

    val uniquePlayerNames = linkedSetOf<String>().apply {
        sessionPlays.forEach { play ->
            play.players.forEach { player ->
                val trimmed = player.name.trim()
                if (trimmed.isNotBlank()) add(trimmed)
            }
        }
    }.toList()

    val winnerCounts = linkedMapOf<String, Int>()
    sessionPlays.forEach { play ->
        play.players.filter { it.isWinner }.forEach { winner ->
            val name = winner.name.trim()
            if (name.isNotBlank()) {
                winnerCounts[name] = (winnerCounts[name] ?: 0) + 1
            }
        }
    }

    val moods = linkedSetOf<String>().apply {
        sessionPlays.forEach { play ->
            play.memory?.moods?.forEach { mood ->
                val trimmed = mood.trim()
                if (trimmed.isNotBlank()) add(trimmed)
            }
        }
    }.toList()

    val quotes = linkedSetOf<String>().apply {
        sessionPlays.forEach { play ->
            play.memory?.quote?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
        }
    }.toList()

    return SessionHub(
        anchorPlayId = anchor.id,
        sessionId = explicitSessionId,
        title = sessionTitle?.trim()?.ifBlank { null },
        date = anchorDate,
        location = anchor.location.trim(),
        plays = sessionPlays,
        totalLoggedPlays = sessionPlays.sumOf { it.quantity.coerceAtLeast(1) },
        uniqueGames = sessionPlays.map { it.gameId }.distinct().size,
        uniquePlayerNames = uniquePlayerNames,
        totalDurationMinutes = sessionPlays.sumOf { it.durationMinutes.coerceAtLeast(0) },
        winners = winnerCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .map { SessionHubWinner(playerName = it.key, wins = it.value) },
        moods = moods,
        quotes = quotes
    )
}

private fun LoggedPlay.belongsToDerivedSession(
    anchorId: String,
    anchorLocation: String,
    anchorPlayers: Set<String>
): Boolean {
    if (id == anchorId) return true

    val playPlayers = normalizedSessionPlayers()
    val sharedPlayers = playPlayers.intersect(anchorPlayers).size
    val playerThreshold = when {
        anchorPlayers.isEmpty() || playPlayers.isEmpty() -> 0
        anchorPlayers.size == 1 || playPlayers.size == 1 -> 1
        else -> 2
    }

    val playLocation = location.normalizeSessionLocation()
    val bothHaveLocation = anchorLocation.isNotBlank() && playLocation.isNotBlank()

    return when {
        bothHaveLocation -> playLocation == anchorLocation && sharedPlayers >= playerThreshold
        anchorLocation.isBlank() && playLocation.isBlank() -> sharedPlayers >= playerThreshold
        else -> false
    }
}

private fun LoggedPlay.normalizedSessionPlayers(): Set<String> =
    players.mapNotNull { player ->
        player.name.trim().lowercase().takeIf { it.isNotBlank() }
    }.toSet()

private fun String.normalizeSessionLocation(): String =
    trim().lowercase()

fun String.trimMemorySuffix(): String {
    fun String.isMemoryLine() = startsWith("\$\$mood:") || startsWith("\$\$quote:") || isBlank()
    val idx = lastIndexOf("\n\n")
    if (idx == -1) {
        return if (lines().all { it.isMemoryLine() }) "" else trimEnd()
    }
    val suffix = substring(idx + 2)
    return if (suffix.lines().all { it.isMemoryLine() }) substring(0, idx).trimEnd() else trimEnd()
}

@Deprecated("Use trimMemorySuffix", ReplaceWith("trimMemorySuffix()"))
fun String.trimMoodSuffix(): String = trimMemorySuffix()

enum class StatsPlayScope(val label: String, val description: String) {
    ALL_PLAYS("All logged plays", "Every saved play appears in History stats."),
    COUNTED_ONLY("Counted plays only", "Only plays marked Count in stats are included.")
}

enum class SleeveTrackingState(val sheetValue: String) {
    SLEEVED("TRUE"),
    TO_SLEEVE("!"),
    POSSIBLE("?"),
    NOT_SLEEVING("FALSE"),
    UNKNOWN("");

    companion object {
        fun fromSheetValue(raw: String?): SleeveTrackingState {
            return when (raw?.trim()?.lowercase()) {
                "1", "1.0", "true", "yes", "y" -> SLEEVED
                "!", "to sleeve", "tosleeve" -> TO_SLEEVE
                "?", "possible", "possible to sleeve" -> POSSIBLE
                "0", "0.0", "false", "no", "n" -> NOT_SLEEVING
                else -> UNKNOWN
            }
        }
    }
}

data class Player(
    val id: String,
    val displayName: String,
    val aliases: List<String>,
    val bggUsername: String = "",
    val lastPlayedAt: Long? = null,
    val isHidden: Boolean = false,
    /** Default avatar colour as "#RRGGBB"; blank means the automatic colour picked from the name. */
    val color: String = ""
)

data class GameRelations(
    val isExpansion: Boolean,
    val baseGames: List<BggGame>,
    val expansions: List<BggGame>
)

/**
 * Merged collection record built from spreadsheet values plus live BGG enrichment.
 *
 * Canonical play count is always BGG numplays.
 */
data class GameItem(
    val identity: Identity,
    val stats: Stats,
    val players: Players,
    val ownership: Ownership,
    val sleeves: Sleeves,
    val media: Media,
    val links: Links,
    val sources: Sources,
    val lastCachedAt: Long = System.currentTimeMillis()
) {
    data class Identity(
        val objectId: String,
        val name: String
    )

    data class Stats(
        val rank: Int?,
        val averageRating: Double?,
        val bayesAverage: Double?,
        val weight: Double?,
        val yearPublished: Int?,
        val playingTime: Int?,
        val minPlayTime: Int?,
        val maxPlayTime: Int?,
        val numOwned: Int?,
        val languageDependence: String?,
        val language: String?
    )

    data class Players(
        val minPlayers: Int?,
        val maxPlayers: Int?,
        val bestPlayers: String?,
        val recommendedPlayers: String?,
        val notRecommendedPlayers: String?,
        val recommendedAge: String?
    )

    data class Ownership(
        val isOwned: Boolean,
        val isWishlisted: Boolean,
        /**
         * Canonical play count from BGG numplays.
         */
        val bggPlayCount: Int?
    )

    data class Sleeves(
        val status: SleeveStatus = SleeveStatus.UNKNOWN,
        val cardSets: List<CardSet> = emptyList(),
        val sourceUrl: String? = null,
        val note: String? = null,
        val lastFetchedAt: Long? = null
    ) {
        data class CardSet(
            val label: String,
            val count: Int?,
            val size: String?,
            val notes: String? = null
        )
    }

    enum class SleeveStatus {
        UNKNOWN,
        FOUND,
        MISSING,
        ERROR
    }

    data class Media(
        val thumbnailUrl: String?
    )

    data class Links(
        val bggUrl: String?,
        val driveUrl: String?,
        val qrImageUrl: String?
    )

    data class Sources(
        val spreadsheetValues: Map<String, String>,
        val bggValues: Map<String, String>
    )

    val name: String get() = identity.name
    val objectId: String get() = identity.objectId
    val rank: Int? get() = stats.rank
    val rating: Double? get() = stats.averageRating
    val bayesAverage: Double? get() = stats.bayesAverage
    val weight: Double? get() = stats.weight
    val yearPublished: Int? get() = stats.yearPublished
    val playingTime: Int? get() = stats.playingTime
    val minPlayTime: Int? get() = stats.minPlayTime
    val maxPlayTime: Int? get() = stats.maxPlayTime
    val numOwned: Int? get() = stats.numOwned
    val languageDependence: String? get() = stats.languageDependence
    val language: String? get() = stats.language
    val minPlayers: Int? get() = players.minPlayers
    val maxPlayers: Int? get() = players.maxPlayers
    val bestPlayers: String? get() = players.bestPlayers
    val recommendedPlayers: String? get() = players.recommendedPlayers
    val notRecommendedPlayers: String? get() = players.notRecommendedPlayers
    val recommendedAge: String? get() = players.recommendedAge
    val isOwned: Boolean get() = ownership.isOwned
    val isWishlisted: Boolean get() = ownership.isWishlisted
    val numPlays: Int? get() = ownership.bggPlayCount
    val sleeveStatus: SleeveStatus get() = sleeves.status
    val sleeveCardSets: List<Sleeves.CardSet> get() = sleeves.cardSets
    val sleeveSourceUrl: String? get() = sleeves.sourceUrl
    val sleeveNote: String? get() = sleeves.note
    val sleevesLastFetchedAt: Long? get() = sleeves.lastFetchedAt
    val thumbnailUrl: String? get() = media.thumbnailUrl
    val bggUrl: String? get() = links.bggUrl
    val shareUrl: String? get() = links.driveUrl
    val qrImageUrl: String? get() = links.qrImageUrl
    val spreadsheetValues: Map<String, String> get() = sources.spreadsheetValues
    val bggValues: Map<String, String> get() = sources.bggValues

    fun withSleeves(sleeves: Sleeves): GameItem =
        copy(sleeves = sleeves)

    fun withSpreadsheetValue(key: String, value: String): GameItem =
        copy(
            sources = sources.copy(
                spreadsheetValues = sources.spreadsheetValues + (key to value)
            )
        )
}

data class SpreadsheetDetails(
    val id: String,
    val title: String,
    val firstSheetTitle: String,
    val webViewUrl: String? = null
)

data class SessionContext(
    val sessionId: String,
    val gameId: Int,
    val gameName: String,
    val players: List<PlayerResult>,
    val location: String,
    val title: String = "",
    val startedAt: Long,
    val lastPlayTimestamp: Long
) {
    fun isActive(): Boolean =
        System.currentTimeMillis() - lastPlayTimestamp < 6L * 60 * 60 * 1000

    fun isRecent(): Boolean =
        System.currentTimeMillis() - lastPlayTimestamp < 60L * 60 * 1000
}

enum class InsightRarity(val label: String, val sortWeight: Int) {
    COMMON("Moment", 0),
    NOTABLE("Notable", 1),
    RARE("Landmark", 2),
    EPIC("Chronicle", 3),
    LEGENDARY("Legacy", 4)
}

sealed class RecordMoment {
    data class FirstWin(val playerName: String, val gameName: String) : RecordMoment()
    data class NewHighScore(val playerName: String, val gameName: String) : RecordMoment()
    data class WinStreak(val playerName: String, val streakLength: Int) : RecordMoment()

    val rarity: InsightRarity
        get() = when (this) {
            is FirstWin    -> InsightRarity.NOTABLE
            is NewHighScore -> InsightRarity.NOTABLE
            is WinStreak   -> when {
                streakLength >= 7 -> InsightRarity.EPIC
                streakLength >= 4 -> InsightRarity.RARE
                else              -> InsightRarity.NOTABLE
            }
        }

    /** Emoji version — used in the post-log PostSaveCard celebration beat. */
    val displayText: String
        get() = when (this) {
            is FirstWin    -> "🎉 $playerName finally wins one."
            is NewHighScore -> "🏆 A new personal best for $playerName. Log it."
            is WinStreak   -> "🔥 $playerName has won ${streakLength} in a row. The table has noticed."
        }

    /** No-emoji version — used in historical insight strips (PlayDetailsDialog). */
    val stripText: String
        get() = when (this) {
            is FirstWin    -> "$playerName wins it for the first time."
            is NewHighScore -> "New personal best for $playerName."
            is WinStreak   -> "$playerName is on a ${streakLength}-win streak."
        }
}

data class PlayerRecognitionHint(
    val scannedNameNormalized: String,
    val confirmedRosterPlayerId: String,
    val playerDisplayName: String,
    val timesConfirmed: Int,
    val lastConfirmedAt: Long
)

data class GameRecognitionHint(
    val gameObjectId: String,
    val gameName: String,
    val normalizedTitles: List<String>,
    val normalizedCategories: List<String>,
    val confirmedAt: Long,
    val timesConfirmed: Int
)

sealed class ScanRecognitionResult {
    /** Auto-switch happened; game was confirmed without user interaction. */
    data class AutoSwitched(val gameName: String) : ScanRecognitionResult()
    /** Gemini detected a title but it was not found in the local collection. */
    data class NoCollectionMatch(val detectedTitle: String) : ScanRecognitionResult()
    /** Gemini could not detect the game, or confidence was too low. */
    object LowConfidence : ScanRecognitionResult()
}

enum class ChallengeType(val label: String) {
    PLAY_N_TIMES("Play N plays"),
    PLAY_SPECIFIC_GAME("Play a game N times"),
    PLAY_N_DISTINCT("Play N different games"),
    PLAYER_WIN_STREAK("Reach a player win streak"),
    PLAY_WITH_GROUP_N_TIMES("Play with a group N times"),
    PLAY_STREAK("Stay consistent week over week"),
    PLAY_N_UNPLAYED("Play unplayed owned games")
}

enum class ChallengeStatus {
    ACTIVE,
    PAUSED,
    ARCHIVED
}

data class Challenge(
    val id: String,
    val title: String,
    val type: ChallengeType,
    val targetCount: Int,
    val gameId: Int? = null,
    val gameName: String? = null,
    val playerIds: List<String> = emptyList(),
    val playerNames: List<String> = emptyList(),
    val startDate: String? = null,
    val endDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val streakPeriod: String? = null,
    val status: ChallengeStatus = ChallengeStatus.ACTIVE
)

data class ChallengeProgress(
    val challenge: Challenge,
    val currentCount: Int,
    val goalCount: Int,
    val remainingText: String? = null,
    val countedGameNames: List<String> = emptyList()
) {
    val isComplete: Boolean get() = currentCount >= goalCount
    val isFailed: Boolean get() {
        if (isComplete) return false
        val end = challenge.endDate ?: return false
        return runCatching { java.time.LocalDate.parse(end) }.getOrNull()
            ?.isBefore(java.time.LocalDate.now()) == true
    }
    val isArchived: Boolean get() = challenge.status == ChallengeStatus.ARCHIVED
    val isPaused: Boolean get() = challenge.status == ChallengeStatus.PAUSED
    val isActive: Boolean get() = challenge.status == ChallengeStatus.ACTIVE && !isComplete && !isFailed
    val fraction: Float
        get() = (currentCount.toFloat() / goalCount.coerceAtLeast(1)).coerceIn(0f, 1f)
}

data class LogPlayPrefill(
    val location: String,
    val durationSuggestion: String = ""
)

data class PlayTimer(
    val startedAt: Long,
    val gameId: Int? = null,
    val gameName: String = "",
)

data class LogEntry(
    val name: String,
    val status: String,
    val type: Type
) {
    enum class Type { HEADER, UPDATED, INSERTED, DONE, ERROR, INFO }

    val icon: String
        get() = when (type) {
            Type.HEADER -> "list"
            Type.UPDATED -> "sync"
            Type.INSERTED -> "+"
            Type.DONE -> "done"
            Type.ERROR -> "error"
            Type.INFO -> "info"
        }
}

/** A game known only from a BoardGameGeek search (not in the collection): its id, name and cover. */
fun bggOnlyGameItem(id: Int, name: String, thumbnailUrl: String? = null): GameItem = GameItem(
    identity = GameItem.Identity(objectId = id.toString(), name = name),
    stats = GameItem.Stats(null, null, null, null, null, null, null, null, null, null, null),
    players = GameItem.Players(null, null, null, null, null, null),
    ownership = GameItem.Ownership(isOwned = false, isWishlisted = false, bggPlayCount = null),
    sleeves = GameItem.Sleeves(),
    media = GameItem.Media(thumbnailUrl = thumbnailUrl),
    links = GameItem.Links(bggUrl = "https://boardgamegeek.com/boardgame/$id", driveUrl = null, qrImageUrl = null),
    sources = GameItem.Sources(spreadsheetValues = emptyMap(), bggValues = emptyMap())
)
