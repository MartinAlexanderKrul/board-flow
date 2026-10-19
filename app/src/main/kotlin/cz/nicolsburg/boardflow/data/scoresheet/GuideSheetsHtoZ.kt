package cz.nicolsburg.boardflow.data.scoresheet

import cz.nicolsburg.boardflow.model.CountRow
import cz.nicolsburg.boardflow.model.ExpeditionRow
import cz.nicolsburg.boardflow.model.MajorityRow
import cz.nicolsburg.boardflow.model.PointsRow
import cz.nicolsburg.boardflow.model.ScoreGroup
import cz.nicolsburg.boardflow.model.ScoreModule
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.ScoreTiebreaker
import cz.nicolsburg.boardflow.model.TableRow
import cz.nicolsburg.boardflow.model.TieRule

// Score sheets for the Quick Setup games scored at the end, H to Z. Rows follow each rulebook's
// end-of-game scoring; BGG ids match the game's guide (setup-guides/<id>.json).

internal val harmonies = ScoreSheet(
    id = "harmonies",
    gameName = "Harmonies",
    gameIds = setOf(414317),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("trees", "Trees"),
        PointsRow("mountains", "Mountains"),
        PointsRow("fields", "Fields"),
        PointsRow("buildings", "Buildings"),
        PointsRow("water", "Water"),
        PointsRow("animals", "Animal cards"),
        PointsRow("spirit", "Nature's Spirit", hint = "If you played with one")
    ),
    tiebreakers = listOf(ScoreTiebreaker("cubes", "Animal cubes placed"))
)

internal val itsAWonderfulWorld = ScoreSheet(
    id = "its-a-wonderful-world",
    gameName = "It's a Wonderful World",
    gameIds = setOf(271324, 298851),
    maxPlayers = 5,
    rows = listOf(
        PointsRow("gross", "Card points", hint = "Points that depend on nothing else"),
        PointsRow("combo", "Combo points", hint = "Points per card type"),
        PointsRow("generals", "Generals", hint = "1 each, plus card bonuses"),
        PointsRow("financiers", "Financiers", hint = "1 each, plus card bonuses")
    ),
    tiebreakers = listOf(
        ScoreTiebreaker("cards", "Cards in your empire"),
        ScoreTiebreaker("characters", "Character tokens")
    )
)

internal val kingdomino = ScoreSheet(
    id = "kingdomino",
    gameName = "Kingdomino",
    gameIds = setOf(204583),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("wheat", "Wheat fields", hint = "Squares × crowns, per territory"),
        PointsRow("forest", "Forests"),
        PointsRow("lake", "Lakes"),
        PointsRow("grassland", "Grassland"),
        PointsRow("swamp", "Swamps"),
        PointsRow("mine", "Mines"),
        PointsRow("middle", "Middle Kingdom", hint = "10 if your castle is in the centre (variant)"),
        PointsRow("harmony", "Harmony", hint = "5 for a complete kingdom (variant)")
    ),
    tiebreakers = listOf(ScoreTiebreaker("territory", "Largest territory"))
)

internal val kutnaHora = ScoreSheet(
    id = "kutna-hora",
    gameName = "Kutná Hora",
    gameIds = setOf(385610),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("track", "Score track", hint = "Points scored during the game"),
        PointsRow("stars", "Star rows", hint = "Most stars in each row of the board"),
        CountRow("income", "Income", per = 10, hint = "1 point per 10 income"),
        PointsRow("reputation", "Reputation", negative = true),
        CountRow("extras", "Extras", hint = "Building tiles in slots, unbuilt plots, unused pelicans")
    ),
    tiebreakers = listOf(ScoreTiebreaker("groschen", "Groschen left"))
)

internal val leHavre = ScoreSheet(
    id = "le-havre",
    gameName = "Le Havre",
    gameIds = setOf(35677),
    maxPlayers = 5,
    rows = listOf(
        CountRow("francs", "Francs"),
        PointsRow("buildings", "Buildings", hint = "Value of your buildings"),
        PointsRow("ships", "Ships", hint = "Value of your ships"),
        CountRow("loans", "Loans", pointsEach = -7, hint = "Each unpaid loan costs 7")
    )
)

private val lostCitiesColors = listOf(
    ScoreGroup("yellow", "Yellow"), ScoreGroup("blue", "Blue"), ScoreGroup("white", "White"),
    ScoreGroup("green", "Green"), ScoreGroup("red", "Red")
)

internal val lostCities = ScoreSheet(
    id = "lost-cities",
    gameName = "Lost Cities",
    gameIds = setOf(50),
    maxPlayers = 2,
    modules = listOf(ScoreModule("purple", "Purple expedition")),
    rows = listOf(
        ExpeditionRow("expeditions", "Expeditions", groups = lostCitiesColors, hint = "(Sum - 20) × (1 + wagers), plus 20 for 8 cards"),
        ExpeditionRow("purple", "Purple expedition", groups = listOf(ScoreGroup("purple", "Purple")), module = "purple")
    )
)

