package cz.nicolsburg.boardflow.data.setupguide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the repo-root `setup-guides/` folder, which is both bundled into the APK and served as the
 * remote catalog. A broken guide here would be shipped to every install, so CI must catch it.
 */
class BundledSetupGuidesTest {

    private val dir = listOf(File("../setup-guides"), File("setup-guides")).first { it.isDirectory }
    private val index = SetupGuideIndex.parse(File(dir, "index.json").readText())

    @Test
    fun everyIndexedGuideParsesAndValidates() {
        assertTrue("index lists no guides", index.isNotEmpty())
        index.forEach { entry ->
            val guide = SetupGuideJson.parse(File(dir, entry.path).readText())
            assertEquals("${entry.path}: problems", emptyList<String>(), SetupGuideValidator.validate(guide))
            assertEquals("${entry.path}: gameId", entry.gameId, guide.gameId)
            assertEquals("${entry.path}: version must match index", entry.version, guide.version)
            assertEquals("${entry.path}: schemaVersion", entry.schemaVersion, guide.schemaVersion)
            assertEquals("${entry.path}: index gameIds must list base, aliases and module ids", guide.allGameIds, entry.gameIds)
        }
    }

    @Test
    fun everyGuideFileIsIndexed() {
        val files = dir.listFiles { f -> f.extension == "json" && f.name != "index.json" }.orEmpty().map { it.name }.toSet()
        assertEquals(files, index.map { it.path }.toSet())
    }

    @Test
    fun guidesRoundTripThroughSerializer() {
        index.forEach { entry ->
            val guide = SetupGuideJson.parse(File(dir, entry.path).readText())
            assertEquals(entry.path, guide, SetupGuideJson.parse(SetupGuideJson.toJsonString(guide)))
        }
    }

    @Test
    fun everyConfigurationResolvesWithoutUnfilledPlaceholders() {
        index.forEach { entry ->
            val guide = SetupGuideJson.parse(File(dir, entry.path).readText())
            val moduleIds = guide.modules.map { it.id }
            // Every single module, and everything at once, at every player count.
            val selections = moduleIds.map { setOf(it) } + listOf(emptySet(), moduleIds.toSet())
            for (players in SetupGuideResolver.allPlayerCounts(guide)) {
                for (selection in selections) {
                    val resolved = SetupGuideResolver.resolve(guide, players, selection)
                    resolved.sections.flatMap { it.steps }.forEach { step ->
                        assertTrue(
                            "${entry.path} p=$players $selection step ${step.id}: '${step.plainText}'",
                            !step.plainText.contains('{')
                        )
                    }
                }
            }
        }
    }
}
