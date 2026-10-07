package cz.nicolsburg.boardflow.data.setupguide

import cz.nicolsburg.boardflow.model.AmountRule
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
    fun quantityValuesPerPlayerCount() {
        // Azul's factory step: {n} = 2p 5, 3p 7, 4p 9.
        val section = first.id
        val step = first.steps.first { "{n}" in it.text }
        var g = GuideEdits.setAmountValue(azul, section, step.id, "n", 2, "6")
        g = GuideEdits.setAmountValue(g, section, step.id, "n", null, "8")
        var rule = g.sections.first().steps.first { it.id == step.id }.amounts.getValue("n")
        assertEquals("6", rule.byPlayers[2])
        assertEquals("8", rule.default)
        assertEquals(6, SetupGuideResolver.resolve(g, 2, emptySet()).sections.first().steps.first { it.id == step.id }
            .plainText.filter { it.isDigit() }.toInt())

        // Clearing every value removes the quantity, and the validator then asks for one.
        listOf(2, 3, 4).forEach { g = GuideEdits.setAmountValue(g, section, step.id, "n", it, " ") }
        g = GuideEdits.setAmountValue(g, section, step.id, "n", null, "")
        rule = g.sections.first().steps.first { it.id == step.id }.amounts["n"] ?: AmountRule()
        assertTrue(rule.byPlayers.isEmpty() && rule.default == null)
        assertTrue(SetupGuideValidator.validate(g).any { "{n}" in it })
    }

    @Test
    fun shownAtCountsIsStoredCompactly() {
        val counts = listOf(2, 3, 4)
        val stepId = first.steps.first().id
        fun condition(selected: Set<Int>) =
            GuideEdits.setShownAtCounts(azul, first.id, stepId, selected, counts).sections.first().steps.first().condition

        assertEquals(null, condition(setOf(2, 3, 4)))
        assertEquals(3, condition(setOf(3, 4))?.minPlayers)
        assertEquals(3, condition(setOf(2, 3))?.maxPlayers)
        assertEquals(setOf(2, 4), condition(setOf(2, 4))?.players)
        assertEquals(setOf(3, 4), GuideEdits.shownAtCounts(condition(setOf(3, 4)), counts))
        assertEquals(azul, GuideEdits.setShownAtCounts(azul, first.id, stepId, emptySet(), counts))
    }

    @Test
    fun moduleRulesOnStepsAndSections() {
        val everdell = guide("199792.json")
        val module = everdell.modules.first().id
        val section = everdell.sections.first()
        val step = section.steps.first()
        var g = GuideEdits.setModuleRule(everdell, section.id, step.id, module, GuideEdits.ModuleRule.WITHOUT)
        var c = g.sections.first().steps.first().condition
        assertEquals(GuideEdits.ModuleRule.WITHOUT, GuideEdits.moduleRule(c, module))
        g = GuideEdits.setModuleRule(g, section.id, step.id, module, GuideEdits.ModuleRule.WITH)
        c = g.sections.first().steps.first().condition
        assertTrue(module in c!!.modules && module !in c.notModules)
        g = GuideEdits.setModuleRule(g, section.id, step.id, module, GuideEdits.ModuleRule.ANY)
        assertEquals(step.condition, g.sections.first().steps.first().condition)

        g = GuideEdits.setModuleRule(everdell, section.id, null, module, GuideEdits.ModuleRule.WITH)
        assertEquals(listOf(module), g.sections.first().condition?.modules)
        assertTrue(SetupGuideValidator.validate(g).isEmpty())
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
