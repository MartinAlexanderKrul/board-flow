package cz.nicolsburg.boardflow.data

import cz.nicolsburg.boardflow.model.BggGame
import org.junit.Assert.assertEquals
import org.junit.Test

class BggGameSearchTest {

    private fun game(id: Int, name: String) = BggGame(id, name, null, null)

    @Test
    fun exactMatchesComeFirstThenTheRestByNameWithoutDuplicates() {
        val exact = listOf(game(359871, "Arcs"))
        val loose = listOf(game(2, "Arcs: The Blighted Reach"), game(359871, "Arcs"), game(3, "Arcade Shooter"))
        assertEquals(listOf(359871, 3, 2), BggGameSearch.merge(exact, loose).map { it.id })
    }

    @Test
    fun resultsAreCapped() {
        val loose = (1..80).map { game(it, "Game $it") }
        assertEquals(BggGameSearch.MAX_RESULTS, BggGameSearch.merge(emptyList(), loose).size)
    }
}
