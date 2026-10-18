package cz.nicolsburg.boardflow.model

import cz.nicolsburg.boardflow.data.scoresheet.ScoreSheets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreSheetScoringTest {

    private fun draft(sheet: ScoreSheet, vararg players: Map<String, Int>, modules: Set<String> = emptySet()) =
        ScoreSheetDraft(
            sheetId = sheet.id,
            gameId = sheet.gameIds.first(),
            players = players.indices.map { "P$it" },
            enabledModules = modules,
            values = players.toList()
        )

    @Test
    fun wingspanAddsEveryRow() {
        val sheet = ScoreSheets.wingspan
        val result = ScoreSheetScoring.score(
            sheet,
            draft(
                sheet,
                mapOf("birds" to 40, "bonus" to 9, "goals" to 12, "eggs" to 14, "cached" to 5, "tucked" to 7),
                mapOf("birds" to 35, "bonus" to 7, "goals" to 10, "eggs" to 20, "cached" to 2, "tucked" to 3)
            )
        )
        assertEquals(listOf(87, 77), result.totals)
        assertEquals(setOf(0), result.winners)
        assertNull(result.pendingTiebreaker)
    }

    @Test
    fun countRowMultipliesByItsPoints() {
        val sheet = ScoreSheets.everdell
        val result = ScoreSheetScoring.score(
            sheet,
            draft(sheet, mapOf("cards" to 20, "pearls" to 3, "wonders" to 10), modules = setOf("pearlbrook"))
        )
        assertEquals(36, result.totals[0])
        assertEquals(listOf(6), result.rowPoints["pearls"])
    }

    @Test
    fun moduleRowsCountOnlyWhileTheModuleIsOn() {
        val sheet = ScoreSheets.everdell
        val values = mapOf("cards" to 20, "pearls" to 3)
        assertEquals(20, ScoreSheetScoring.score(sheet, draft(sheet, values)).totals[0])
        assertEquals(26, ScoreSheetScoring.score(sheet, draft(sheet, values, modules = setOf("pearlbrook"))).totals[0])
    }

    @Test
    fun majorityAwardsByPlaceAndSplitsTies() {
        // Most 5, second 2.
        assertEquals(listOf(5, 2, 0), ScoreSheetScoring.majorityPoints(listOf(4, 2, 1), listOf(5, 2)))
        // Two tied for first share 5 + 2; nobody is second.
        assertEquals(listOf(3, 3, 0), ScoreSheetScoring.majorityPoints(listOf(3, 3, 1), listOf(5, 2)))
        // Two tied for second share 2, rounded down.
        assertEquals(listOf(5, 1, 1), ScoreSheetScoring.majorityPoints(listOf(4, 2, 2), listOf(5, 2)))
        // No nectar, no points.
        assertEquals(listOf(5, 0), ScoreSheetScoring.majorityPoints(listOf(1, 0), listOf(5, 2)))
    }

    @Test
    fun oceaniaNectarScoresEachHabitat() {
        val sheet = ScoreSheets.wingspan
        val result = ScoreSheetScoring.score(
            sheet,
            draft(
                sheet,
                mapOf("birds" to 10, "nectar.forest" to 3, "nectar.wetland" to 1),
                mapOf("birds" to 10, "nectar.forest" to 1, "nectar.grassland" to 2),
                modules = setOf("oceania")
            )
        )
        // Forest 5 / 2, grassland 0 / 5, wetland 5 / 0.
        assertEquals(listOf(10, 7), result.rowPoints["nectar"])
        assertEquals(listOf(20, 17), result.totals)
    }

    @Test
    fun tieAsksForTheTiebreakerThenSettlesIt() {
        val sheet = ScoreSheets.wingspan
        val tied = draft(sheet, mapOf("birds" to 50), mapOf("birds" to 50), mapOf("birds" to 30))
        val pending = ScoreSheetScoring.score(sheet, tied)
        assertEquals(setOf(0, 1), pending.winners)
        assertEquals("food", pending.pendingTiebreaker?.id)
        assertEquals(setOf(0, 1), pending.tiedPlayers["food"])

        val settled = ScoreSheetScoring.score(sheet, tied.withValue(0, "tie.food", 2).withValue(1, "tie.food", 4))
        assertEquals(setOf(1), settled.winners)
        assertEquals("food", settled.decidedBy?.id)
        assertNull(settled.pendingTiebreaker)
    }

    @Test
    fun everdellFallsThroughToTheSecondTiebreakerThenShares() {
        val sheet = ScoreSheets.everdell
        val tied = draft(sheet, mapOf("cards" to 60), mapOf("cards" to 60))
            .withValue(0, "tie.events", 3).withValue(1, "tie.events", 3)
        val second = ScoreSheetScoring.score(sheet, tied)
        assertEquals("resources", second.pendingTiebreaker?.id)

        val shared = ScoreSheetScoring.score(sheet, tied.withValue(0, "tie.resources", 2).withValue(1, "tie.resources", 2))
        assertEquals(setOf(0, 1), shared.winners)
        assertNull(shared.decidedBy)
        assertNull(shared.pendingTiebreaker)
    }

    @Test
    fun emptySheetHasNoWinner() {
        val sheet = ScoreSheets.everdell
        val result = ScoreSheetScoring.score(sheet, draft(sheet, emptyMap(), emptyMap()))
        assertFalse(result.hasInput)
        assertTrue(result.winners.isEmpty())
    }

    @Test
    fun expansionsAndStandalonesOpenTheirGamesSheet() {
        assertEquals("wingspan", ScoreSheets.forGames(listOf(366161))?.id)
        assertEquals("everdell", ScoreSheets.forGames(listOf(999999, 259996))?.id)
        assertEquals(setOf("oceania"), ScoreSheets.wingspan.modulesFor(listOf(266192, 300580)))
        assertNull(ScoreSheets.forGames(listOf(230802)))
    }
}
