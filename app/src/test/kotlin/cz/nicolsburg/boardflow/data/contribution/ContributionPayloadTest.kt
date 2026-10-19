package cz.nicolsburg.boardflow.data.contribution

import cz.nicolsburg.boardflow.data.scoresheet.ScoreSheetIndex
import cz.nicolsburg.boardflow.data.scoresheet.ScoreSheets
import cz.nicolsburg.boardflow.data.setupguide.SetupGuideJson
import cz.nicolsburg.boardflow.model.SheetOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class ContributionPayloadTest {

    @Test
    fun aSheetIsSharedAsACommunitySheetTheCatalogLoads() {
        val sheet = ScoreSheets.byId("wingspan")!!.copy(id = "user-266192", origin = SheetOrigin.AI_DRAFT, reviewed = true)
        val payload = ContributionPayload.scoreSheet(sheet, "6.7.0")
        assertEquals("score-sheet", payload.getString("kind"))
        assertEquals(sheet.gameIds.first(), payload.getInt("gameId"))
        val document = payload.getJSONObject("document")
        assertEquals("community-${sheet.gameIds.first()}", document.getString("id"))
        assertNotNull(ScoreSheetIndex.parseSheet(document.toString()))
    }

    @Test
    fun aGuideIsSharedWithoutItsAuthor() {
        val dir = listOf(File("../setup-guides"), File("setup-guides")).first { it.isDirectory }
        val guide = SetupGuideJson.parse(File(dir, "191710.json").readText())
        val mine = guide.copy(provenance = guide.provenance.copy(author = "Martin", reviewed = true))
        val payload = ContributionPayload.setupGuide(mine, "6.7.0")
        assertFalse(payload.toString().contains("Martin"))
        assertEquals(guide.allGameIds.sorted(), payload.getJSONArray("gameIds").let { a -> (0 until a.length()).map(a::getInt) })
    }

    @Test
    fun theFingerprintFollowsTheContentOnly() {
        val sheet = ScoreSheets.byId("wingspan")!!
        val a = ContributionPayload.scoreSheet(sheet, "6.7.0")
        val b = ContributionPayload.scoreSheet(sheet, "6.8.0")
        assertEquals(ContributionPayload.fingerprint(a), ContributionPayload.fingerprint(b))
        val changed = ContributionPayload.scoreSheet(sheet.copy(maxPlayers = 4), "6.7.0")
        assertNotEquals(ContributionPayload.fingerprint(a), ContributionPayload.fingerprint(changed))
    }
}
