package cz.nicolsburg.boardflow.data.setupguide

import cz.nicolsburg.boardflow.model.GuideSectionKind
import cz.nicolsburg.boardflow.model.ResolvedSetup
import cz.nicolsburg.boardflow.model.SetupGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SetupGuideResolverTest {

    private fun guide(file: String): SetupGuide {
        val dir = listOf(File("../setup-guides"), File("setup-guides")).first { it.isDirectory }
        return SetupGuideJson.parse(File(dir, file).readText())
    }

    private val everdell = guide("199792.json")
    private val mistborn = guide("422780.json")

    private fun ResolvedSetup.step(id: String): String? =
        sections.flatMap { it.steps }.firstOrNull { it.id == id }?.plainText

    private fun ResolvedSetup.sectionIds() = sections.map { it.id }

    @Test
    fun baseGameUsesPlayerCountAmounts() {
        val two = SetupGuideResolver.resolve(everdell, 2, emptySet())
        assertEquals("Shuffle the Forest cards and place 3 on the clearings; return the rest", two.step("forest"))
        assertEquals("Deal starting hands in turn order: 5, 6 cards", two.step("hands"))
        assertNull("no solo steps at 2 players", two.step("hands-solo"))

        val four = SetupGuideResolver.resolve(everdell, 4, emptySet())
        assertTrue(four.step("forest")!!.contains("place 4"))
        assertTrue(four.step("hands")!!.endsWith("5, 6, 7, 8 cards"))
    }

    @Test
    fun amountsAreMarkedForEmphasis() {
        val step = SetupGuideResolver.resolve(everdell, 3, emptySet())
            .sections.flatMap { it.steps }.first { it.id == "meadow" }
        assertEquals(listOf("8"), step.parts.filter { it.isAmount }.map { it.text })
    }

    @Test
    fun expansionSectionsAndReplacementsFollowModules() {
        val base = SetupGuideResolver.resolve(everdell, 3, emptySet())
        assertFalse("pearlbrook" in base.sectionIds())
        assertTrue(base.step("basic-events") != null)

        val pearlbrook = SetupGuideResolver.resolve(everdell, 3, setOf("pearlbrook"))
        assertTrue("pearlbrook" in pearlbrook.sectionIds())
        assertNull("Wonder boards replace the basic Events", pearlbrook.step("basic-events"))
    }

    @Test
    fun bellfaireUnlocksFiveAndSixPlayers() {
        assertEquals(1..4, SetupGuideResolver.selectablePlayerCounts(everdell, emptySet()))
        assertEquals(1..6, SetupGuideResolver.selectablePlayerCounts(everdell, setOf("bellfaire")))

        val six = SetupGuideResolver.resolve(everdell, 6, setOf("bellfaire"))
        assertTrue(six.step("special-events")!!.contains("place 6"))
        assertEquals("Hand limit is 7 cards", six.step("hand-limit"))
        assertNull(six.step("tree-workers"))
        assertTrue(six.step("tree-workers-big") != null)
    }

    @Test
    fun nightweaveCountsAsAnExtraPlayer() {
        val solo = SetupGuideResolver.resolve(everdell, 1, setOf("nightweave"))
        assertTrue(solo.step("forest")!!.contains("place 3"))
        assertFalse("rugwort" in solo.sectionIds())
        assertTrue("nightweave" in solo.sectionIds())

        val duo = SetupGuideResolver.resolve(everdell, 2, setOf("nightweave"))
        assertTrue(duo.step("forest")!!.contains("place 4"))
        assertNull("both humans draw 5, not 5/6", duo.step("hands"))
        assertEquals("Each player draws 5 cards", duo.step("hands-nightweave"))
    }

    @Test
    fun modulesUnavailableAtPlayerCountAreDropped() {
        assertEquals(emptySet<String>(), SetupGuideResolver.effectiveModules(everdell, 3, setOf("nightweave")))
        assertTrue("nightweave" in SetupGuideResolver.lockedModules(everdell, 3))
    }

    @Test
    fun soloForcesCoopInMistborn() {
        val solo = SetupGuideResolver.resolve(mistborn, 1, emptySet())
        assertEquals(setOf("coop"), solo.enabledModules)
        assertTrue("lord-ruler" in solo.sectionIds())
        assertNull(solo.step("bonuses"))
        assertTrue("coop" in SetupGuideResolver.lockedModules(mistborn, 1))

        val competitive = SetupGuideResolver.resolve(mistborn, 4, emptySet())
        assertFalse("lord-ruler" in competitive.sectionIds())
        assertTrue(competitive.step("bonuses")!!.endsWith("4th +4 health and 1 Boxing"))
        assertTrue(competitive.step("target") != null)
        assertNull("no unused components at 4", competitive.step("unused"))
    }

    @Test
    fun modeGroupAlwaysHasExactlyOneChoice() {
        assertEquals("default mode", setOf("competitive"), SetupGuideResolver.effectiveModules(mistborn, 3, emptySet()))
        assertEquals(setOf("coop"), SetupGuideResolver.effectiveModules(mistborn, 3, setOf("coop")))
        assertEquals(
            "two members of one group collapse to one",
            1,
            SetupGuideResolver.effectiveModules(mistborn, 3, setOf("coop", "competitive")).size
        )
        assertTrue("competitive is unavailable solo", "competitive" in SetupGuideResolver.lockedModules(mistborn, 1))
    }

    @Test
    fun onlySetupSectionsAreCheckable() {
        val resolved = SetupGuideResolver.resolve(mistborn, 2, emptySet())
        val reminderIds = resolved.sections.filter { it.kind != GuideSectionKind.SETUP }.flatMap { s -> s.steps.map { it.id } }
        assertTrue(reminderIds.isNotEmpty())
        assertTrue(reminderIds.none { it in resolved.checkableStepIds })
    }

    @Test
    fun validatorCatchesBrokenGuides() {
        val broken = everdell.copy(
            sections = everdell.sections.map { section ->
                section.copy(steps = section.steps.map { it.copy(id = "dup") })
            }
        )
        assertTrue(SetupGuideValidator.validate(broken).any { it.contains("Duplicate step id") })

        val badRef = mistborn.copy(modules = emptyList())
        assertTrue(SetupGuideValidator.validate(badRef).any { it.contains("unknown module 'coop'") })
    }
}
