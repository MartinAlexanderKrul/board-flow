package cz.nicolsburg.boardflow.data.scoresheet

import cz.nicolsburg.boardflow.model.CountRow
import cz.nicolsburg.boardflow.model.MajorityRow
import cz.nicolsburg.boardflow.model.PointsRow
import cz.nicolsburg.boardflow.model.ScoreGroup
import cz.nicolsburg.boardflow.model.ScoreModule
import cz.nicolsburg.boardflow.model.ScoreRow
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.ScoreTiebreaker
import cz.nicolsburg.boardflow.model.SetsRow
import cz.nicolsburg.boardflow.model.TableRow
import cz.nicolsburg.boardflow.model.TieRule

// Score sheets for the Quick Setup games scored at the end, A to G. Rows follow each rulebook's
// end-of-game scoring; BGG ids match the game's guide (setup-guides/<id>.json).

internal val sevenWonders = ScoreSheet(
    id = "7-wonders",
    gameName = "7 Wonders",
    gameIds = setOf(68448, 92539, 111661, 154638, 247315, 133993, 378839, 83445),
    maxPlayers = 7,
    modules = listOf(
        ScoreModule("leaders", "Leaders", setOf(92539)),
        ScoreModule("cities", "Cities", setOf(111661)),
        ScoreModule("armada", "Armada", setOf(247315)),
        ScoreModule("babel", "Babel", setOf(154638)),
        ScoreModule("edifice", "Edifice", setOf(378839))
    ),
    rows = listOf(
        PointsRow("military", "Military", hint = "Victory tokens minus defeat tokens", negative = true),
        CountRow("treasury", "Treasury", per = 3, hint = "Coins: 1 point per 3"),
        PointsRow("wonder", "Wonder", hint = "Stages built"),
        PointsRow("civilian", "Civilian", hint = "Blue cards"),
        SetsRow(
            "science", "Science", hint = "Green cards: each symbol squared, plus 7 per set of three",
            groups = listOf(ScoreGroup("compass", "Compasses"), ScoreGroup("tablet", "Tablets"), ScoreGroup("gear", "Gears")),
            setBonus = 7
        ),
        PointsRow("commercial", "Commercial", hint = "Yellow cards"),
        PointsRow("guilds", "Guilds", hint = "Purple cards"),
        PointsRow("leaders", "Leaders", module = "leaders"),
        PointsRow("cities", "Cities", hint = "Black cards", module = "cities"),
        PointsRow("debt", "Debt", hint = "Debt tokens", module = "cities", subtract = true),
        PointsRow("armada", "Armada", hint = "Naval conflicts and islands", module = "armada"),
        PointsRow("babel", "Babel", hint = "Tower and Great Projects", module = "babel"),
        PointsRow("edifice", "Edifices", module = "edifice")
    ),
    tiebreakers = listOf(ScoreTiebreaker("coins", "Coins", fromKey = "treasury"))
)

internal val sevenWondersDuel = ScoreSheet(
    id = "7-wonders-duel",
    gameName = "7 Wonders Duel",
    gameIds = setOf(173346, 202976, 309116),
    maxPlayers = 2,
    modules = listOf(
        ScoreModule("pantheon", "Pantheon", setOf(202976)),
        ScoreModule("agora", "Agora", setOf(309116))
    ),
    rows = listOf(
        PointsRow("military", "Military", hint = "Zone of the conflict pawn: 0, 2, 5 or 10"),
        CountRow("treasury", "Treasury", per = 3, hint = "Coins: 1 point per 3"),
        PointsRow("wonders", "Wonders"),
        PointsRow("civilian", "Civilian", hint = "Blue cards"),
        PointsRow("scientific", "Scientific", hint = "Green cards"),
        PointsRow("commercial", "Commercial", hint = "Yellow cards"),
        PointsRow("guilds", "Guilds", hint = "Purple cards"),
        PointsRow("progress", "Progress tokens"),
        PointsRow("pantheon", "Divinities", module = "pantheon"),
        PointsRow("agora", "Senate", hint = "Agora decrees and senators", module = "agora")
    ),
    tiebreakers = listOf(ScoreTiebreaker("civilian", "Civilian points", fromKey = "civilian"))
)

