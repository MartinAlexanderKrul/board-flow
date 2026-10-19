package cz.nicolsburg.boardflow.ui.score

import cz.nicolsburg.boardflow.data.scoresheet.ScoreSheets
import cz.nicolsburg.boardflow.model.SheetOrigin
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreSheetDesignTest {

    /** Every sheet has a design, and every row and asked tiebreaker has its own drawn icon. */
    @Test
    fun everyRowHasAnIcon() {
        ScoreSheets.all.forEach { sheet ->
            val design = builtInDesign(sheet.id) ?: error("No hand-made design for ${sheet.id}")
            sheet.rows.forEach { row ->
                assertTrue("${sheet.id} ${row.id}", row.id in design.glyphs)
            }
            sheet.tiebreakers.filter { it.asks }.forEach { tb ->
                assertTrue("${sheet.id} ${tb.inputKey}", tb.inputKey in design.glyphs)
            }
        }
    }

    /** A user's sheet has no hand-made design; keyword icons still cover every row. */
    @Test
    fun userSheetsGetKeywordIcons() {
        ScoreSheets.all.map { it.copy(id = "user-${it.id}", origin = SheetOrigin.AI_DRAFT) }.forEach { sheet ->
            val design = scoreSheetDesign(sheet)
            sheet.rows.forEach { row -> assertTrue("${sheet.id} ${row.id}", row.id in design.glyphs) }
        }
    }
}
