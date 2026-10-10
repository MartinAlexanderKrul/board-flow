package cz.nicolsburg.boardflow.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpansionPlaysTest {

    private val table = listOf(PlayerResult("Martin", "92", true), PlayerResult("Eva", "80", false))

    private fun play(
        id: String,
        gameId: Int,
        name: String,
        date: String = "2026-10-10",
        players: List<PlayerResult> = table,
        sessionId: String? = null
    ) = LoggedPlay(
        id = id, gameId = gameId, gameName = name, date = date, sessionId = sessionId,
        players = players, durationMinutes = 93, location = "Home", postedToBgg = true
    )

    @Test
    fun expansionLoggedWithItsBaseGameRidesOnTheBasePlay() {
        val linked = ExpansionPlays.link(listOf(play("1", 436126, "Finspan"), play("2", 468426, "Finspan: Sharks & Reefs")))

        val base = linked.single { it.id == "1" }
        assertNull(base.expansionOf)
        assertEquals(listOf(PlayedExpansion("2", 468426, "Finspan: Sharks & Reefs")), base.expansions)
        assertEquals("1", linked.single { it.id == "2" }.expansionOf)
        assertEquals(listOf("1"), linked.sessionPlays().map { it.id })
    }

    @Test
    fun knownExpansionTypeLinksEvenWhenTheNameDoesNotExtendTheBase() {
        val linked = ExpansionPlays.link(
            listOf(play("1", 100, "Sky Team"), play("2", 200, "Turbulence")),
            expansionGameIds = setOf(200)
        )
        assertEquals("1", linked.single { it.id == "2" }.expansionOf)
    }

    @Test
    fun differentScoresOrDatesAreSeparateSittings() {
        val otherScores = listOf(PlayerResult("Martin", "70", false), PlayerResult("Eva", "81", true))
        val linked = ExpansionPlays.link(
            listOf(
                play("1", 436126, "Finspan"),
                play("2", 468426, "Finspan: Sharks & Reefs", players = otherScores),
                play("3", 468426, "Finspan: Sharks & Reefs", date = "2026-10-09")
            )
        )
        assertTrue(linked.all { it.expansionOf == null && it.expansions.isEmpty() })
    }

    @Test
    fun twoUnrelatedGamesWithTheSameResultStayTwoPlays() {
        val linked = ExpansionPlays.link(listOf(play("1", 1, "Azul"), play("2", 2, "Patchwork")))
        assertEquals(2, linked.sessionPlays().size)
    }

    @Test
    fun matchingPlaysFromDifferentSessionsAreNotLinked() {
        val linked = ExpansionPlays.link(
            listOf(play("1", 436126, "Finspan", sessionId = "a"), play("2", 468426, "Finspan: Sharks & Reefs", sessionId = "b"))
        )
        assertEquals(2, linked.sessionPlays().size)
    }

    @Test
    fun expansionOfSubtitledBaseGameAttachesThroughTheSharedRoot() {
        val linked = ExpansionPlays.link(
            listOf(play("1", 264220, "Tainted Grail: The Fall of Avalon"), play("2", 300000, "Tainted Grail: The Last Knight")),
            expansionGameIds = setOf(300000)
        )
        assertEquals("1", linked.single { it.id == "2" }.expansionOf)
    }

    @Test
    fun relinkingReplacesStaleLinks() {
        val stale = play("1", 1, "Azul").copy(expansions = listOf(PlayedExpansion("x", 9, "Old")))
        val linked = ExpansionPlays.link(listOf(stale))
        assertTrue(linked.single().expansions.isEmpty())
    }

    @Test
    fun playOfTheExpansionCountsForThatGame() {
        val base = ExpansionPlays.link(listOf(play("1", 436126, "Finspan"), play("2", 468426, "Finspan: Sharks & Reefs")))
            .sessionPlays().single()
        assertTrue(base.includesGame(468426))
        assertTrue(base.includesGame(436126))
        assertFalse(base.includesGame(1))
    }

    @Test
    fun shortNameDropsTheBaseGamesName() {
        assertEquals("Sharks & Reefs", ExpansionPlays.shortName("Finspan: Sharks & Reefs", "Finspan"))
        assertEquals("Expansion", ExpansionPlays.shortName("Wingspan Expansion", "Wingspan"))
        assertEquals("The Last Knight", ExpansionPlays.shortName("Tainted Grail: The Last Knight", "Tainted Grail: The Fall of Avalon"))
        assertEquals("Turbulence", ExpansionPlays.shortName("Turbulence", "Sky Team"))
    }

    @Test
    fun extendsNameNeedsTheBaseAsRoot() {
        assertTrue(ExpansionPlays.extendsName("Wingspan: European Expansion", "Wingspan"))
        assertTrue(ExpansionPlays.extendsName("Wingspan Asia", "Wingspan"))
        assertFalse(ExpansionPlays.extendsName("Wingspan", "Wingspan"))
        assertFalse(ExpansionPlays.extendsName("Wingspanner", "Wingspan"))
        assertFalse(ExpansionPlays.extendsName("Azul: Summer Pavilion", "Wingspan"))
    }
}