internal val feastForOdin = ScoreSheet(
    id = "a-feast-for-odin",
    gameName = "A Feast for Odin",
    gameIds = setOf(177736),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("ships", "Ships", hint = "Whaling boat 3, knarr 5, longship 8"),
        PointsRow("emigration", "Emigration", hint = "Knarr 18, longship 21"),
        PointsRow("exploration", "Exploration boards"),
        PointsRow("buildings", "Sheds and houses"),
        PointsRow("animals", "Sheep and cattle", hint = "Sheep 2 (3 pregnant), cattle 3 (4 pregnant)"),
        PointsRow("occupations", "Occupations"),
        CountRow("silver", "Silver", hint = "In your supply"),
        PointsRow("income", "Final income"),
        PointsRow("crown", "English Crown", hint = "2 points"),
        PointsRow("negative", "Negative spaces", hint = "Uncovered minus points on your boards", subtract = true),
        PointsRow("thing", "Thing penalties", subtract = true)
    )
)

private val agricolaRows: List<ScoreRow> = listOf(
    TableRow("fields", "Fields", steps = listOf(2 to 1, 3 to 2, 4 to 3, 5 to 4), below = -1),
    TableRow("pastures", "Pastures", steps = listOf(1 to 1, 2 to 2, 3 to 3, 4 to 4), below = -1),
    TableRow("grain", "Grain", steps = listOf(1 to 1, 4 to 2, 6 to 3, 8 to 4), below = -1, hint = "In fields and supply"),
    TableRow("vegetables", "Vegetables", steps = listOf(1 to 1, 2 to 2, 3 to 3, 4 to 4), below = -1, hint = "In fields and supply"),
    TableRow("sheep", "Sheep", steps = listOf(1 to 1, 4 to 2, 6 to 3, 8 to 4), below = -1),
    TableRow("boar", "Wild boar", steps = listOf(1 to 1, 3 to 2, 5 to 3, 7 to 4), below = -1),
    TableRow("cattle", "Cattle", steps = listOf(1 to 1, 2 to 2, 4 to 3, 6 to 4), below = -1),
    CountRow("unused", "Unused spaces", pointsEach = -1),
    CountRow("stables", "Fenced stables"),
    CountRow("clay", "Clay rooms"),
    CountRow("stone", "Stone rooms", pointsEach = 2),
    CountRow("family", "Family members", pointsEach = 3),
    PointsRow("cards", "Card points", hint = "Printed on improvements and occupations"),
    PointsRow("bonus", "Bonus points"),
    CountRow("begging", "Begging cards", pointsEach = -3)
)

internal val agricola = ScoreSheet(
    id = "agricola",
    gameName = "Agricola",
    gameIds = setOf(31260),
    maxPlayers = 5,
    rows = agricolaRows
)

internal val agricolaRevised = ScoreSheet(
    id = "agricola-revised",
    gameName = "Agricola (Revised Edition)",
    gameIds = setOf(200680),
    maxPlayers = 4,
    rows = agricolaRows,
    tiebreakers = listOf(ScoreTiebreaker("building", "Building resources left"))
)

/** Age of Innovation and Terra Mystica score the same way at the end. */
internal fun mysticaRows(tracks: List<ScoreGroup>, trackLabel: String, coinsPer: Int, coinsHint: String): List<ScoreRow> = listOf(
    PointsRow("track", "Score track", hint = "Points scored during the game"),
    MajorityRow(
        id = "tracks", label = trackLabel, groups = tracks, awards = listOf(8, 4, 2),
        hint = "Per track: highest 8, second 4, third 2; ties split"
    ),
    MajorityRow(
        id = "area", label = "Largest area", groups = listOf(ScoreGroup("buildings", "Buildings connected")),
        awards = listOf(18, 12, 6), hint = "Most 18, second 12, third 6; ties split"
    ),
    CountRow("coins", "Coins", per = coinsPer, hint = coinsHint)
)

internal val ageOfInnovation = ScoreSheet(
    id = "age-of-innovation",
    gameName = "Age of Innovation",
    gameIds = setOf(383179),
    maxPlayers = 5,
    rows = mysticaRows(
        listOf(ScoreGroup("banking", "Banking"), ScoreGroup("law", "Law"), ScoreGroup("engineering", "Engineering"), ScoreGroup("medicine", "Medicine")),
        "Discipline tracks", 5, "After converting resources: 1 point per 5"
    )
)

