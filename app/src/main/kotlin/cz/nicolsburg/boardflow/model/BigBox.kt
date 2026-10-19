package cz.nicolsburg.boardflow.model

/**
 * A big box ("Catan: Big Box", "Carcassonne Big Box 6", "Dominion (Second Edition) Big Box")
 * holds its base game, so BoardFlow treats it as a base game: never as an expansion, and the
 * expansions of the game it holds are its expansions too. BGG links an expansion to the
 * original base game only (Explorers & Pirates expands Catan, not Catan: Big Box), so the
 * link is made from the names.
 */
object BigBox {

    fun isBigBox(name: String): Boolean = name.contains("big box", ignoreCase = true)

    /**
     * The base game names a big box stands for: the name before "Big Box", and that name without
     * an edition in brackets ("Dominion (Second Edition) Big Box" -> "Dominion (Second Edition)",
     * "Dominion"). Empty for a game that is not a big box.
     */
    fun roots(name: String): List<String> {
        val at = name.indexOf("big box", ignoreCase = true)
        if (at <= 0) return emptyList()
        val root = name.substring(0, at).trimEnd(*SEPARATOR_CHARS)
        if (root.isEmpty()) return emptyList()
        val withoutEdition = root.replace(EDITION, "").trimEnd(*SEPARATOR_CHARS)
        return listOf(root, withoutEdition).filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
    }

    /** True when [gameName] is the base game a big box holds ("Catan" for "Catan: Big Box"). */
    fun holds(bigBoxName: String, gameName: String): Boolean =
        roots(bigBoxName).any { it.equals(gameName.trim(), ignoreCase = true) }

    /** True when [expansionName] names an expansion of the base game [bigBoxName] holds. */
    fun covers(bigBoxName: String, expansionName: String): Boolean =
        !isBigBox(expansionName) && roots(bigBoxName).any { ExpansionPlays.extendsName(expansionName, it) }

    private val SEPARATOR_CHARS = charArrayOf(':', ' ', '–', '—', '-')
    private val EDITION = Regex("""\s*\([^)]*\)\s*$""")
}
