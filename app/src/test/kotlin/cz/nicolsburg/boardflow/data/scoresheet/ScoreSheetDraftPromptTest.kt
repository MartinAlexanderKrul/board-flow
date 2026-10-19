package cz.nicolsburg.boardflow.data.scoresheet

import cz.nicolsburg.boardflow.model.SheetOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreSheetDraftPromptTest {

    private fun answer(totals: String = "[41, 38]", examples: Boolean = true) = """
        ```json
        {
          "id": "wingspan-ish",
          "gameName": "Something else",
          "gameIds": [999],
          "maxPlayers": 40,
          "provenance": { "origin": "BUILT_IN", "reviewed": true },
          "modules": [ { "id": "extra", "name": "Extra", "bggIds": [12345] } ],
          "rows": [
            { "kind": "points", "id": "birds", "label": "Birds" },
            { "kind": "count", "id": "eggs", "label": "Eggs", "pointsEach": 1 },
            { "kind": "majority", "id": "goals", "label": "Most forests", "groups": [ { "id": "forest", "label": "Forest" } ], "awards": [5, 2] }
          ],
          "examples": ${if (examples) """[ { "players": [ { "birds": 30, "eggs": 8, "goals.forest": 3 }, { "birds": 25, "eggs": 10, "goals.forest": 3 } ], "totals": $totals } ]""" else "[]"}
        }
        ```
    """.trimIndent()

    @Test
    fun parseForcesTheGameAndAiProvenance() {
        val (sheet, problems) = ScoreSheetDraftPrompt.parse(answer(), 266192, "Wingspan", "gemini-x", "rules.pdf")
        assertEquals(emptyList<String>(), problems)
        assertEquals("user-266192", sheet.id)
        assertEquals("Wingspan", sheet.gameName)
        assertEquals(setOf(266192), sheet.gameIds)
        assertEquals(12, sheet.maxPlayers)
        assertEquals(emptySet<Int>(), sheet.modules.single().bggIds)
        assertEquals(SheetOrigin.AI_DRAFT, sheet.origin)
        assertFalse(sheet.reviewed)
        assertEquals("gemini-x", sheet.aiModel)
        assertEquals(listOf("rules.pdf"), sheet.sources)
    }

    @Test
    fun examplesThatDoNotAddUpAreProblems() {
        val (_, problems) = ScoreSheetDraftPrompt.parse(answer(totals = "[43, 38]"), 1, "G", null, null)
        assertEquals(1, problems.size)
        assertTrue(problems.single(), problems.single().contains("should total"))
    }

    @Test
    fun aSheetNeedsAWorkedExample() {
        val (_, problems) = ScoreSheetDraftPrompt.parse(answer(examples = false), 1, "G", null, null)
        assertTrue(problems.toString(), problems.any { it.contains("worked example") })
    }

    @Test
    fun noScoreSheetIsARefusal() {
        val reason = ScoreSheetDraftPrompt.refusal("""{"noScoreSheet": "Points are scored only during play"}""")
        assertEquals("This rulebook has no end-of-game scoring to put on a sheet: Points are scored only during play", reason)
        assertNull(ScoreSheetDraftPrompt.refusal(answer()))
        assertNull(ScoreSheetDraftPrompt.refusal("not json"))
    }

    @Test
    fun theRetryPromptListsTheProblems() {
        val prompt = ScoreSheetDraftPrompt.build(1, "Game", listOf("Example 1 should total [1] but the rows give [2]"))
        assertTrue(prompt.contains("\"Game\""))
        assertTrue(prompt.contains("- Example 1 should total [1] but the rows give [2]"))
        assertTrue(prompt.contains("noScoreSheet"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun anUnknownRowKindIsNotASheet() {
        ScoreSheetDraftPrompt.parse("""{"id":"x","gameName":"G","rows":[{"kind":"dice","id":"a"}]}""", 1, "G", null, null)
    }

    /** A model sometimes writes an id with an underscore; it is fixed everywhere the id is used, not rejected. */
    @Test
    fun anUnderscoreInAnIdIsFixedNotRejected() {
        val text = """
            {
              "id": "x", "gameName": "G", "maxPlayers": 4,
              "modules": [ { "id": "base_game", "name": "Base" } ],
              "rows": [
                { "kind": "points", "id": "ideology_researched", "label": "Ideology", "module": "base_game" }
              ],
              "examples": [ { "players": [ { "ideology_researched": 3 }, { "ideology_researched": 1 } ], "totals": [3, 1], "modules": ["base_game"] } ]
            }
        """.trimIndent()
        val (sheet, problems) = ScoreSheetDraftPrompt.parse(text, 1, "G", null, null)
        assertEquals(emptyList<String>(), problems)
        assertEquals("ideology-researched", sheet.rows.single().id)
        assertEquals("base-game", sheet.modules.single().id)
        assertEquals("base-game", sheet.rows.single().module)
    }
}