internal val akropolis = ScoreSheet(
    id = "akropolis",
    gameName = "Akropolis",
    gameIds = setOf(357563, 422822),
    maxPlayers = 4,
    modules = listOf(ScoreModule("athena", "Athena", setOf(422822))),
    rows = listOf(
        PointsRow("houses", "Houses", hint = "District heights × stars"),
        PointsRow("markets", "Markets"),
        PointsRow("barracks", "Barracks"),
        PointsRow("temples", "Temples"),
        PointsRow("gardens", "Gardens"),
        CountRow("stones", "Stones"),
        PointsRow("athena", "Athena", hint = "Constructions completed", module = "athena")
    ),
    tiebreakers = listOf(ScoreTiebreaker("stones", "Stones", fromKey = "stones"))
)

internal val anachrony = ScoreSheet(
    id = "anachrony",
    gameName = "Anachrony",
    gameIds = setOf(185343),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("buildings", "Buildings"),
        CountRow("anomalies", "Anomalies", pointsEach = -3),
        PointsRow("superprojects", "Superprojects"),
        PointsRow("travel", "Time travel"),
        PointsRow("morale", "Morale"),
        PointsRow("tokens", "Victory point tokens"),
        CountRow("warps", "Unremoved warps", pointsEach = -2),
        CountRow("conditions", "Endgame conditions", pointsEach = 3, hint = "Conditions met; ties all score"),
        CountRow("breakthroughs", "Breakthroughs"),
        CountRow("breakthrough-sets", "Breakthrough sets", pointsEach = 2, hint = "Circle, triangle and square")
    ),
    tiebreakers = listOf(ScoreTiebreaker("water", "Water"), ScoreTiebreaker("resources", "Resources"))
)

internal val cartographers = ScoreSheet(
    id = "cartographers",
    gameName = "Cartographers",
    gameIds = setOf(263918),
    maxPlayers = 8,
    rows = listOf(
        PointsRow("edict-a", "Edict A", hint = "Both seasons it was scored"),
        PointsRow("edict-b", "Edict B"),
        PointsRow("edict-c", "Edict C"),
        PointsRow("edict-d", "Edict D"),
        PointsRow("coins", "Coins", hint = "Every season"),
        PointsRow("monsters", "Monsters", hint = "Empty spaces next to monster terrain", subtract = true)
    ),
    tiebreakers = listOf(ScoreTiebreaker("monsters", "Lost to monsters", preferHighest = false, fromKey = "monsters"))
)

internal val cascadia = ScoreSheet(
    id = "cascadia",
    gameName = "Cascadia",
    gameIds = setOf(295947, 385318),
    maxPlayers = 4,
    modules = listOf(ScoreModule("landmarks", "Landmarks", setOf(385318))),
    rows = listOf(
        PointsRow("bear", "Bears", hint = "By the wildlife scoring cards"),
        PointsRow("elk", "Elk"),
        PointsRow("salmon", "Salmon"),
        PointsRow("hawk", "Hawks"),
        PointsRow("fox", "Foxes"),
        MajorityRow(
            id = "habitats", label = "Habitats",
            hint = "Largest corridor, 1 per tile, plus the bonus for the largest",
            groups = listOf(
                ScoreGroup("mountains", "Mountains"), ScoreGroup("forests", "Forests"), ScoreGroup("prairies", "Prairies"),
                ScoreGroup("wetlands", "Wetlands"), ScoreGroup("rivers", "Rivers")
            ),
            awards = listOf(3, 1), awardsByPlayers = mapOf(1 to emptyList(), 2 to listOf(2)), pointsEach = 1
        ),
        CountRow("nature", "Nature tokens"),
        PointsRow("landmarks", "Landmarks", module = "landmarks")
    ),
    tiebreakers = listOf(ScoreTiebreaker("nature", "Nature tokens", fromKey = "nature"))
)

internal val catan = ScoreSheet(
    id = "catan",
    gameName = "Catan",
    gameIds = setOf(13, 191710, 2807, 135378),
    maxPlayers = 6,
    // Explorers & Pirates has no cities, development cards, Longest Road or Largest Army.
    modules = listOf(
        ScoreModule("classic", "Catan", group = "Game"),
        ScoreModule("explorers", "Explorers & Pirates", bggIds = setOf(135378), group = "Game")
    ),
    rows = listOf(
        CountRow("settlements", "Settlements"),
        CountRow("cities", "Cities", pointsEach = 2, module = "classic"),
        PointsRow("road", "Longest road", hint = "2 points", module = "classic"),
        PointsRow("army", "Largest army", hint = "2 points", module = "classic"),
        CountRow("vp-cards", "Victory point cards", module = "classic"),
        CountRow("harbors", "Harbor settlements", pointsEach = 2, module = "explorers"),
        PointsRow("lairs", "Pirate lairs track", hint = "Points next to your marker", module = "explorers"),
        PointsRow("fish", "Fish for Catan track", hint = "Points next to your marker", module = "explorers"),
        PointsRow("spices", "Spices for Catan track", hint = "Points next to your marker", module = "explorers"),
        CountRow("bonus", "Bonus VP tiles", hint = "1 point for leading a mission track", module = "explorers")
    )
)

