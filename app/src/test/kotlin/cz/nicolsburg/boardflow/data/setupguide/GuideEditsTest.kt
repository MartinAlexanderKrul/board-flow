package cz.nicolsburg.boardflow.data.setupguide

import cz.nicolsburg.boardflow.model.GuideSectionKind
import cz.nicolsburg.boardflow.model.SetupGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GuideEditsTest {

    private fun guide(file: String): SetupGuide {
        val dir = listOf(File("../setup-guides"), File("setup-guides")).first { it.isDirectory }
        return SetupGuideJson.parse(File(dir, file).readText())
    }

    private val azul = guide("230802.json")
    private val first get() = azul.sections.first()

    @Test
    fun addedStepGetsUniqueIdAndEmptyStepsAreDroppedOnSave() {
        val (withStep, id) = GuideEdits.addStep(azul, first.id)
        val (withTwo, id2) = GuideEdits.addStep(withStep, first.id)
        assertFalse(id == id2)
        assertEquals(first.steps.size + 2, withTwo.sections.first().steps.size)

        val typed = GuideEdits.setStepText(withTwo, first.id, id, "Shuffle the house rule deck")
        val tidy = GuideEdits.tidy(typed)
        val steps = tidy.sections.first().steps
        assertEquals("Shuffle the house rule deck", steps.last().text)
        assertTrue("the untouched empty step is dropped", steps.none { it.id == id2 })
        assertTrue(SetupGuideValidator.validate(tidy).isEmpty())
    }

    @Test
    fun moveStepStaysInsideSection() {
        val ids = first.steps.map { it.id }
        val moved = GuideEdits.moveStep(azul, first.id, ids[1], -1).sections.first().steps.map { it.id }
        assertEquals(listOf(ids[1], ids[0]) + ids.drop(2), moved)
        val unchanged = GuideEdits.moveStep(azul, first.id, ids[0], -1).sections.first().steps.map { it.id }
        assertEquals(ids, unchanged)
    }

    @Test
    fun deleteAndNotesAndTitles() {
        val stepId = first.steps.first().id
        var g = GuideEdits.setStepNote(azul, first.id, stepId, "  ")
        assertEquals(null, g.sections.first().steps.first().note)
        g = GuideEdits.setStepNote(g, first.id, stepId, "Use the sun side")
        assertEquals("Use the sun side", g.sections.first().steps.first().note)
        g = GuideEdits.setSectionTitle(g, first.id, "Table")
        assertEquals("Table", g.sections.first().title)
        g = GuideEdits.deleteStep(g, first.id, stepId)
        assertTrue(g.sections.first().steps.none { it.id == stepId })
    }

    @Test
    fun newSectionGoesAfterTheLastChecklistAndEmptyOnesVanish() {
        val (g, id) = GuideEdits.addSection(azul)
        val index = g.sections.indexOfFirst { it.id == id }
        assertEquals(GuideSectionKind.SETUP, g.sections[index].kind)
        assertTrue(g.sections.drop(index + 1).none { it.kind == GuideSectionKind.SETUP })
        assertTrue("an empty section is dropped on save", GuideEdits.tidy(g).sections.none { it.id == id })
    }

    @Test
    fun rulesLineDescribesAmountsAndConditions() {
        val everdell = guide("199792.json")
        val forest = everdell.sections.flatMap { it.steps }.first { it.id == "forest" }
        val line = GuideEdits.describeRules(forest, null, everdell.modules)
        assertNotNull(line)
        assertTrue(line!!.contains("{n}"))
        assertTrue(line.contains("2p"))
    }
}