internal val lostRuinsOfArnak = ScoreSheet(
    id = "lost-ruins-of-arnak",
    gameName = "Lost Ruins of Arnak",
    gameIds = setOf(312484, 341254, 382350, 439816, 349092),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("research", "Research", hint = "Research tokens and the Lost Temple"),
        PointsRow("temple", "Temple tiles"),
        PointsRow("idols", "Idols", hint = "3 each, plus empty idol slots"),
        CountRow("guardians", "Guardians", pointsEach = 5),
        PointsRow("cards", "Items and artifacts"),
        CountRow("fear", "Fear cards", pointsEach = -1),
        CountRow("fear-tiles", "Fear tiles", pointsEach = -2)
    ),
    tiebreakers = listOf(
        ScoreTiebreaker("temple-first", "Reached the Lost Temple first (1 = yes)"),
        ScoreTiebreaker("research", "Research", fromKey = "research")
    )
)

internal val lordsOfWaterdeep = ScoreSheet(
    id = "lords-of-waterdeep",
    gameName = "Lords of Waterdeep",
    gameIds = setOf(110327),
    maxPlayers = 5,
    rows = listOf(
        PointsRow("track", "Score track", hint = "Points scored during the game"),
        PointsRow("lord", "Lord card"),
        CountRow("adventurers", "Adventurers", hint = "Cubes left in your tavern"),
        CountRow("gold", "Gold", per = 2, hint = "1 point per 2")
    )
)

internal val onMars = ScoreSheet(
    id = "on-mars",
    gameName = "On Mars",
    gameIds = setOf(184267),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("track", "Score track", hint = "Points scored during the game"),
        TableRow("progress", "Progress cubes", steps = listOf(1 to 1, 2 to 2, 3 to 4, 4 to 7, 5 to 11)),
        CountRow("ships", "Ships in hangar", pointsEach = 3),
        PointsRow("colonists", "Colonists", hint = "Next to your highest colonist"),
        PointsRow("tech", "Tech tiles"),
        PointsRow("advanced", "Advanced buildings"),
        PointsRow("blueprints", "Unbuilt blueprints", hint = "3 per level 1, 5 per level 3", subtract = true),
        PointsRow("scientists", "Scientists"),
        PointsRow("contracts", "Earth contracts", negative = true)
    ),
    tiebreakers = listOf(
        ScoreTiebreaker("crystals", "Crystals"),
        ScoreTiebreaker("buildings", "Advanced buildings"),
        ScoreTiebreaker("cubes", "Progress cubes")
    )
)

internal val orleans = ScoreSheet(
    id = "orleans",
    gameName = "Orléans",
    gameIds = setOf(164928),
    maxPlayers = 4,
    rows = listOf(
        CountRow("coins", "Coins"),
        PointsRow("goods", "Goods", hint = "Brocade 5, wool 4, wine 3, cheese 2, grain 1"),
        PointsRow("development", "Stations and citizens", hint = "Their count × development status")
    ),
    tiebreakers = listOf(ScoreTiebreaker("development", "Development track"))
)

internal val paladins = ScoreSheet(
    id = "paladins",
    gameName = "Paladins of the West Kingdom",
    gameIds = setOf(266810),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("orders", "King's Orders"),
        PointsRow("attributes", "Attribute tracks"),
        PointsRow("commissions", "Commissions"),
        PointsRow("fortifies", "Fortifies"),
        PointsRow("develops", "Develops"),
        PointsRow("garrisons", "Garrisons"),
        PointsRow("absolves", "Absolves"),
        PointsRow("converts", "Converts"),
        PointsRow("debts", "Debts", hint = "Paid minus unpaid", negative = true),
        CountRow("leftovers", "Silver and provisions", per = 3, hint = "1 point per 3")
    ),
    tiebreakers = listOf(
        ScoreTiebreaker("orders", "King's Orders points", fromKey = "orders"),
        ScoreTiebreaker("suspicion", "Suspicion", preferHighest = false)
    )
)

internal val parks = ScoreSheet(
    id = "parks",
    gameName = "Parks",
    gameIds = setOf(266524),
    maxPlayers = 5,
    rows = listOf(
        PointsRow("parks", "Parks"),
        PointsRow("photos", "Photos"),
        PointsRow("year", "Year card", hint = "Your personal bonus")
    ),
    tiebreakers = listOf(ScoreTiebreaker("visited", "Parks visited"))
)