internal val caverna = ScoreSheet(
    id = "caverna",
    gameName = "Caverna",
    gameIds = setOf(102794),
    maxPlayers = 7,
    rows = listOf(
        CountRow("animals", "Animals", hint = "Farm animals and dogs"),
        CountRow("missing", "Missing farm animal types", pointsEach = -2),
        CountRow("grain", "Grain", per = 2, roundUp = true, hint = "Half a point each, rounded up"),
        CountRow("vegetables", "Vegetables"),
        CountRow("rubies", "Rubies"),
        CountRow("dwarfs", "Dwarfs"),
        CountRow("unused", "Unused spaces", pointsEach = -1),
        PointsRow("furnishings", "Furnishings and dwellings"),
        PointsRow("pastures", "Pastures and mines"),
        CountRow("gold", "Gold"),
        PointsRow("bonus", "Furnishing bonuses"),
        CountRow("begging", "Begging markers", pointsEach = -3)
    )
)

internal val citadels = ScoreSheet(
    id = "citadels",
    gameName = "Citadels",
    gameIds = setOf(478),
    maxPlayers = 8,
    rows = listOf(
        PointsRow("districts", "District costs", hint = "Gold cost of every district in your city"),
        PointsRow("colors", "All five colours", hint = "3 points"),
        PointsRow("first", "First complete city", hint = "4 points"),
        PointsRow("complete", "Complete city", hint = "2 points for the others with 8 districts"),
        PointsRow("bonus", "District bonuses", hint = "Unique districts that add points")
    ),
    tiebreakers = listOf(
        ScoreTiebreaker("districts", "District costs", fromKey = "districts"),
        ScoreTiebreaker("gold", "Gold")
    )
)

private val clankTiebreakers = listOf(ScoreTiebreaker("artifact", "Most valuable artifact"))

internal val clank = ScoreSheet(
    id = "clank",
    gameName = "Clank!",
    gameIds = setOf(201808, 218103, 245377, 257276, 282426),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("artifacts", "Artifacts"),
        CountRow("gold", "Gold"),
        PointsRow("tokens", "Tokens and items", hint = "Chalices, eggs, idols, keys, backpacks, crowns"),
        PointsRow("cards", "Card points"),
        PointsRow("mastery", "Mastery", hint = "20 if you got out of the depths")
    ),
    tiebreakers = clankTiebreakers
)

internal val clankInSpace = ScoreSheet(
    id = "clank-in-space",
    gameName = "Clank! In! Space!",
    gameIds = setOf(233371, 286265, 333536, 254470),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("artifacts", "Artifacts"),
        CountRow("credits", "Credits"),
        PointsRow("tokens", "Tokens and items", hint = "Plans, crystals, keys, passes, med kits, contraband"),
        PointsRow("cards", "Card points"),
        PointsRow("mastery", "Mastery", hint = "20 if you escaped")
    ),
    tiebreakers = clankTiebreakers
)

internal val clansOfCaledonia = ScoreSheet(
    id = "clans-of-caledonia",
    gameName = "Clans of Caledonia",
    gameIds = setOf(216132),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("glory", "Glory track"),
        CountRow("basic", "Basic goods"),
        CountRow("processed", "Processed goods", pointsEach = 2),
        CountRow("money", "Money", per = 10, hint = "1 point per 10"),
        CountRow("hops", "Hops imported"),
        PointsRow("imports", "Cotton, tobacco, sugar", hint = "3, 4 or 5 each by rarity"),
        MajorityRow(
            id = "export", label = "Export contracts", groups = listOf(ScoreGroup("contracts", "Fulfilled contracts")),
            awards = listOf(12, 6), awardsByPlayers = mapOf(2 to listOf(8)), hint = "Most 12, second 6 (2 players: 8)"
        ),
        MajorityRow(
            id = "settlements", label = "Settlements", groups = listOf(ScoreGroup("count", "Within shipping reach")),
            awards = listOf(18, 12, 6), awardsByPlayers = mapOf(2 to listOf(12)), hint = "Most 18, 12, 6 (2 players: 12)"
        )
    ),
    tiebreakers = listOf(ScoreTiebreaker("money", "Money left", fromKey = "money"))
)

