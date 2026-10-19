package cz.nicolsburg.boardflow.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BigBoxTest {

    @Test
    fun rootsAreTheBaseGameNames() {
        assertEquals(listOf("Catan"), BigBox.roots("Catan: Big Box"))
        assertEquals(listOf("Carcassonne"), BigBox.roots("Carcassonne Big Box 6"))
        assertEquals(listOf("Dominion (Second Edition)", "Dominion"), BigBox.roots("Dominion (Second Edition) Big Box"))
        assertEquals(emptyList<String>(), BigBox.roots("Catan"))
    }

    @Test
    fun aBigBoxCoversTheExpansionsOfTheGameItHolds() {
        assertTrue(BigBox.holds("Catan: Big Box", "Catan"))
        assertTrue(BigBox.covers("Catan: Big Box", "Catan: Explorers & Pirates"))
        assertTrue(BigBox.covers("Dominion (Second Edition) Big Box", "Dominion: Seaside"))
        assertFalse(BigBox.covers("Catan: Big Box", "Catan"))
        assertFalse(BigBox.covers("Catan: Big Box", "Carcassonne: Inns & Cathedrals"))
        assertFalse(BigBox.covers("Catan", "Catan: Explorers & Pirates"))
    }

    @Test
    fun anExpansionPlayedWithABigBoxRidesOnTheBigBoxPlay() {
        val table = listOf(PlayerResult("Martin", "17", true), PlayerResult("Eva", "12", false))
        fun play(id: String, gameId: Int, name: String) = LoggedPlay(
            id = id, gameId = gameId, gameName = name, date = "2026-10-19", players = table,
            durationMinutes = 120, location = "Home", postedToBgg = true
        )
        // BGG says Explorers & Pirates expands Catan (13), which was not played here.
        val linked = ExpansionPlays.link(
            listOf(play("1", 191710, "Catan: Big Box"), play("2", 135378, "Catan: Explorers & Pirates")),
            expansionGameIds = setOf(135378),
            baseGamesOf = mapOf(135378 to setOf(13))
        )

        assertEquals("1", linked.single { it.id == "2" }.expansionOf)
        assertEquals(listOf("1"), linked.sessionPlays().map { it.id })
        assertEquals("Explorers & Pirates", ExpansionPlays.shortName("Catan: Explorers & Pirates", "Catan: Big Box"))
        assertEquals("Inns & Cathedrals", ExpansionPlays.shortName("Carcassonne: Inns & Cathedrals", "Carcassonne Big Box 6"))
    }
}