internal val patchwork = ScoreSheet(
    id = "patchwork",
    gameName = "Patchwork",
    gameIds = setOf(163412),
    maxPlayers = 2,
    rows = listOf(
        CountRow("buttons", "Buttons"),
        PointsRow("bonus", "7×7 bonus", hint = "7 points"),
        CountRow("empty", "Empty spaces", pointsEach = -2)
    ),
    tiebreakers = listOf(ScoreTiebreaker("first", "Reached the end first (1 = yes)"))
)

internal val puertoRico = ScoreSheet(
    id = "puerto-rico",
    gameName = "Puerto Rico",
    gameIds = setOf(3076),
    maxPlayers = 5,
    rows = listOf(
        PointsRow("chips", "Victory point chips"),
        PointsRow("buildings", "Buildings"),
        PointsRow("large", "Large building bonuses")
    ),
    tiebreakers = listOf(ScoreTiebreaker("money", "Doubloons and goods"))
)

internal val raceForTheGalaxy = ScoreSheet(
    id = "race-for-the-galaxy",
    gameName = "Race for the Galaxy",
    gameIds = setOf(28143),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("cards", "Card points", hint = "Worlds and developments"),
        PointsRow("chips", "Victory point chips"),
        PointsRow("six", "6-cost developments")
    ),
    tiebreakers = listOf(ScoreTiebreaker("hand", "Cards in hand and goods"))
)

internal val revive = ScoreSheet(
    id = "revive",
    gameName = "Revive",
    gameIds = setOf(332772),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("track", "Score track", hint = "Blue points scored during the game"),
        PointsRow("artifacts", "Artifacts"),
        PointsRow("artifact-card", "Artifact card"),
        PointsRow("technologies", "Technologies"),
        PointsRow("progress", "Progress track"),
        PointsRow("locations", "Populated locations")
    ),
    tiebreakers = listOf(
        ScoreTiebreaker("resources", "Unscored resources"),
        ScoreTiebreaker("machines", "Machine tracks combined")
    )
)

internal val sagrada = ScoreSheet(
    id = "sagrada",
    gameName = "Sagrada",
    gameIds = setOf(199561),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("public", "Public objectives"),
        PointsRow("private", "Private objective"),
        CountRow("favor", "Favor tokens"),
        CountRow("open", "Open spaces", pointsEach = -1)
    ),
    tiebreakers = listOf(
        ScoreTiebreaker("private", "Private objective", fromKey = "private"),
        ScoreTiebreaker("favor", "Favor tokens", fromKey = "favor")
    )
)

internal val scythe = ScoreSheet(
    id = "scythe",
    gameName = "Scythe",
    gameIds = setOf(169786, 199727, 223555),
    maxPlayers = 7,
    rows = listOf(
        CountRow("coins", "Coins in hand"),
        PointsRow("stars", "Stars", hint = "Coins for your popularity tier"),
        PointsRow("territories", "Territories"),
        PointsRow("resources", "Resources", hint = "Per 2 resources"),
        PointsRow("structures", "Structure bonus")
    ),
    tiebreakers = listOf(
        ScoreTiebreaker("units", "Units and structures"),
        ScoreTiebreaker("power", "Power"),
        ScoreTiebreaker("popularity", "Popularity"),
        ScoreTiebreaker("owned", "Resources owned"),
        ScoreTiebreaker("controlled", "Territories controlled"),
        ScoreTiebreaker("placed", "Stars placed")
    )
)

internal val seti = ScoreSheet(
    id = "seti",
    gameName = "SETI",
    gameIds = setOf(418059),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("track", "Score track", hint = "Points scored during the game"),
        PointsRow("gold", "Gold scoring tiles", hint = "Sets for each tile you marked")
    )
)

internal val stoneAge = ScoreSheet(
    id = "stone-age",
    gameName = "Stone Age",
    gameIds = setOf(34635),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("track", "Score track", hint = "Points scored during the game"),
        PointsRow("culture", "Culture sets", hint = "Each set of different green cards: count squared"),
        PointsRow("farmers", "Farmers", hint = "× agriculture level"),
        PointsRow("toolmakers", "Tool makers", hint = "× tool value"),
        PointsRow("builders", "Hut builders", hint = "× huts"),
        PointsRow("shamans", "Shamans", hint = "× people"),
        CountRow("resources", "Resources")
    ),
    tiebreakers = listOf(ScoreTiebreaker("sum", "Agriculture + tools + people"))
)

internal val sushiGo = ScoreSheet(
    id = "sushi-go",
    gameName = "Sushi Go!",
    gameIds = setOf(133473),
    maxPlayers = 5,
    rows = listOf(
        PointsRow("round-1", "Round 1"),
        PointsRow("round-2", "Round 2"),
        PointsRow("round-3", "Round 3"),
        MajorityRow(
            id = "pudding", label = "Pudding", groups = listOf(ScoreGroup("count", "Puddings")),
            awards = listOf(6), fewestPenalty = 6,
            hint = "Most 6, fewest lose 6 (not with 2 players); ties split"
        )
    ),
    tiebreakers = listOf(ScoreTiebreaker("pudding", "Puddings", fromKey = "pudding.count"))
)