internal val concordia = ScoreSheet(
    id = "concordia",
    gameName = "Concordia",
    gameIds = setOf(124361, 256916),
    maxPlayers = 6,
    modules = listOf(ScoreModule("venus", "Venus", setOf(256916))),
    rows = listOf(
        PointsRow("vesta", "Vesta", hint = "Goods and money: 1 per 10 sestertii"),
        PointsRow("jupiter", "Jupiter", hint = "Houses outside brick cities × cards"),
        PointsRow("saturnus", "Saturnus", hint = "Provinces × cards"),
        PointsRow("mercurius", "Mercurius", hint = "2 per goods type produced × cards"),
        PointsRow("mars", "Mars", hint = "2 per colonist × cards"),
        PointsRow("minerva", "Minerva", hint = "Specialist cities"),
        PointsRow("venus", "Venus", module = "venus"),
        PointsRow("concordia", "Concordia card", hint = "7 points")
    ),
    tiebreakers = listOf(ScoreTiebreaker("prefect", "Praefectus Magnus order (1 = holder)", preferHighest = false))
)

internal val darwinsJourney = ScoreSheet(
    id = "darwins-journey",
    gameName = "Darwin's Journey",
    gameIds = setOf(322289),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("track", "Score track", hint = "Points scored during the game"),
        PointsRow("objectives", "Objectives", hint = "Silver and golden, plus the 4 for both last ones"),
        PointsRow("evolution", "Theory of Evolution"),
        PointsRow("seals", "Wax seals", hint = "3 per worker with 5 seals, 10 with 6"),
        PointsRow("tents", "Tents", hint = "5 for all five"),
        CountRow("coins", "Coins", per = 5, hint = "1 point per 5"),
        CountRow("knowledge", "Temporary knowledge")
    ),
    tiebreakers = listOf(
        ScoreTiebreaker("evolution", "Evolution track position"),
        ScoreTiebreaker("coins", "Coins", fromKey = "coins")
    )
)

internal val dominion = ScoreSheet(
    id = "dominion",
    gameName = "Dominion",
    gameIds = setOf(216849, 36218, 51811, 66098, 66690, 90850, 125403, 137166, 171915, 298055, 257673),
    maxPlayers = 6,
    modules = listOf(ScoreModule("prosperity", "Prosperity", setOf(66690))),
    rows = listOf(
        CountRow("estates", "Estates"),
        CountRow("duchies", "Duchies", pointsEach = 3),
        CountRow("provinces", "Provinces", pointsEach = 6),
        CountRow("colonies", "Colonies", pointsEach = 10, module = "prosperity"),
        CountRow("curses", "Curses", pointsEach = -1),
        PointsRow("other", "Other victory cards", hint = "Gardens and kingdom victory cards"),
        PointsRow("tokens", "Victory tokens")
    ),
    tiebreakers = listOf(ScoreTiebreaker("turns", "Turns taken", preferHighest = false))
)

private fun eclipseRows(withAlliances: Boolean): List<ScoreRow> = listOfNotNull(
    PointsRow("reputation", "Reputation tiles"),
    CountRow("ambassadors", "Ambassadors"),
    PointsRow("sectors", "Controlled sectors"),
    CountRow("discoveries", "Discovery tiles", pointsEach = 2, hint = "Kept victory point side up"),
    CountRow("monoliths", "Monoliths", pointsEach = 3),
    PointsRow("tech", "Technology tracks", hint = "4/5/6/7 tiles on a track: 1/2/3/5"),
    PointsRow("traitor", "Traitor", hint = "2 points", subtract = true),
    PointsRow("species", "Species bonuses"),
    if (withAlliances) PointsRow("alliance", "Alliance tile", hint = "2, or -3 for the Betrayer", module = "rota", negative = true) else null
)

internal val eclipse = ScoreSheet(
    id = "eclipse",
    gameName = "Eclipse",
    gameIds = setOf(72125, 125898),
    maxPlayers = 9,
    modules = listOf(ScoreModule("rota", "Rise of the Ancients", setOf(125898))),
    rows = eclipseRows(withAlliances = true),
    tiebreakers = listOf(ScoreTiebreaker("resources", "Resources in storage"))
)

