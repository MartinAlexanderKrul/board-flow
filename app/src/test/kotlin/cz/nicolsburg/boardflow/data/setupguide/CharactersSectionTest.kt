package cz.nicolsburg.boardflow.data.setupguide

import cz.nicolsburg.boardflow.model.GuideSectionKind
import cz.nicolsburg.boardflow.model.SetupGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CharactersSectionTest {

    private val karak: SetupGuide = SetupGuideJson.parse(
        listOf(File("../setup-guides"), File("setup-guides")).first { it.isDirectory }.resolve("241477.json").readText()
    )
    private val heroes get() = karak.sections.first { it.kind == GuideSectionKind.CHARACTERS }

    @Test
    fun heroesShowWithTheirExpansionOnly() {
        fun names(modules: Set<String>) = SetupGuideResolver.resolve(karak, 3, modules).sections
            .first { it.kind == GuideSectionKind.CHARACTERS }.steps.map { it.id }
        assertEquals(6, names(emptySet()).size)
        assertEquals(10, names(setOf("regent")).size)
        assertTrue("alchemist" in names(setOf("sidhar-kirima-elspeth")))
        val wizard = SetupGuideResolver.resolve(karak, 3, emptySet()).sections
            .first { it.kind == GuideSectionKind.CHARACTERS }.steps.first { it.id == "wizard" }
        assertEquals(2, wizard.details.size)
    }

    @Test
    fun charactersNeedSchemaTwoAndAbilities() {
        assertTrue(SetupGuideValidator.validate(karak).isEmpty())
        // Schema 1 apps would show the box as a checklist, so the guide must say it is schema 2.
        assertTrue(SetupGuideValidator.validate(karak.copy(schemaVersion = 1)).any { "schemaVersion 2" in it })
        val noAbilities = karak.copy(sections = karak.sections.map { s ->
            if (s.kind == GuideSectionKind.CHARACTERS) s.copy(steps = s.steps.map { it.copy(details = listOf(" ")) }) else s
        })
        assertTrue(SetupGuideValidator.validate(noAbilities).any { "no abilities" in it })
    }

    @Test
    fun abilitiesSurviveTheFormatAndTheEditor() {
        val back = SetupGuideJson.parse(SetupGuideJson.toJsonString(karak))
        assertEquals(karak, back)
        // While typing, an empty new line stays; saving (tidy) drops it.
        val typing = GuideEdits.setStepDetails(karak, heroes.id, "thief", "Backstab: wins ties\n")
        val draft = SetupGuideJson.parse(SetupGuideJson.toJsonString(typing))
        assertEquals(listOf("Backstab: wins ties", ""), draft.sections.first { it.id == heroes.id }.steps.first { it.id == "thief" }.details)
        val saved = GuideEdits.tidy(draft)
        assertEquals(listOf("Backstab: wins ties"), saved.sections.first { it.id == heroes.id }.steps.first { it.id == "thief" }.details)
    }
}
