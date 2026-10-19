package cz.nicolsburg.boardflow.data.scoresheet

import cz.nicolsburg.boardflow.model.ScoreExample
import cz.nicolsburg.boardflow.model.SheetOrigin
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreSheetJsonTest {

    @Test
    fun everyBuiltInSheetRoundTrips() {
        ScoreSheets.all.forEach { sheet ->
            val text = ScoreSheetJson.write(sheet).toString()
            val back = ScoreSheetJson.parse(text)
            // Built-in sheets read back as built in; everything else must be identical.
            assertEquals(sheet.id, sheet, back.copy(origin = SheetOrigin.BUILT_IN))
        }
    }

    @Test
    fun everyBuiltInSheetPassesTheValidator() {
        ScoreSheets.all.forEach { sheet ->
            assertEquals(sheet.id, emptyList<String>(), ScoreSheetValidator.validate(sheet))
        }
    }

    @Test
    fun examplesMustAddUp() {
        val sheet = ScoreSheets.byId("lost-cities")!!
        fun e(sum: Int, wagers: Int, cards: Int) = mapOf(
            "expeditions.red.sum" to sum, "expeditions.red.wagers" to wagers, "expeditions.red.cards" to cards
        )
        val good = sheet.copy(examples = listOf(ScoreExample(players = listOf(e(35, 2, 8), e(10, 0, 2)), totals = listOf(65, -10))))
        assertEquals(emptyList<String>(), ScoreSheetValidator.validate(good))

        val wrong = good.copy(examples = listOf(good.examples.first().copy(totals = listOf(45, -10))))
        val problems = ScoreSheetValidator.validate(wrong)
        assertEquals(1, problems.size)
        assertTrue(problems.first(), problems.first().contains("should total [45, -10] but the rows give [65, -10]"))

        val unknown = good.copy(examples = listOf(ScoreExample(players = listOf(mapOf("nope" to 1)), totals = listOf(1))))
        assertTrue(ScoreSheetValidator.validate(unknown).first().contains("nope"))
    }

    @Test
    fun validatorCatchesBrokenSheets() {
        val sheet = ScoreSheets.wingspan
        val broken = sheet.copy(
            rows = sheet.rows + sheet.rows.first(),
            tiebreakers = sheet.tiebreakers + sheet.tiebreakers.first().copy(id = "x", fromKey = "missing")
        )
        val problems = ScoreSheetValidator.validate(broken)
        assertTrue(problems.toString(), problems.any { it.contains("Row id \"birds\" is used twice") })
        assertTrue(problems.toString(), problems.any { it.contains("reads \"missing\"") })
    }

    @Test
    fun readsAHandWrittenSheet() {
        val json = JSONObject(
            """
            {"id":"user-1","gameName":"Test","gameIds":[1],"maxPlayers":4,
             "provenance":{"origin":"AI_DRAFT","reviewed":false,"aiModel":"m"},
             "rows":[{"kind":"points","id":"cards","label":"Cards"},
                     {"kind":"count","id":"coins","label":"Coins","per":3},
                     {"kind":"majority","id":"most","label":"Most","groups":[{"id":"x","label":"X"}],"awards":[5,2],"ties":"ALL"}],
             "examples":[{"players":[{"cards":10,"coins":7,"most.x":3},{"cards":4,"most.x":1}],"totals":[17,6]}]}
            """.trimIndent()
        )
        val sheet = ScoreSheetJson.parse(json)
        assertEquals(SheetOrigin.AI_DRAFT, sheet.origin)
        assertEquals(false, sheet.reviewed)
        assertEquals(3, sheet.rows.size)
        assertEquals(emptyList<String>(), ScoreSheetValidator.validate(sheet))
    }
}