internal val eclipseSecondDawn = ScoreSheet(
    id = "eclipse-second-dawn",
    gameName = "Eclipse: Second Dawn",
    gameIds = setOf(246900),
    maxPlayers = 6,
    rows = eclipseRows(withAlliances = false),
    tiebreakers = listOf(ScoreTiebreaker("resources", "Resources in storage"))
)

internal val endeavorDeepSea = ScoreSheet(
    id = "endeavor-deep-sea",
    gameName = "Endeavor: Deep Sea",
    gameIds = setOf(367966, 446892),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("attributes", "Attribute tracks"),
        PointsRow("impact", "Impact board"),
        PointsRow("missions", "Mission goals"),
        PointsRow("journals", "Journals and specialists", hint = "Journals and tier-5 senior specialists"),
        CountRow("leftovers", "Leftover research and discs", per = 3, hint = "1 point per 3")
    )
)

internal val faraway = ScoreSheet(
    id = "faraway",
    gameName = "Faraway",
    gameIds = setOf(385761, 425029),
    maxPlayers = 6,
    rows = (8 downTo 1).map { n ->
        PointsRow("region-$n", "Region card $n", hint = if (n == 8) "Scored from the last card back" else null)
    } + PointsRow("sanctuaries", "Sanctuaries"),
    tiebreakers = listOf(ScoreTiebreaker("duration", "Exploration durations", preferHighest = false))
)

internal val fiveTribes = ScoreSheet(
    id = "five-tribes",
    gameName = "Five Tribes",
    gameIds = setOf(157354),
    maxPlayers = 4,
    rows = listOf(
        CountRow("coins", "Gold coins"),
        MajorityRow(
            id = "viziers", label = "Viziers", groups = listOf(ScoreGroup("count", "Viziers")),
            awards = emptyList(), pointsEach = 1, beatsEach = 10,
            hint = "1 each, plus 10 per opponent with fewer"
        ),
        CountRow("elders", "Elders", pointsEach = 2),
        PointsRow("djinns", "Djinns"),
        CountRow("palms", "Palm trees", pointsEach = 3),
        CountRow("palaces", "Palaces", pointsEach = 5),
        PointsRow("tiles", "Tiles"),
        PointsRow("merchandise", "Merchandise", hint = "Sets of different goods")
    )
)

internal val forestShuffle = ScoreSheet(
    id = "forest-shuffle",
    gameName = "Forest Shuffle",
    gameIds = setOf(391163, 412576, 424129, 438231),
    maxPlayers = 5,
    rows = listOf(
        PointsRow("trees", "Trees"),
        PointsRow("top", "Top cards"),
        PointsRow("bottom", "Bottom cards"),
        PointsRow("left", "Left cards"),
        PointsRow("right", "Right cards"),
        CountRow("cave", "Cave", hint = "1 per card")
    )
)

internal val gaiaProject = ScoreSheet(
    id = "gaia-project",
    gameName = "Gaia Project",
    gameIds = setOf(220308),
    maxPlayers = 4,
    rows = listOf(
        PointsRow("track", "Score track", hint = "Points scored during the game"),
        MajorityRow(
            id = "final", label = "Final scoring tiles",
            groups = listOf(ScoreGroup("tile-1", "First tile"), ScoreGroup("tile-2", "Second tile")),
            awards = listOf(18, 12, 6), hint = "Highest 18, 12, 6; ties split"
        ),
        PointsRow("research", "Research", hint = "4 per level 3, 4 and 5 reached"),
        CountRow("resources", "Resources", per = 3, hint = "Credits, knowledge and ore: 1 point per 3")
    )
)

internal val greatWesternTrail = ScoreSheet(
    id = "great-western-trail",
    gameName = "Great Western Trail",
    gameIds = setOf(193738, 341169),
    maxPlayers = 4,
    rows = listOf(
        CountRow("dollars", "Dollars", per = 5, hint = "1 point per 5"),
        PointsRow("buildings", "Buildings", hint = "Your building tiles on the board"),
        PointsRow("cities", "Cities", hint = "Kansas City discs cost 6", negative = true),
        PointsRow("stations", "Train stations"),
        PointsRow("hazards", "Hazards"),
        PointsRow("cattle", "Cattle cards"),
        PointsRow("objectives", "Objectives", hint = "Fulfilled minus failed", negative = true),
        PointsRow("stationmasters", "Station master tiles"),
        PointsRow("other", "Other", hint = "Job market and other bonuses")
    )
)
