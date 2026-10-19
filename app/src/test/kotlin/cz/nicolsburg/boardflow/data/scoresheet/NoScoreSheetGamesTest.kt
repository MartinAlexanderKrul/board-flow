package cz.nicolsburg.boardflow.data.scoresheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoScoreSheetGamesTest {

    /** A game listed as needing no sheet must not have one, or the list would hide nothing and mislead. */
    @Test
    fun noListedGameHasASheet() {
        val withSheets = ScoreSheets.all.flatMap { it.gameIds }.toSet()
        val both = NoScoreSheetGames.all.keys.filter { it in withSheets }
        assertEquals(emptyList<Int>(), both)
    }

    @Test
    fun reasonsFollowTheRulebooks() {
        assertEquals(NoSheetReason.COOPERATIVE, NoScoreSheetGames.reasonFor(30549)) // Pandemic
        assertEquals(NoSheetReason.DURING_PLAY, NoScoreSheetGames.reasonFor(230802)) // Azul
        assertEquals(NoSheetReason.OBJECTIVE, NoScoreSheetGames.reasonFor(171)) // Chess
        assertEquals(NoSheetReason.PARTY, NoScoreSheetGames.reasonFor(178900)) // Codenames
        // Scored at the end, just no sheet yet: still offered a draft.
        assertNull(NoScoreSheetGames.reasonFor(126163)) // Tzolk'in
        assertNull(NoScoreSheetGames.reasonFor(266192)) // Wingspan has a sheet
        assertTrue(NoScoreSheetGames.all.size > 150)
    }
}
