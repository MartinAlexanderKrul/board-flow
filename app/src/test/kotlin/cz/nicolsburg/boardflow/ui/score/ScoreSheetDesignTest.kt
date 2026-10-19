package cz.nicolsburg.boardflow.ui.score

import cz.nicolsburg.boardflow.data.scoresheet.ScoreSheets
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreSheetDesignTest {

    /** Every sheet has a design, and every row and asked tiebreaker has its own drawn icon. */
    @Test
    fun everyRowHasAnIcon() {
        ScoreSheets.all.forEach { sheet ->
            val design = scoreSheetDesign(sheet.id)
            sheet.rows.forEach { row ->
                assertTrue("${sheet.id} ${row.id}", row.id in design.glyphs)
            }
            sheet.tiebreakers.filter { it.asks }.forEach { tb ->
                assertTrue("${sheet.id} ${tb.inputKey}", tb.inputKey in design.glyphs)
            }
        }
    }
}
