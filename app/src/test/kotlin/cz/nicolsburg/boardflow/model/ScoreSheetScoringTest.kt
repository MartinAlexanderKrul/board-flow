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
    fun removingAPlayerDropsTheirColumn() {
        val sheet = ScoreSheets.wingspan
        val three = draft(sheet, mapOf("birds" to 10), mapOf("birds" to 20), mapOf("birds" to 30))
        val two = three.withoutPlayer(1)
        assertEquals(listOf("P0", "P2"), two.players)
        assertEquals(listOf(10, 30), ScoreSheetScoring.score(sheet, two).totals)
    }

    @Test
    fun typedSumsAddUp() {
        assertEquals(25, ScoreEntry.sum("12+8+5"))
        assertEquals(12, ScoreEntry.sum("12+"))
        assertEquals(7, ScoreEntry.sum("7"))
        assertNull(ScoreEntry.sum(""))
        assertNull(ScoreEntry.sum("+"))
        assertEquals("3+4", ScoreEntry.clean("3++4"))
        assertEquals("12+5", ScoreEntry.clean("12 + 5"))
    }

    @Test
    fun aSumKeepsWhatWasTypedAndScoresItsTotal() {
        val sheet = ScoreSheets.wingspan
        val summed = draft(sheet, emptyMap(), emptyMap()).withEntry(0, "birds", "12+8+5").withEntry(1, "birds", "30")
        assertEquals("12+8+5", summed.entry(0, "birds"))
        assertEquals("30", summed.entry(1, "birds"))
        assertEquals(listOf(25, 30), ScoreSheetScoring.score(sheet, summed).totals)

        // A plain number replaces the sum; clearing the cell clears both.
        assertEquals("9", summed.withEntry(0, "birds", "9").entry(0, "birds"))
        assertEquals("", summed.withEntry(0, "birds", "").entry(0, "birds"))
        assertEquals(listOf("P1"), summed.withoutPlayer(0).players)
        assertEquals("30", summed.withoutPlayer(0).entry(0, "birds"))
    }

    @Test
    fun tiesAllScoreGivesEveryTiedPlayerTheFullAward() {
        // Bellfaire garland: most 6, second 3, tied players all score.
        assertEquals(listOf(6, 6, 3, 0), ScoreSheetScoring.majorityPoints(listOf(4, 4, 2, 0), listOf(6, 3), TieRule.ALL))
        assertEquals(listOf(6, 3, 3), ScoreSheetScoring.majorityPoints(listOf(5, 2, 2), listOf(6, 3), TieRule.ALL))
    }

    @Test
    fun silverfrostSnowFollowsTheChosenReward() {
        val sheet = ScoreSheets.silverfrost
        assertEquals(setOf("flurries"), sheet.modulesFor(listOf(436821)))
        val snow = draft(sheet, mapOf("snow.cleared" to 6), mapOf("snow.cleared" to 6), mapOf("snow.cleared" to 3), modules = setOf("flurries"))
        // Flurries: 1 per Snow, and the tied leaders both get 5 more.
        assertEquals(listOf(11, 11, 3), ScoreSheetScoring.score(sheet, snow).totals)
        // Ice Storm keeps the typed Snow and gives the leaders 12.
        val iceStorm = snow.copy(enabledModules = sheet.toggleModule(snow.enabledModules, "ice-storm"))
        assertEquals(setOf("ice-storm"), iceStorm.enabledModules)
        assertEquals(listOf(12, 12, 0), ScoreSheetScoring.score(sheet, iceStorm).totals)
        // A group never ends up empty.
        assertEquals(setOf("ice-storm"), sheet.toggleModule(iceStorm.enabledModules, "ice-storm"))
    }

    @Test
    fun countRowCanScorePerTwo() {
        val sheet = ScoreSheets.emerland
        val result = ScoreSheetScoring.score(sheet, draft(sheet, mapOf("resources" to 7)))
        assertEquals(listOf(3), result.rowPoints["resources"])
    }

    @Test
    fun hummingbirdsCanGoNegative() {
        assertEquals(4, ScoreEntry.sum("1+2+3-3+1"))
        assertEquals(-3, ScoreEntry.sum("-3"))
        assertEquals("-3+4", ScoreEntry.clean("--3++4", allowNegative = true))
        assertEquals("34", ScoreEntry.clean("-3-4"))
        val sheet = ScoreSheets.wingspan
        val result = ScoreSheetScoring.score(sheet, draft(sheet, emptyMap(), modules = setOf("americas")).withEntry(0, "hummingbirds", "1+2-5"))
        assertEquals(listOf(-2), result.totals)
    }

    @Test
    fun tiebreakerReadFromTheSheetNeedsNoInput() {
        // Farshore: tied totals go to the ship that moved furthest.
        val sheet = ScoreSheets.farshore
        val tied = draft(sheet, mapOf("cards" to 40, "ship" to 10), mapOf("cards" to 35, "ship" to 15))
        val result = ScoreSheetScoring.score(sheet, tied)
        assertEquals(setOf(1), result.winners)
        assertEquals("ship", result.decidedBy?.id)
        assertNull(result.pendingTiebreaker)
    }

    @Test
    fun everyGameOfBothFamiliesHasASheet() {
        val ids = mapOf(
            266192 to "wingspan", 290448 to "wingspan", 300580 to "wingspan", 461932 to "wingspan", 366161 to "wingspan",
            199792 to "everdell", 259996 to "everdell", 289056 to "everdell", 289057 to "everdell", 332390 to "everdell",
            332397 to "everdell", 297972 to "everdell", 297973 to "everdell", 261142 to "everdell", 289405 to "everdell",
            292794 to "everdell", 394106 to "farshore", 425005 to "everdell-duo", 436821 to "silverfrost", 455843 to "emerland",
            436126 to "finspan", 468426 to "finspan", 410201 to "wyrmspan", 453406 to "wyrmspan"
        )
        ids.forEach { (id, sheet) -> assertEquals("BGG $id", sheet, ScoreSheets.forGames(listOf(id))?.id) }
        // No id opens two sheets, and row and module ids are unique within a sheet.
        val all = ScoreSheets.all.flatMap { it.gameIds }
        assertEquals(all.size, all.toSet().size)
        ScoreSheets.all.forEach { sheet ->
            assertEquals(sheet.id, sheet.rows.size, sheet.rows.map { it.id }.toSet().size)
            sheet.rows.mapNotNull { it.module }.forEach { m -> assertTrue("${sheet.id} $m", sheet.modules.any { it.id == m }) }
        }
    }

    @Test
    fun fledglingsScoreTheirCountSquared() {
        val sheet = ScoreSheets.wyrmspan
        val result = ScoreSheetScoring.score(sheet, draft(sheet, mapOf("fledglings" to 3, "items" to 9), modules = setOf("academy")))
        assertEquals(listOf(9), result.rowPoints["fledglings"])
        assertEquals(listOf(2), result.rowPoints["items"])
        assertEquals(11, result.totals[0])
    }

    @Test
    fun finspanSchoolsAreWorthSix() {
        val sheet = ScoreSheets.finspan
        val result = ScoreSheetScoring.score(sheet, draft(sheet, mapOf("schools" to 2, "eggs" to 3, "coral" to 4), modules = setOf("sharks")))
        assertEquals(19, result.totals[0])
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
