package cz.nicolsburg.boardflow.model

/**
 * An expansion play with no base-game play of its sitting (see [ExpansionPlays]).
 *
 * When the base game was logged the same day with the same players but no result yet (no
 * scores, no winner: [existingBasePlay]), the fix is to give that play the expansion play's
 * result, so the two become one sitting. A base play with a result of its own is a different
 * sitting and is never overwritten. Otherwise the fix is a new play of one of [baseOptions] with the
 * expansion play's date, players, scores, length and location.
 */
data class BasePlayFix(
    val expansionPlay: LoggedPlay,
    val baseOptions: List<BggGame>,
    val suggestedBase: BggGame?,
    val existingBasePlay: LoggedPlay?,
    /** True when nothing is left to choose: a base is suggested or an existing base play is matched. */
    val selectedByDefault: Boolean
)

object BasePlayFixes {

    /**
     * @param plays history with expansion plays already linked ([ExpansionPlays.link])
     * @param baseGamesOf for each expansion game id, the games BGG says it expands
     * @param knownGameIds games the user owns or has played, used to pick a base when BGG lists several
     */
    fun find(plays: List<LoggedPlay>, baseGamesOf: Map<Int, List<BggGame>>, knownGameIds: Set<Int>): List<BasePlayFix> =
        plays.mapNotNull { play ->
            // Bulk "played" marks (one player, no result, no length) are not sittings.
            if (play.expansionOf != null || !play.looksLikeASitting()) return@mapNotNull null
            val options = baseGamesOf[play.gameId].orEmpty()
            if (options.isEmpty()) return@mapNotNull null
            val optionIds = options.mapTo(hashSetOf()) { it.id }
            val existing = plays.firstOrNull { other ->
                other.id != play.id && other.expansionOf == null && other.gameId in optionIds &&
                    other.date == play.date && other.playerNames() == play.playerNames() && !other.hasResult()
            }
            val suggested = if (existing != null) null else suggestBase(play.gameName, options, knownGameIds)
            BasePlayFix(
                expansionPlay = play,
                baseOptions = options,
                suggestedBase = suggested,
                existingBasePlay = existing,
                selectedByDefault = existing != null || suggested != null
            )
        }.sortedByDescending { it.expansionPlay.date }

    /** The base game to preselect, or null when the choice is the user's. */
    fun suggestBase(expansionName: String, options: List<BggGame>, knownGameIds: Set<Int>): BggGame? {
        options.singleOrNull()?.let { return it }
        val named = options.filter { ExpansionPlays.extendsName(expansionName, it.name) }
        val known = options.filter { it.id in knownGameIds }
        return named.singleOrNull()
            ?: known.singleOrNull()
            ?: named.filter { it.id in knownGameIds }.singleOrNull()
    }

    /** A new, unposted play of [base] for the sitting of [expansionPlay]. */
    fun basePlayFor(expansionPlay: LoggedPlay, base: BggGame, newId: String): LoggedPlay = expansionPlay.copy(
        id = newId,
        gameId = base.id,
        gameName = base.name,
        postedToBgg = false,
        comments = "",
        nowInStats = true,
        memory = null,
        expansionOf = null,
        expansions = emptyList()
    )

    private fun LoggedPlay.playerNames(): Set<String> = players.mapTo(hashSetOf()) { it.name.trim().lowercase() }

    private fun LoggedPlay.hasResult(): Boolean =
        players.any { it.isWinner || (it.score.isNotBlank() && it.score != "0") }

    private fun LoggedPlay.looksLikeASitting(): Boolean =
        players.size > 1 || durationMinutes > 0 || players.any { it.score.isNotBlank() && it.score != "0" }
}
