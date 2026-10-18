package cz.nicolsburg.boardflow.data.scoresheet

import cz.nicolsburg.boardflow.model.CountRow
import cz.nicolsburg.boardflow.model.MajorityRow
import cz.nicolsburg.boardflow.model.PointsRow
import cz.nicolsburg.boardflow.model.ScoreGroup
import cz.nicolsburg.boardflow.model.ScoreModule
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.ScoreTiebreaker

/**
 * Every game with a score sheet. A sheet follows the game's printed score pad, row for row; its
 * look lives next to it in `ui/score/` (one design per sheet id). BGG ids match the Quick Setup
 * guide of the same game (`setup-guides/<id>.json`).
 */
object ScoreSheets {

    /** Wingspan score pad; Oceania adds the nectar row. Wingspan Asia (standalone) shares it. */
    val wingspan = ScoreSheet(
        id = "wingspan",
        gameName = "Wingspan",
        gameIds = setOf(266192, 290448, 300580, 461932, 366161),
        maxPlayers = 7,
        modules = listOf(ScoreModule("oceania", "Oceania", setOf(300580))),
        rows = listOf(
            PointsRow("birds", "Birds", hint = "Points on your bird cards"),
            PointsRow("bonus", "Bonus cards"),
            PointsRow("goals", "End-of-round goals"),
            CountRow("eggs", "Eggs", pointsEach = 1),
            CountRow("cached", "Food on cards", pointsEach = 1),
            CountRow("tucked", "Tucked cards", pointsEach = 1),
            MajorityRow(
                id = "nectar",
                label = "Nectar",
                hint = "Per habitat: most 5, second 2",
                module = "oceania",
                groups = listOf(
                    ScoreGroup("forest", "Forest"),
                    ScoreGroup("grassland", "Grassland"),
                    ScoreGroup("wetland", "Wetland")
                ),
                awards = listOf(5, 2)
            )
        ),
        tiebreakers = listOf(ScoreTiebreaker("food", "Unused food"))
    )

    /** Everdell score pad; Pearlbrook adds pearls, wonders and adornments. */
    val everdell = ScoreSheet(
        id = "everdell",
        gameName = "Everdell",
        gameIds = setOf(199792, 332398, 476125, 259996, 289056, 289057, 332390, 332397),
        maxPlayers = 6,
        modules = listOf(
            ScoreModule("pearlbrook", "Pearlbrook", setOf(259996)),
            // The base rows cover most of these; anything else they score goes in one row.
            ScoreModule("other", "Other expansions", setOf(289056, 289057, 332390, 332397))
        ),
        rows = listOf(
            PointsRow("cards", "Cards", hint = "Base points printed on the cards in your city"),
            PointsRow("tokens", "Point tokens"),
            PointsRow("prosperity", "Prosperity", hint = "Bonus points from purple cards"),
            PointsRow("journey", "Journey"),
            PointsRow("events", "Events", hint = "Basic and special events"),
            CountRow("pearls", "Pearls", pointsEach = 2, module = "pearlbrook"),
            PointsRow("wonders", "Wonders", module = "pearlbrook"),
            PointsRow("adornments", "Adornments", module = "pearlbrook"),
            PointsRow("expansion", "Expansion points", hint = "Anything the rows above don't cover", module = "other")
        ),
        tiebreakers = listOf(
            ScoreTiebreaker("events", "Events achieved"),
            ScoreTiebreaker("resources", "Leftover resources")
        )
    )

    val all: List<ScoreSheet> = listOf(wingspan, everdell)

    fun byId(id: String): ScoreSheet? = all.firstOrNull { it.id == id }

    /** The sheet for the first id that has one: the game first, then its logged expansions. */
    fun forGames(gameIds: List<Int>): ScoreSheet? =
        gameIds.firstNotNullOfOrNull { id -> all.firstOrNull { id in it.gameIds } }
}
