package cz.nicolsburg.boardflow.model

/**
 * An end-of-game score pad for one game: categories as rows, players as columns, like the
 * printed pad in the box. Each game has its own sheet (`data/scoresheet/ScoreSheets.kt`) and its
 * own look (`ui/score/`). Games scored during play (Azul) have no sheet: their total is typed on
 * the play form as before.
 *
 * Pure data and maths, no Android: [ScoreSheetScoring] turns typed values into totals and winners.
 */
data class ScoreSheet(
    val id: String,
    val gameName: String,
    /** The base game first; every id here (editions, standalones, expansions) opens this sheet. */
    val gameIds: Set<Int>,
    val maxPlayers: Int,
    val modules: List<ScoreModule> = emptyList(),
    val rows: List<ScoreRow>,
    /** Asked one at a time, only while players are tied for the win. None left: a shared win. */
    val tiebreakers: List<ScoreTiebreaker> = emptyList(),
    val winRule: WinRule = WinRule.HIGHEST
) {
    fun rowsFor(enabledModules: Set<String>): List<ScoreRow> =
        rows.filter { it.module == null || it.module in enabledModules }

    /** Modules switched on because the game or one of its logged expansions is that module. */
    fun modulesFor(gameIds: Collection<Int>): Set<String> =
        modules.filter { m -> m.bggIds.any { it in gameIds } }.mapTo(linkedSetOf()) { it.id }
}

data class ScoreModule(val id: String, val name: String, val bggIds: Set<Int> = emptySet())

enum class WinRule { HIGHEST, LOWEST }

sealed interface ScoreRow {
    val id: String
    val label: String
    val hint: String?
    /** Shown only while this module is on; null for the base game. */
    val module: String?
    /** Keys of the values typed for this row, per player. */
    val inputKeys: List<String>
}

/** Points written straight onto the pad (bird cards, bonus cards). */
data class PointsRow(
    override val id: String,
    override val label: String,
    override val hint: String? = null,
    override val module: String? = null
) : ScoreRow {
    override val inputKeys: List<String> get() = listOf(id)
}

/** Something counted, each worth [pointsEach] (eggs, tucked cards, pearls). */
data class CountRow(
    override val id: String,
    override val label: String,
    val pointsEach: Int,
    override val hint: String? = null,
    override val module: String? = null
) : ScoreRow {
    override val inputKeys: List<String> get() = listOf(id)
}

/**
 * Points for placing among the players on a counted value, separately in each group (Oceania
 * nectar: in each habitat, most nectar 5, second 2). Players tied for a place add up the awards of
 * the places they cover and split them, rounded down; the next player takes the place after them.
 * A count of 0 earns nothing.
 */
data class MajorityRow(
    override val id: String,
    override val label: String,
    val groups: List<ScoreGroup>,
    val awards: List<Int>,
    override val hint: String? = null,
    override val module: String? = null
) : ScoreRow {
    override val inputKeys: List<String> get() = groups.map { groupKey(it) }
    fun groupKey(group: ScoreGroup) = "$id.${group.id}"
}

data class ScoreGroup(val id: String, val label: String)

data class ScoreTiebreaker(
    val id: String,
    val label: String,
    val preferHighest: Boolean = true
) {
    val inputKey: String get() = "tie.$id"
}

/** What a score sheet holds while it is being filled in; one entry per player column. */
data class ScoreSheetDraft(
    val sheetId: String,
    val gameId: Int,
    val players: List<String>,
    val enabledModules: Set<String>,
    val values: List<Map<String, Int>>,
    /** What was typed where it was a sum ("12+8+5"), per player and key; plain numbers live only in [values]. */
    val entries: List<Map<String, String>> = emptyList()
) {
    fun value(player: Int, key: String): Int? = values.getOrNull(player)?.get(key)

    /** The text to edit for a cell: the sum as typed, or the plain number. */
    fun entry(player: Int, key: String): String =
        entries.getOrNull(player)?.get(key) ?: value(player, key)?.toString().orEmpty()

    fun withValue(player: Int, key: String, value: Int?): ScoreSheetDraft {
        val updated = values.padded(player).toMutableList()
        updated[player] = if (value == null) updated[player] - key else updated[player] + (key to value)
        return copy(values = updated, entries = withoutEntry(player, key))
    }

    /** Takes what was typed in a cell: a number, or numbers joined by "+" that are added up. */
    fun withEntry(player: Int, key: String, text: String): ScoreSheetDraft {
        val sum = ScoreEntry.sum(text)
        val base = withValue(player, key, sum)
        if (!text.contains('+') || sum == null) return base
        val updated = base.entries.padded(player).toMutableList()
        updated[player] = updated[player] + (key to text)
        return base.copy(entries = updated)
    }

    fun withPlayer(name: String): ScoreSheetDraft = copy(
        players = players + name,
        values = values.padded(players.size - 1) + listOf(emptyMap()),
        entries = entries.padded(players.size - 1) + listOf(emptyMap())
    )

    fun withoutPlayer(player: Int): ScoreSheetDraft = copy(
        players = players.filterIndexed { i, _ -> i != player },
        values = values.filterIndexed { i, _ -> i != player },
        entries = entries.filterIndexed { i, _ -> i != player }
    )

    fun renamed(player: Int, name: String): ScoreSheetDraft =
        copy(players = players.toMutableList().also { it[player] = name })

    private fun withoutEntry(player: Int, key: String): List<Map<String, String>> =
        if (entries.getOrNull(player)?.containsKey(key) != true) entries
        else entries.toMutableList().also { it[player] = it[player] - key }

    private fun <V> List<Map<String, V>>.padded(index: Int): List<Map<String, V>> =
        this + List((index + 1 - size).coerceAtLeast(0)) { emptyMap() }
}

