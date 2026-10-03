package cz.nicolsburg.boardflow.data.setupguide

import cz.nicolsburg.boardflow.model.GuideOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GuideDraftPromptTest {

    private val azulJson: String = listOf(File("../setup-guides"), File("setup-guides"))
        .first { it.isDirectory }
        .resolve("230802.json")
        .readText()

    @Test
    fun fencedAnswerBecomesAnUnreviewedDraftForTheRequestedGame() {
        // The model may wrap the JSON in a fence, get the id wrong and invent BGG ids.
        val answer = "```json\n" + azulJson
            .replace("\"gameId\": 230802", "\"gameId\": 1, \"aliasGameIds\": [5]")
            .replace("\"version\": 2", "\"version\": 7") + "\n```"
        val (guide, problems) = GuideDraftPrompt.parse(answer, 230802, "Azul", "gemini-flash-latest", "azul-rules.pdf")

        assertTrue(problems.toString(), problems.isEmpty())
        assertEquals(230802, guide.gameId)
        assertTrue(guide.aliasGameIds.isEmpty())
        assertEquals(1, guide.version)
        assertEquals(GuideOrigin.AI_DRAFT, guide.provenance.origin)
        assertFalse(guide.provenance.reviewed)
        assertEquals("gemini-flash-latest", guide.provenance.aiModel)
        assertEquals(listOf("azul-rules.pdf"), guide.provenance.sources)
        assertTrue(guide.modules.all { it.bggId == null })
    }

    @Test
    fun invalidGuideComesBackWithItsProblems() {
        val noSteps = """{"schemaVersion":1,"gameId":1,"gameName":"X","version":1,"sections":[]}"""
        val (_, problems) = GuideDraftPrompt.parse(noSteps, 42, "Test", null, null)
        assertTrue(problems.isNotEmpty())
    }

    @Test(expected = Exception::class)
    fun answerWithoutJsonThrows() {
        GuideDraftPrompt.parse("Sorry, I cannot read this file.", 42, "Test", null, null)
    }

    @Test
    fun retryPromptListsTheProblems() {
        val first = GuideDraftPrompt.build(230802, "Azul")
        val retry = GuideDraftPrompt.build(230802, "Azul", listOf("Duplicate step id setup-1"))
        assertFalse(first.contains("rejected"))
        assertTrue(retry.contains("Duplicate step id setup-1"))
        assertTrue(first.contains("230802"))
    }
}
