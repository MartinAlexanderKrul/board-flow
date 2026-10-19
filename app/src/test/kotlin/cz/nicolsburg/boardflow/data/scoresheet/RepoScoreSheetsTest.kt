package cz.nicolsburg.boardflow.data.scoresheet

import cz.nicolsburg.boardflow.model.SheetOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the repo-root `score-sheets/` folder, which is bundled into the APK and served as the
 * catalog. Pull requests from the contribution relay land here; CI runs this on every one.
 */
class RepoScoreSheetsTest {

    private val dir = listOf(File("../score-sheets"), File("score-sheets")).first { it.isDirectory }
    private val index = ScoreSheetIndex.parse(File(dir, "index.json").readText())

    @Test
    fun everyIndexedSheetParsesAndValidates() {
        index.forEach { entry ->
            val text = File(dir, entry.path).readText()
            val sheet = ScoreSheetJson.parse(text)
            assertEquals("${entry.path}: problems", emptyList<String>(), ScoreSheetValidator.validate(sheet))
            assertEquals("${entry.path}: id must match index", entry.id, sheet.id)
            assertEquals("${entry.path}: index gameIds must match the sheet", sheet.gameIds, entry.gameIds)
            assertTrue("${entry.path}: version", entry.version >= 1)
            assertNotNull("${entry.path}: the app must load it", ScoreSheetIndex.parseSheet(text))
        }
    }

    @Test
    fun everySheetFileIsIndexedOnce() {
        val files = dir.listFiles { f -> f.extension == "json" && f.name != "index.json" }.orEmpty().map { it.name }.toSet()
        assertEquals(files, index.map { it.path }.toSet())
        assertEquals("duplicate ids", index.size, index.map { it.id }.toSet().size)
    }

    /** A shared sheet is for a game with no built-in sheet; a fix to a built-in one keeps its id. */
    @Test
    fun sharedSheetsDoNotShadowBuiltInsByAccident() {
        val builtInIds = ScoreSheets.all.associate { it.id to it.gameIds }
        index.forEach { entry ->
            val sameId = builtInIds[entry.id]
            if (sameId == null) {
                val clash = ScoreSheets.all.filter { s -> s.gameIds.any { it in entry.gameIds } }.map { it.id }
                assertEquals("${entry.path} covers games of built-in sheets $clash; give it that sheet's id to replace it", emptyList<String>(), clash)
            }
        }
    }

    @Test
    fun indexRoundTripsAndSheetsLoadAsCommunity() {
        val entries = listOf(ScoreSheetIndexEntry("community-1", "Game", setOf(1, 2), 3, "1.json"))
        assertEquals(entries, ScoreSheetIndex.parse(ScoreSheetIndex.toJson(entries)))
        val sheet = ScoreSheets.byId("wingspan")!!
        val loaded = ScoreSheetIndex.parseSheet(ScoreSheetJson.write(sheet.copy(origin = SheetOrigin.AI_DRAFT, reviewed = false)).toString())!!
        assertEquals(SheetOrigin.COMMUNITY, loaded.origin)
        assertTrue(loaded.reviewed)
    }
}