/** Sums typed into a score cell: "12+8+5" is 25. Only digits and "+" count; a trailing "+" is fine. */
object ScoreEntry {
    private const val MAX_TOTAL = 99_999

    /** Keeps digits and single "+" signs, so the text stays a sum while it is typed. */
    fun clean(typed: String): String =
        typed.filter { it.isDigit() || it == '+' }
            .let { var t = it; while ("++" in t) t = t.replace("++", "+"); t }
            .split('+').joinToString("+") { it.take(4) }
            .take(24)

    /** The total, or null when there is no number yet. */
    fun sum(text: String): Int? {
        val parts = text.split('+').mapNotNull { it.trim().toIntOrNull() }
        if (parts.isEmpty()) return null
        return parts.sum().coerceAtMost(MAX_TOTAL)
    }
}

data class ScoreSheetResult(
    /** Points per row id, per player; MajorityRow points are the sum over its groups. */
    val rowPoints: Map<String, List<Int>>,
    /** Points per MajorityRow group key, per player. */
    val groupPoints: Map<String, List<Int>>,
    val totals: List<Int>,
    /** Player indexes who win; empty until something is entered. */
    val winners: Set<Int>,
    /** The tiebreaker that settled the win, if one did. */
    val decidedBy: ScoreTiebreaker?,
    /** Tiebreakers to show: those used, and the one still needed (with [tiedPlayers]). */
    val shownTiebreakers: List<ScoreTiebreaker>,
    val pendingTiebreaker: ScoreTiebreaker?,
    /** Players a shown tiebreaker applies to: those still tied when it was reached. */
    val tiedPlayers: Map<String, Set<Int>>,
    val hasInput: Boolean
)

object ScoreSheetScoring {

    fun score(sheet: ScoreSheet, draft: ScoreSheetDraft): ScoreSheetResult {
        val playerCount = draft.players.size
        val rows = sheet.rowsFor(draft.enabledModules)
        val rowPoints = linkedMapOf<String, List<Int>>()
        val groupPoints = linkedMapOf<String, List<Int>>()

        for (row in rows) {
            rowPoints[row.id] = when (row) {
                is PointsRow -> List(playerCount) { draft.value(it, row.id) ?: 0 }
                is CountRow -> List(playerCount) { (draft.value(it, row.id) ?: 0) * row.pointsEach }
                is MajorityRow -> {
                    val perGroup = row.groups.map { group ->
                        val key = row.groupKey(group)
                        majorityPoints(List(playerCount) { draft.value(it, key) ?: 0 }, row.awards)
                            .also { groupPoints[key] = it }
                    }
                    List(playerCount) { p -> perGroup.sumOf { it[p] } }
                }
            }
        }

        val totals = List(playerCount) { p -> rowPoints.values.sumOf { it[p] } }
        val rowKeys = rows.flatMap { it.inputKeys }.toSet()
        val hasInput = draft.values.any { values -> values.keys.any { it in rowKeys } }

        var leaders: Set<Int> = if (!hasInput || playerCount == 0) emptySet() else {
            val best = if (sheet.winRule == WinRule.HIGHEST) totals.max() else totals.min()
            totals.indices.filterTo(linkedSetOf()) { totals[it] == best }
        }
        var decidedBy: ScoreTiebreaker? = null
        var pending: ScoreTiebreaker? = null
        val shown = mutableListOf<ScoreTiebreaker>()
        val tied = linkedMapOf<String, Set<Int>>()
        for (tiebreaker in sheet.tiebreakers) {
            if (leaders.size <= 1) break
            shown += tiebreaker
            tied[tiebreaker.id] = leaders
            val values = leaders.associateWith { draft.value(it, tiebreaker.inputKey) }
            if (values.values.any { it == null }) {
                pending = tiebreaker
                break
            }
            val best = if (tiebreaker.preferHighest) values.values.maxOf { it!! } else values.values.minOf { it!! }
            val remaining = leaders.filterTo(linkedSetOf()) { values[it] == best }
            if (remaining.size < leaders.size) decidedBy = tiebreaker
            leaders = remaining
        }
        if (leaders.size > 1 && pending == null) decidedBy = null

        return ScoreSheetResult(
            rowPoints = rowPoints,
            groupPoints = groupPoints,
            totals = totals,
            winners = leaders,
            decidedBy = decidedBy,
            shownTiebreakers = shown,
            pendingTiebreaker = pending,
            tiedPlayers = tied,
            hasInput = hasInput
        )
    }

    /** Awards by place for one group; see [MajorityRow]. */
    fun majorityPoints(counts: List<Int>, awards: List<Int>): List<Int> {
        val points = MutableList(counts.size) { 0 }
        val byCount = counts.indices.filter { counts[it] > 0 }.groupBy { counts[it] }
            .toSortedMap(compareByDescending { it })
        var place = 0
        for ((_, players) in byCount) {
            if (place >= awards.size) break
            val pot = (place until place + players.size).sumOf { awards.getOrElse(it) { 0 } }
            players.forEach { points[it] = pot / players.size }
            place += players.size
        }
        return points
    }
}
