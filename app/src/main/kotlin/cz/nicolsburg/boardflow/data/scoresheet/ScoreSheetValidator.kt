package cz.nicolsburg.boardflow.data.scoresheet

import cz.nicolsburg.boardflow.model.CountRow
import cz.nicolsburg.boardflow.model.ExpeditionRow
import cz.nicolsburg.boardflow.model.MajorityRow
import cz.nicolsburg.boardflow.model.ScoreGroup
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.ScoreSheetDraft
import cz.nicolsburg.boardflow.model.ScoreSheetScoring
import cz.nicolsburg.boardflow.model.SetsRow
import cz.nicolsburg.boardflow.model.TableRow

/**
 * Structural checks on a score sheet before it is shown or stored (AI drafts, imports). Each
 * problem is one short sentence a model can act on when its answer is sent back. A sheet's
 * worked [ScoreSheet.examples] must come out at the totals they state: that catches a draft whose
 * rows do not add up the way the rulebook does.
 */
object ScoreSheetValidator {
    private val ID = Regex("[a-z0-9][a-z0-9-]*")

    fun validate(sheet: ScoreSheet): List<String> = buildList {
        if (sheet.id.isBlank()) add("The sheet has no id")
        if (sheet.gameName.isBlank()) add("The sheet has no gameName")
        if (sheet.gameIds.isEmpty()) add("The sheet has no gameIds")
        if (sheet.maxPlayers !in 1..20) add("maxPlayers must be between 1 and 20")
        if (sheet.rows.isEmpty()) add("The sheet has no rows")

        val moduleIds = sheet.modules.map { it.id }
        moduleIds.groupingBy { it }.eachCount().filter { it.value > 1 }.keys.forEach { add("Module id \"$it\" is used twice") }
        moduleIds.filterNot { ID.matches(it) }.forEach { add("Module id \"$it\" must be lower-case kebab-case") }

        val rowIds = sheet.rows.map { it.id }
        rowIds.groupingBy { it }.eachCount().filter { it.value > 1 }.keys.forEach { add("Row id \"$it\" is used twice") }
        for (row in sheet.rows) {
            if (!ID.matches(row.id)) add("Row id \"${row.id}\" must be lower-case kebab-case")
            if (row.label.isBlank()) add("Row ${row.id} has no label")
            row.module?.let { if (it !in moduleIds) add("Row ${row.id} uses module \"$it\", which is not in modules") }
            when (row) {
                is CountRow -> {
                    if (row.per < 1) add("Row ${row.id}: per must be 1 or more")
                    if (row.squared && row.per != 1) add("Row ${row.id}: a squared count cannot also use per")
                }
                is MajorityRow -> {
                    checkGroups(row.id, row.groups)
                    if (row.awards.any { it < 0 } || row.awardsByPlayers.values.flatten().any { it < 0 }) {
                        add("Row ${row.id}: awards cannot be negative; use fewestPenalty for a loss")
                    }
                    if (row.awards.isEmpty() && row.awardsByPlayers.isEmpty() && row.pointsEach == 0 && row.beatsEach == 0 && row.fewestPenalty == 0) {
                        add("Row ${row.id}: a majority row needs awards")
                    }
                }
                is TableRow -> {
                    if (row.steps.isEmpty()) add("Row ${row.id}: a table needs steps")
                    if (row.steps.zipWithNext().any { (a, b) -> b.first <= a.first }) add("Row ${row.id}: table steps must go up by count")
                }
                is SetsRow -> {
                    checkGroups(row.id, row.groups)
                    if (row.groups.size < 2) add("Row ${row.id}: sets need at least 2 symbols")
                }
                is ExpeditionRow -> {
                    checkGroups(row.id, row.groups)
                    if (row.bonusCards < 1) add("Row ${row.id}: bonusCards must be 1 or more")
                }
                else -> Unit
            }
        }

        val inputKeys = sheet.rows.flatMap { it.inputKeys }.toSet()
        sheet.tiebreakers.map { it.id }.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
            .forEach { add("Tiebreaker id \"$it\" is used twice") }
        for (tiebreaker in sheet.tiebreakers) {
            if (tiebreaker.label.isBlank()) add("Tiebreaker ${tiebreaker.id} has no label")
            tiebreaker.fromKey?.let { if (it !in inputKeys) add("Tiebreaker ${tiebreaker.id} reads \"$it\", which no row has as an input") }
        }

        // Examples only make sense on a sheet that is otherwise sound.
        if (isEmpty()) sheet.examples.forEachIndexed { i, example -> addAll(checkExample(sheet, i + 1, example)) }
    }

    private fun MutableList<String>.checkGroups(rowId: String, groups: List<ScoreGroup>) {
        if (groups.isEmpty()) add("Row $rowId needs groups")
        groups.map { it.id }.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
            .forEach { add("Row $rowId: group id \"$it\" is used twice") }
        groups.filterNot { ID.matches(it.id) }.forEach { add("Row $rowId: group id \"${it.id}\" must be lower-case kebab-case") }
    }

    private fun checkExample(sheet: ScoreSheet, number: Int, example: cz.nicolsburg.boardflow.model.ScoreExample): List<String> {
        if (example.players.isEmpty()) return listOf("Example $number has no players")
        if (example.totals.size != example.players.size) return listOf("Example $number has ${example.players.size} players but ${example.totals.size} totals")
        val known = sheet.rows.flatMap { it.inputKeys }.toSet() + sheet.tiebreakers.map { it.inputKey }
        val unknown = example.players.flatMap { it.keys }.filterNot { it in known }.distinct()
        if (unknown.isNotEmpty()) return listOf("Example $number uses input keys no row has: ${unknown.joinToString()}")
        val draft = ScoreSheetDraft(
            sheetId = sheet.id,
            gameId = sheet.gameIds.firstOrNull() ?: 0,
            players = example.players.indices.map { "P${it + 1}" },
            enabledModules = example.modules,
            values = example.players
        )
        val totals = ScoreSheetScoring.score(sheet, draft).totals
        return if (totals == example.totals) emptyList()
        else listOf("Example $number should total ${example.totals} but the rows give $totals")
    }
}
