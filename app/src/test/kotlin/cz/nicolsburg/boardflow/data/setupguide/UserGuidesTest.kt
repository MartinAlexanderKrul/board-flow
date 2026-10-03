package cz.nicolsburg.boardflow.data.setupguide

import cz.nicolsburg.boardflow.data.BackupSerializer
import cz.nicolsburg.boardflow.model.LoadedSetupGuide
import cz.nicolsburg.boardflow.model.SetupGuide
import cz.nicolsburg.boardflow.model.SetupGuideSource
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UserGuidesTest {

    private val azul: SetupGuide = run {
        val dir = listOf(File("../setup-guides"), File("setup-guides")).first { it.isDirectory }
        SetupGuideJson.parse(File(dir, "230802.json").readText())
    }

    @Test
    fun updateNoteOnlyForUserGuidesWithNewerUpstream() {
        val user = LoadedSetupGuide(azul, SetupGuideSource.USER, upstreamVersion = 3, basedOnVersion = 2)
        assertTrue(user.upstreamUpdated)
        assertFalse(user.copy(basedOnVersion = 3).upstreamUpdated)
        assertFalse("a guide with no standard version never shows the note", user.copy(upstreamVersion = null).upstreamUpdated)
        assertTrue("no recorded base counts as older", user.copy(basedOnVersion = null).upstreamUpdated)
        assertFalse(LoadedSetupGuide(azul, SetupGuideSource.BUNDLED, upstreamVersion = 9).upstreamUpdated)
    }

    @Test
    fun exportedGuideRoundTripsAndValidates() {
        val json = SetupGuideJson.toJsonString(azul)
        val back = SetupGuideJson.parse(json)
        assertEquals(azul, back)
        assertTrue(SetupGuideValidator.validate(back).isEmpty())
    }

    private fun importBackup(json: String) = BackupSerializer.import(
        json = json,
        onSettings = {},
        onSecureSettings = {},
        onRecentGamesJson = {},
        onAvailableModelsJson = {},
        clearLegacyCachedCollection = {}
    )

    @Test
    fun backupCarriesUserGuides() {
        val root = JSONObject()
            .put("version", 8)
            .put("setupGuides", JSONArray().put(JSONObject(SetupGuideJson.toJsonString(azul))))
        val imported = importBackup(root.toString())
        val guides = imported.setupGuides
        assertEquals(1, guides?.size)
        assertEquals(azul, SetupGuideJson.parse(guides!!.first()))
    }

    @Test
    fun olderBackupLeavesUserGuidesAlone() {
        assertNull(importBackup(JSONObject().put("version", 7).toString()).setupGuides)
    }
}
