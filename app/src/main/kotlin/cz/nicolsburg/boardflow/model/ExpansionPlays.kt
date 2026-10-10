package cz.nicolsburg.boardflow.model

/** An expansion played as part of a base-game play. */
data class PlayedExpansion(val playId: String, val gameId: Int, val gameName: String)

/**
 * BGG has no "base game plus expansion" play, so an expansion is logged as a play of its own
 * next to the base-game play (BoardFlow does this, and so does every app that syncs to BGG).
 * Both plays describe one sitting. [link] finds those pairs: it marks the expansion play with
 * [LoggedPlay.expansionOf] and lists it in the base play's [LoggedPlay.expansions], so
 * session-level views (Journal, stats, challenges, widgets) can count the sitting once through
 * [sessionPlays], while per-game counts still see the expansion play.
 *
 * Two plays belong to the same sitting when they share the date, location, length, quantity,
 * incomplete flag and players with the same scores and wins. Within a sitting a play is an
 * expansion when its game is a known expansion ([expansionGameIds], from the collection's BGG
 * type) or its name extends another game's name there ("Wingspan: European Expansion" next to
 * "Wingspan"). An expansion attaches to the base game its name extends, otherwise to the only
 * base game of the sitting; when neither exists it stays a play of its own.
 */
object ExpansionPlays {

    fun link(plays: List<LoggedPlay>, expansionGameIds: Set<Int> = emptySet()): List<LoggedPlay> {
        val clean = plays.map {
            if (it.expansionOf == null && it.expansions.isEmpty()) it else it.copy(expansionOf = null, expansions = emptyList())
        }
        val baseOf = mutableMapOf<String, String>()
        clean.groupBy { it.sittingKey() }.values
            .filter { group -> group.size > 1 && group.map { it.gameId }.distinct().size > 1 }
            .forEach { group -> attachWithinSitting(group, expansionGameIds, baseOf) }
        if (baseOf.isEmpty()) return clean

        val expansionsByBase = clean.filter { it.id in baseOf }
            .groupBy({ baseOf.getValue(it.id) }) { PlayedExpansion(it.id, it.gameId, it.gameName) }
        return clean.map { play ->
            when {
                play.id in baseOf -> play.copy(expansionOf = baseOf.getValue(play.id))
                play.id in expansionsByBase -> play.copy(expansions = expansionsByBase.getValue(play.id).sortedBy { it.gameName.lowercase() })
                else -> play
            }
        }
    }

    private fun attachWithinSitting(group: List<LoggedPlay>, expansionGameIds: Set<Int>, baseOf: MutableMap<String, String>) {
        // Two sessions that happen to match (different session ids) are not one sitting.
        val sessionIds = group.mapNotNull { it.sessionId?.takeIf(String::isNotBlank) }.distinct()
        if (sessionIds.size > 1) return
        // Names that extend each other ("Tainted Grail: The Fall of Avalon" and "Tainted Grail:
        // The Last Knight") say nothing on their own; only the BGG type decides those.
        fun isExpansion(play: LoggedPlay) = play.gameId in expansionGameIds ||
            group.any { other ->
                other.gameId != play.gameId && extendsName(play.gameName, other.gameName) && !extendsName(other.gameName, play.gameName)
            }
        val (expansions, bases) = group.partition(::isExpansion)
        if (bases.isEmpty()) return
        expansions.forEach { expansion ->
            // Without a name link only plays logged together (one session id) are trusted: bulk
            // "played" entries share a date and an empty result but are not one sitting.
            val loggedTogether = sessionIds.size == 1 && group.all { it.sessionId == sessionIds.single() }
            val base = bases.firstOrNull { extendsName(expansion.gameName, it.gameName) }
                ?: bases.singleOrNull()?.takeIf { loggedTogether }
                ?: return@forEach
            baseOf[expansion.id] = base.id
        }
    }

    /**
     * True when [candidate] names an expansion of [base]: "Root: Sub" or "Root Sub" where Root
     * is the base name or the part of it before its own subtitle separator.
     */
    fun extendsName(candidate: String, base: String): Boolean {
        val c = candidate.trim()
        val b = base.trim()
        if (c.isEmpty() || b.isEmpty() || c.equals(b, ignoreCase = true)) return false
        val candidateRoot = separatorIndex(c)?.let { c.substring(0, it).trim() }
            ?: return c.lowercase().startsWith(b.lowercase() + " ")
        val baseRoot = separatorIndex(b)?.let { b.substring(0, it).trim() }
        return candidateRoot.equals(b, ignoreCase = true) || candidateRoot.equals(baseRoot, ignoreCase = true)
    }

    /** The expansion's name without its base game's name: "Wingspan: European Expansion" -> "European Expansion". */
    fun shortName(expansionName: String, baseName: String): String {
        val name = expansionName.trim()
        val sep = separatorIndex(name) ?: return name.removePrefixIgnoreCase(baseName.trim() + " ").ifBlank { name }
        val root = name.substring(0, sep).trim()
        val baseRoot = separatorIndex(baseName.trim())?.let { baseName.trim().substring(0, it).trim() }
        return if (root.equals(baseName.trim(), ignoreCase = true) || root.equals(baseRoot, ignoreCase = true)) {
            name.substring(sep).trimStart(':', ' ', '–', '—', '-').ifBlank { name }
        } else name
    }

    private fun String.removePrefixIgnoreCase(prefix: String): String =
        if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this

    private fun separatorIndex(s: String): Int? = listOf(
        s.indexOf(':').takeIf { it > 0 },
        s.indexOf(" – ").takeIf { it > 0 },
        s.indexOf(" — ").takeIf { it > 0 },
        s.indexOf(" - ").takeIf { it > 0 }
    ).filterNotNull().minOrNull()

    private fun LoggedPlay.sittingKey(): String = listOf(
        date,
        location.trim().lowercase(),
        durationMinutes.toString(),
        quantity.toString(),
        incomplete.toString(),
        players.joinToString("|") { "${it.name.trim().lowercase()}~${it.score.trim()}~${it.isWinner}" }
    ).joinToString("||")
}

/** One play per sitting: expansion plays logged next to their base game are left out. */
fun List<LoggedPlay>.sessionPlays(): List<LoggedPlay> = filter { it.expansionOf == null }

/** True when this play is of [gameId] or included it as an expansion. */
fun LoggedPlay.includesGame(gameId: Int?): Boolean = gameId != null && (this.gameId == gameId || expansions.any { it.gameId == gameId })

/** BGG ids of the collection entries BGG lists as expansions. */
fun List<GameItem>.expansionGameIds(): Set<Int> = mapNotNullTo(hashSetOf()) { item ->
    val type = item.spreadsheetValues["objecttype"] ?: item.bggValues["objecttype"]
    if (type?.trim().equals("boardgameexpansion", ignoreCase = true)) item.objectId.toIntOrNull() else null
}
