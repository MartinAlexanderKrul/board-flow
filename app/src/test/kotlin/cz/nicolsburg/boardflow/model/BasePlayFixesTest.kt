package cz.nicolsburg.boardflow.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BasePlayFixesTest {

    private val two = listOf(PlayerResult("Martin", "", true), PlayerResult("Yotam", "", false))
    private val wingspan = BggGame(266192, "Wingspan", null, null)
    private val wingspanAsia = BggGame(366161, "Wingspan Asia", null, null)
    private val dune = BggGame(316554, "Dune: Imperium", null, null)
    private val uprising = BggGame(397598, "Dune: Imperium – Uprising", null, null)

    private fun play(id: String, gameId: Int, name: String, date: String = "2025-01-13", players: List<PlayerResult> = two, minutes: Int = 60) =
        LoggedPlay(id = id, gameId = gameId, gameName = name, date = date, players = players, durationMinutes = minutes, location = "", postedToBgg = true)

    @Test
    fun expansionWithoutBasePlayGetsTheBaseItsNameExtends() {
        val fixes = BasePlayFixes.find(
            listOf(play("1", 290837, "Wingspan: Oceania Expansion")),
            mapOf(290837 to listOf(wingspan, wingspanAsia)),
            knownGameIds = setOf(266192, 366161)
        )
        val fix = fixes.single()
        assertEquals(wingspan, fix.suggestedBase)
        assertNull(fix.existingBasePlay)
        assertTrue(fix.selectedByDefault)
    }

    @Test
    fun twoKnownBasesTheNameFitsBothLeavesTheChoiceToTheUser() {
        val fix = BasePlayFixes.find(
            listOf(play("1", 400000, "Dune: Imperium – Bloodlines", minutes = 0)),
            mapOf(400000 to listOf(dune, uprising)),
            knownGameIds = setOf(316554, 397598)
        ).single()
        assertNull(fix.suggestedBase)
        assertFalse(fix.selectedByDefault)
    }

    @Test
    fun baseLoggedThatDayWithAnotherResultIsMatchedInsteadOfAdded() {
        val base = play("b", 331106, "The Witcher: Old World", date = "2025-03-22", minutes = 0,
            players = listOf(PlayerResult("Martin", "", false), PlayerResult("Yotam", "", false)))
        val mages = play("m", 340000, "The Witcher: Old World – Mages", date = "2025-03-22", minutes = 180)
        val fix = BasePlayFixes.find(
            listOf(base, mages),
            mapOf(340000 to listOf(BggGame(331106, "The Witcher: Old World", null, null))),
            knownGameIds = setOf(331106)
        ).single()
        assertEquals("b", fix.existingBasePlay?.id)
        assertTrue(fix.selectedByDefault)
    }

    @Test
    fun linkedExpansionPlaysAndBulkPlayedMarksAreLeftAlone() {
        val linked = play("1", 290837, "Wingspan: Oceania Expansion").copy(expansionOf = "x")
        val bulk = play("2", 287954, "Dune: Imperium – Rise of Ix", date = "2023-01-01", minutes = 0,
            players = listOf(PlayerResult("Martin", "", false)))
        val fixes = BasePlayFixes.find(
            listOf(linked, bulk),
            mapOf(290837 to listOf(wingspan), 287954 to listOf(dune)),
            knownGameIds = setOf(266192, 316554)
        )
        assertTrue(fixes.isEmpty())
    }

    @Test
    fun basePlayCopiesTheSittingButNotTheExpansionsIdentity() {
        val expansion = play("1", 290837, "Wingspan: Oceania Expansion").copy(comments = "great", nowInStats = false)
        val base = BasePlayFixes.basePlayFor(expansion, wingspan, "new")
        assertEquals("new", base.id)
        assertEquals(266192, base.gameId)
        assertEquals(expansion.players, base.players)
        assertEquals(expansion.date, base.date)
        assertFalse(base.postedToBgg)
        assertTrue(base.nowInStats)
        assertEquals("", base.comments)
    }
}
