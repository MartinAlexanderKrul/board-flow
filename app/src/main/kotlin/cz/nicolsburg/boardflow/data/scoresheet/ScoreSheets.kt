package cz.nicolsburg.boardflow.data.scoresheet

import cz.nicolsburg.boardflow.model.CountRow
import cz.nicolsburg.boardflow.model.MajorityRow
import cz.nicolsburg.boardflow.model.PointsRow
import cz.nicolsburg.boardflow.model.ScoreGroup
import cz.nicolsburg.boardflow.model.ScoreModule
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.ScoreTiebreaker
import cz.nicolsburg.boardflow.model.TieRule

/**
 * Every game with a score sheet. A sheet follows the game's printed score pad and its rulebook's
 * end-of-game scoring, row for row; its look lives next to it in `ui/score/` (one design per sheet
 * id). BGG ids match the Quick Setup guide of the same game (`setup-guides/<id>.json`).
 *
 * Expansions that only add cards or point tokens (Wingspan European; Everdell Mistwood, Legends,
 * Extra! Extra!, Rugwort and the collector's packs) need no rows of their own: their points land
 * in the base rows. They still open their game's sheet.
 */
object ScoreSheets {

    /** Wingspan and Wingspan Asia: Oceania nectar, Americas hummingbirds, Asia's Duet mode. */
    val wingspan = ScoreSheet(
        id = "wingspan",
        gameName = "Wingspan",
        gameIds = setOf(266192, 290448, 300580, 461932, 366161),
        maxPlayers = 7,
        modules = listOf(
            ScoreModule("oceania", "Oceania", setOf(300580)),
            ScoreModule("americas", "Americas", setOf(461932)),
            ScoreModule("duet", "Duet mode")
        ),
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
            ),
            PointsRow(
                "hummingbirds", "Hummingbirds",
                hint = "Points of your tokens on the hummingbird track; can be below 0",
                module = "americas", negative = true
            ),
            CountRow(
                "duet", "Duet tokens", pointsEach = 1,
                hint = "Largest connected group of your tokens on the Duet map",
                module = "duet"
            )
        ),
        tiebreakers = listOf(ScoreTiebreaker("food", "Unused food"))
    )

    /** Everdell and the expansions played with it. */
    val everdell = ScoreSheet(
        id = "everdell",
        gameName = "Everdell",
        gameIds = setOf(
            199792, 332398, 476125, 259996, 289056, 289057, 332390, 332397,
            297972, 297973, 261142, 289405, 292794
        ),
        maxPlayers = 6,
        modules = listOf(
            ScoreModule("pearlbrook", "Pearlbrook", setOf(259996, 292794)),
            ScoreModule("spirecrest", "Spirecrest", setOf(289056)),
            ScoreModule("bellfaire", "Bellfaire", setOf(289057)),
            ScoreModule("newleaf", "Newleaf", setOf(332390))
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
            PointsRow(
                "expedition", "Expedition",
                hint = "Map tiles your Traveler paid to reach", module = "spirecrest"
            ),
            PointsRow(
                "discoveries", "Discoveries",
                hint = "End-game points on Trails and Cities", module = "spirecrest"
            ),
            MajorityRow(
                id = "garland",
                label = "Garland award",
                hint = "Most cards of the Garland's type 6, second 3; ties all score",
                module = "bellfaire",
                groups = listOf(ScoreGroup("cards", "Cards of the type")),
                awards = listOf(6, 3),
                ties = TieRule.ALL
            ),
            PointsRow("visitors", "Visitors", hint = "Visitor cards whose goal you met", module = "newleaf")
        ),
        tiebreakers = listOf(
            ScoreTiebreaker("events", "Events achieved"),
            ScoreTiebreaker("resources", "Leftover resources")
        )
    )

    /** Everdell Farshore: treasures, maps and the ship's final space. */
    val farshore = ScoreSheet(
        id = "farshore",
        gameName = "Everdell Farshore",
        gameIds = setOf(394106),
        maxPlayers = 4,
        rows = listOf(
            PointsRow("cards", "Cards", hint = "Base points printed on the cards in your city"),
            PointsRow("prosperity", "Prosperity", hint = "Bonus points from purple cards"),
            PointsRow("tokens", "Point tokens"),
            CountRow("treasures", "Treasures", pointsEach = 2, hint = "Unspent treasures"),
            PointsRow("maps", "Maps", hint = "Point symbols on your maps × number of maps"),
            PointsRow("ship", "Ship", hint = "Points at your ship's final space")
        ),
        tiebreakers = listOf(
            ScoreTiebreaker("ship", "Ship moved furthest", fromKey = "ship"),
            ScoreTiebreaker("maps", "Maps"),
            ScoreTiebreaker("treasures", "Treasures", fromKey = "treasures"),
            ScoreTiebreaker("resources", "Leftover resources")
        )
    )

    /** Everdell Duo, competitive game. */
    val everdellDuo = ScoreSheet(
        id = "everdell-duo",
        gameName = "Everdell Duo",
        gameIds = setOf(425005),
        maxPlayers = 2,
        rows = listOf(
            PointsRow("cards", "Cards", hint = "Base points printed on the cards in your city"),
            PointsRow("prosperity", "Prosperity", hint = "Bonus points from purple cards"),
            PointsRow("tokens", "Point tokens"),
            PointsRow("events", "Events", hint = "Event tiles, Journey included")
        ),
        tiebreakers = listOf(
            ScoreTiebreaker("events", "Events achieved"),
            ScoreTiebreaker("resources", "Leftover resources"),
            ScoreTiebreaker("city", "Cards in city"),
            ScoreTiebreaker("hand", "Cards in hand")
        )
    )

    /** Everdell Silverfrost: quests and the snow reward chosen at setup. */
    val silverfrost = ScoreSheet(
        id = "silverfrost",
        gameName = "Everdell Silverfrost",
        gameIds = setOf(436821),
        maxPlayers = 4,
        modules = listOf(
            ScoreModule("flurries", "Flurries", group = "Snow reward", defaultEnabled = true),
            ScoreModule("ice-storm", "Ice Storm", group = "Snow reward")
        ),
        rows = listOf(
            PointsRow("cards", "Cards", hint = "Base points printed on the cards in your city"),
            PointsRow("prosperity", "Prosperity", hint = "Bonus points from purple cards"),
            PointsRow("quests", "Quests"),
            MajorityRow(
                id = "snow-flurries",
                label = "Snow",
                hint = "1 per Snow cleared; most cleared 5 more, ties all score",
                module = "flurries",
                groups = listOf(ScoreGroup("cleared", "Snow cleared")),
                awards = listOf(5),
                ties = TieRule.ALL,
                pointsEach = 1,
                inputPrefix = "snow"
            ),
            MajorityRow(
                id = "snow-ice-storm",
                label = "Snow",
                hint = "Most Snow cleared 12; ties all score",
                module = "ice-storm",
                groups = listOf(ScoreGroup("cleared", "Snow cleared")),
                awards = listOf(12),
                ties = TieRule.ALL,
                inputPrefix = "snow"
            ),
            PointsRow("tokens", "Point tokens"),
            PointsRow("journey", "Journey")
        ),
        tiebreakers = listOf(
            ScoreTiebreaker("snow", "Snow cleared", fromKey = "snow.cleared"),
            ScoreTiebreaker("quests", "Quests achieved"),
            ScoreTiebreaker("fire", "Leftover Fire"),
            ScoreTiebreaker("resources", "Leftover resources")
        )
    )

    /** Everdell Emerland: artifacts, the Lost City path and realms. */
    val emerland = ScoreSheet(
        id = "emerland",
        gameName = "Everdell Emerland",
        gameIds = setOf(455843),
        maxPlayers = 4,
        rows = listOf(
            PointsRow("cards", "Cards", hint = "Base points printed on the Jungle cards in your city"),
            PointsRow("prosperity", "Prosperity", hint = "Bonus points from purple cards"),
            PointsRow("tokens", "Point tokens"),
            PointsRow("artifacts", "Artifacts"),
            PointsRow("lost-city", "Lost City", hint = "Your Archaeologist's space, if you reached the Lost City"),
            PointsRow("realms", "Realms"),
            CountRow(
                "resources", "Leftover resources", pointsEach = 1, per = 2,
                hint = "1 point per 2, emeralds included"
            )
        ),
        tiebreakers = listOf(
            ScoreTiebreaker("upgraded", "Upgraded cards"),
            ScoreTiebreaker("lost-city", "Furthest on the Lost City path", fromKey = "lost-city"),
            ScoreTiebreaker("realms", "Realms"),
            ScoreTiebreaker("resources", "Leftover resources", fromKey = "resources")
        )
    )

    /** Finspan; Sharks & Reefs adds coral and reef bonuses. */
    val finspan = ScoreSheet(
        id = "finspan",
        gameName = "Finspan",
        gameIds = setOf(436126, 468426),
        maxPlayers = 5,
        modules = listOf(ScoreModule("sharks", "Sharks & Reefs", setOf(468426))),
        rows = listOf(
            PointsRow("achievements", "Achievements", hint = "Weeks 1-3, side B bonuses included"),
            PointsRow("game-end", "Game end abilities", hint = "Yellow GAME END points on visible fish"),
            PointsRow("fish", "Fish", hint = "Points printed on visible fish"),
            CountRow("consumed", "Consumed fish", hint = "Cards and forage under your fish"),
            CountRow("eggs", "Eggs"),
            CountRow("young", "Young"),
            CountRow("schools", "Schools", pointsEach = 6),
            CountRow("coral", "Coral", module = "sharks"),
            PointsRow("reefs", "Complete reefs", hint = "Blue 6, purple 8, green 5", module = "sharks")
        ),
        tiebreakers = listOf(ScoreTiebreaker("hand", "Fish cards in hand"))
    )

    /** Wyrmspan; Dragon Academy adds fledglings. */
    val wyrmspan = ScoreSheet(
        id = "wyrmspan",
        gameName = "Wyrmspan",
        gameIds = setOf(410201, 453406),
        maxPlayers = 5,
        modules = listOf(ScoreModule("academy", "Dragon Academy", setOf(453406))),
        rows = listOf(
            PointsRow("guild", "Dragon Guild", hint = "Points under your markers on the Dragon Guild"),
            PointsRow("dragons", "Dragons", hint = "Points printed on visible dragons"),
            PointsRow("end-game", "End game abilities"),
            CountRow("eggs", "Eggs"),
            CountRow("cached", "Cached resources"),
            CountRow("tucked", "Tucked cards"),
            PointsRow("objectives", "Public objectives", hint = "Round objectives; ties all score"),
            CountRow("coins", "Coins"),
            CountRow("items", "Other items", per = 4, hint = "Resources, dragon and cave cards left: 1 per 4"),
            CountRow(
                "fledglings", "Fledglings", squared = true,
                hint = "Each scores 1 per fledgling you have: 3 score 9", module = "academy"
            )
        ),
        tiebreakers = listOf(ScoreTiebreaker("dragons", "Visible dragons"))
    )

    val all: List<ScoreSheet> = listOf(wingspan, everdell, farshore, everdellDuo, silverfrost, emerland, finspan, wyrmspan)

    fun byId(id: String): ScoreSheet? = all.firstOrNull { it.id == id }

    /** The sheet for the first id that has one: the game first, then its logged expansions. */
    fun forGames(gameIds: List<Int>): ScoreSheet? =
        gameIds.firstNotNullOfOrNull { id -> all.firstOrNull { id in it.gameIds } }
}