internal val sushiGoParty = ScoreSheet(
    id = "sushi-go-party",
    gameName = "Sushi Go Party!",
    gameIds = setOf(192291),
    maxPlayers = 8,
    rows = listOf(
        PointsRow("round-1", "Round 1"),
        PointsRow("round-2", "Round 2"),
        PointsRow("round-3", "Round 3"),
        PointsRow("dessert", "Dessert", negative = true)
    ),
    tiebreakers = listOf(ScoreTiebreaker("dessert", "Dessert cards"))
)

internal val takenoko = ScoreSheet(
    id = "takenoko",
    gameName = "Takenoko",
    gameIds = setOf(70919),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("plots", "Plot objectives"),
        PointsRow("gardener", "Gardener objectives"),
        PointsRow("panda", "Panda objectives"),
        PointsRow("emperor", "Emperor", hint = "2 points")
    ),
    tiebreakers = listOf(ScoreTiebreaker("panda", "Panda objectives", fromKey = "panda"))
)

internal val terraMystica = ScoreSheet(
    id = "terra-mystica",
    gameName = "Terra Mystica",
    gameIds = setOf(120677),
    maxPlayers = 5,
    rows = mysticaRows(
        listOf(ScoreGroup("fire", "Fire"), ScoreGroup("water", "Water"), ScoreGroup("earth", "Earth"), ScoreGroup("air", "Air")),
        "Cult tracks", 3, "After converting resources: 1 point per 3"
    )
)

internal val terraformingMars = ScoreSheet(
    id = "terraforming-mars",
    gameName = "Terraforming Mars",
    gameIds = setOf(167791),
    maxPlayers = 5,
    rows = listOf(
        PointsRow("tr", "Terraform rating"),
        PointsRow("awards", "Awards", hint = "First 5, second 2; ties all score"),
        CountRow("milestones", "Milestones", pointsEach = 5),
        CountRow("greenery", "Greenery tiles"),
        PointsRow("cities", "Cities", hint = "1 per adjacent greenery"),
        PointsRow("cards", "Card points")
    ),
    tiebreakers = listOf(ScoreTiebreaker("megacredits", "Megacredits"))
)

private val longestPath = { label: String, hint: String ->
    MajorityRow(
        id = "longest", label = label, groups = listOf(ScoreGroup("length", "Longest path length")),
        awards = listOf(10), ties = TieRule.ALL, hint = hint
    )
}

internal val ticketToRide = ScoreSheet(
    id = "ticket-to-ride",
    gameName = "Ticket to Ride",
    gameIds = setOf(9209),
    maxPlayers = 5,
    rows = listOf(
        PointsRow("routes", "Routes", hint = "Points scored for claimed routes"),
        PointsRow("completed", "Completed tickets"),
        PointsRow("failed", "Failed tickets", subtract = true),
        longestPath("Longest path", "10 for the longest continuous path; ties all score")
    ),
    tiebreakers = listOf(ScoreTiebreaker("tickets", "Completed tickets (count)"))
)

internal val ticketToRideEurope = ScoreSheet(
    id = "ticket-to-ride-europe",
    gameName = "Ticket to Ride: Europe",
    gameIds = setOf(14996, 329841),
    maxPlayers = 5,
    rows = listOf(
        PointsRow("routes", "Routes", hint = "Points scored for claimed routes"),
        PointsRow("completed", "Completed tickets"),
        PointsRow("failed", "Failed tickets", subtract = true),
        CountRow("stations", "Unused stations", pointsEach = 4),
        longestPath("European Express", "10 for the longest continuous path; ties all score")
    ),
    tiebreakers = listOf(
        ScoreTiebreaker("tickets", "Completed tickets (count)"),
        ScoreTiebreaker("stations-used", "Stations used", preferHighest = false)
    )
)

internal val welcomeTo = ScoreSheet(
    id = "welcome-to",
    gameName = "Welcome To...",
    gameIds = setOf(233867),
    maxPlayers = 12,
    rows = listOf(
        PointsRow("plans", "City plans"),
        PointsRow("parks", "Parks"),
        PointsRow("pools", "Pools"),
        MajorityRow(
            id = "temps", label = "Temp agency", groups = listOf(ScoreGroup("boxes", "Boxes crossed off")),
            awards = listOf(7, 4, 1), ties = TieRule.ALL, hint = "Most 7, then 4, then 1; ties score the same"
        ),
        PointsRow("estates", "Estates"),
        PointsRow("bis", "Bis", subtract = true),
        PointsRow("refusals", "Permit refusals", subtract = true)
    ),
    tiebreakers = listOf(ScoreTiebreaker("estates", "Completed estates"))
)
