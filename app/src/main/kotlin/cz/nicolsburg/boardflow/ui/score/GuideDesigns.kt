package cz.nicolsburg.boardflow.ui.score

import androidx.compose.ui.graphics.Color
import cz.nicolsburg.boardflow.ui.score.Kit.Blue
import cz.nicolsburg.boardflow.ui.score.Kit.Brown
import cz.nicolsburg.boardflow.ui.score.Kit.Cream
import cz.nicolsburg.boardflow.ui.score.Kit.Dark
import cz.nicolsburg.boardflow.ui.score.Kit.Gold
import cz.nicolsburg.boardflow.ui.score.Kit.Green
import cz.nicolsburg.boardflow.ui.score.Kit.Grey
import cz.nicolsburg.boardflow.ui.score.Kit.Orange
import cz.nicolsburg.boardflow.ui.score.Kit.Pink
import cz.nicolsburg.boardflow.ui.score.Kit.Purple
import cz.nicolsburg.boardflow.ui.score.Kit.Red
import cz.nicolsburg.boardflow.ui.score.Kit.Teal
import cz.nicolsburg.boardflow.ui.score.Kit.White
import cz.nicolsburg.boardflow.ui.score.Kit.Yellow

// Designs for the Quick Setup games' score sheets: icons from the shared kit, tinted in each
// game's own colours. Keys are row ids, `<row>.<group>` and `tie.<id>`.

private val Terracotta = Color(0xFFC8704A)
private val Sand = Color(0xFFD9C08A)
private val Navy = Color(0xFF3A5A9A)
private val Olive = Color(0xFF8A9A4A)
private val Rust = Color(0xFFB0563A)
private val Sea = Color(0xFF3F8FC0)

internal val guideDesigns: Map<String, ScoreSheetDesign> = mapOf(
    "7-wonders" to design(
        Kit.temple(Sand),
        "military" to Kit.shield(Red), "treasury" to Kit.coin(), "wonder" to Kit.temple(Sand),
        "civilian" to Kit.card(Blue), "science" to Kit.card(Green), "science.compass" to Kit.target(Green),
        "science.tablet" to Kit.scroll(Sand), "science.gear" to Kit.gear(Green), "commercial" to Kit.card(Yellow),
        "guilds" to Kit.card(Purple), "leaders" to Kit.card(White, Gold), "cities" to Kit.card(Dark, Grey),
        "debt" to Kit.minus(), "armada" to Kit.ship(), "babel" to Kit.tower(Sand), "edifice" to Kit.building(Sand)
    ),
    "7-wonders-duel" to design(
        Kit.temple(Gold),
        "military" to Kit.swords(), "treasury" to Kit.coin(), "wonders" to Kit.temple(Sand),
        "civilian" to Kit.card(Blue), "scientific" to Kit.card(Green), "commercial" to Kit.card(Yellow),
        "guilds" to Kit.card(Purple), "progress" to Kit.coin(Green), "pantheon" to Kit.sun(Gold),
        "agora" to Kit.people(Red), "tie.civilian" to Kit.card(Blue)
    ),
    "a-feast-for-odin" to design(
        Kit.ship(Brown, Red),
        "ships" to Kit.ship(Brown, Red), "emigration" to Kit.ship(Brown, Cream), "exploration" to Kit.mountain(Teal),
        "buildings" to Kit.house(Brown, Rust), "animals" to Kit.sheep(), "occupations" to Kit.card(Cream, Red),
        "silver" to Kit.coin(Grey), "income" to Kit.coins(Grey), "crown" to Kit.crown(),
        "negative" to Kit.minus(), "thing" to Kit.flag(Red)
    ),
    "agricola" to agricolaDesign(),
    "agricola-revised" to agricolaDesign(),
    "age-of-innovation" to mysticaDesign(Teal),
    "terra-mystica" to mysticaDesign(Rust),
    "akropolis" to design(
        Kit.temple(White),
        "houses" to Kit.house(Blue, Blue), "markets" to Kit.house(Yellow, Yellow), "barracks" to Kit.castle(Red),
        "temples" to Kit.temple(Purple), "gardens" to Kit.tree(Green), "stones" to Kit.cube(Grey),
        "athena" to Kit.star(Gold), "tie.stones" to Kit.cube(Grey)
    ),
    "anachrony" to design(
        Kit.hourglass(Teal),
        "buildings" to Kit.factory(Grey), "anomalies" to Kit.minus(Purple), "superprojects" to Kit.building(Teal),
        "travel" to Kit.hourglass(Teal), "morale" to Kit.heart(Red), "tokens" to Kit.coin(Gold),
        "warps" to Kit.bolt(Purple), "conditions" to Kit.target(Gold), "breakthroughs" to Kit.gem(Teal),
        "breakthrough-sets" to Kit.hexes(Teal), "tie.water" to Kit.water(), "tie.resources" to Kit.resources()
    ),
    "cartographers" to design(
        Kit.scroll(Sand),
        "edict-a" to Kit.scroll(Sand), "edict-b" to Kit.scroll(Sand), "edict-c" to Kit.scroll(Sand),
        "edict-d" to Kit.scroll(Sand), "coins" to Kit.coin(), "monsters" to Kit.skull(Red),
        "tie.monsters" to Kit.skull(Red)
    ),
    "cascadia" to design(
        Kit.pine(Green),
        "bear" to Kit.paw(Brown), "elk" to Kit.animal(Olive), "salmon" to Kit.fish(Pink), "hawk" to Kit.bird(Blue),
        "fox" to Kit.animal(Orange), "habitats" to Kit.tile(Green), "habitats.mountains" to Kit.mountain(),
        "habitats.forests" to Kit.pine(Green), "habitats.prairies" to Kit.grain(Yellow),
        "habitats.wetlands" to Kit.tile(Teal), "habitats.rivers" to Kit.water(), "nature" to Kit.leafG(Green),
        "landmarks" to Kit.tower(Grey), "tie.nature" to Kit.leafG(Green)
    ),
    "catan" to design(
        Kit.hexes(Orange),
        "settlements" to Kit.house(Red, Red), "cities" to Kit.building(Red), "road" to Kit.route(Red),
        "army" to Kit.swords(), "vp-cards" to Kit.card(Yellow, Gold)
    ),
    "caverna" to design(
        Kit.mountain(Grey),
        "animals" to Kit.sheep(), "missing" to Kit.minus(), "grain" to Kit.grain(), "vegetables" to Kit.vegetable(),
        "rubies" to Kit.gem(Red), "dwarfs" to Kit.meeple(Orange), "unused" to Kit.minus(Grey),
        "furnishings" to Kit.house(Brown, Brown), "pastures" to Kit.fence(), "gold" to Kit.coin(),
        "bonus" to Kit.star(), "begging" to Kit.minus(Purple)
    ),
    "citadels" to design(
        Kit.crown(),
        "districts" to Kit.castle(Grey), "colors" to Kit.hexes(Purple), "first" to Kit.crown(),
        "complete" to Kit.building(Grey), "bonus" to Kit.card(Purple), "tie.districts" to Kit.castle(Grey),
        "tie.gold" to Kit.coin()
    ),
    "clank" to clankDesign(Kit.Red),
    "clank-in-space" to clankDesign(Kit.Blue),
    "clans-of-caledonia" to design(
        Kit.flag(Teal),
        "glory" to Kit.laurel(), "basic" to Kit.cube(Brown), "processed" to Kit.barrel(), "money" to Kit.coins(),
        "hops" to Kit.leafG(Green), "imports" to Kit.ship(), "export" to Kit.scroll(Sand),
        "export.contracts" to Kit.scroll(Sand), "settlements" to Kit.house(Teal, Teal),
        "settlements.count" to Kit.house(Teal, Teal), "tie.money" to Kit.coins()
    ),
    "concordia" to design(
        Kit.temple(Cream),
        "vesta" to Kit.coins(), "jupiter" to Kit.house(Cream, Red), "saturnus" to Kit.hexes(Olive),
        "mercurius" to Kit.barrel(), "mars" to Kit.meeple(Red), "minerva" to Kit.building(Grey),
        "venus" to Kit.heart(Pink), "concordia" to Kit.card(Purple, Gold), "tie.prefect" to Kit.crown()
    ),
    "darwins-journey" to design(
        Kit.ship(Brown, Cream),
        "track" to Kit.track(), "objectives" to Kit.target(), "evolution" to Kit.book(Green),
        "seals" to Kit.medal(Red, Red), "tents" to Kit.house(Sand, Brown), "coins" to Kit.coin(),
        "knowledge" to Kit.lightbulb(), "tie.evolution" to Kit.book(Green), "tie.coins" to Kit.coin()
    ),
    "dominion" to design(
        Kit.crown(),
        "estates" to Kit.card(Green), "duchies" to Kit.card(Green, Gold), "provinces" to Kit.castle(Green),
        "colonies" to Kit.building(Green), "curses" to Kit.card(Purple), "other" to Kit.card(Green, Purple),
        "tokens" to Kit.coin(Green), "tie.turns" to Kit.hourglass()
    ),
    "eclipse" to eclipseDesign(),
    "eclipse-second-dawn" to eclipseDesign(),
    "endeavor-deep-sea" to design(
        Kit.fish(Sea),
        "attributes" to Kit.track(Sea), "impact" to Kit.hexes(Sea), "missions" to Kit.target(Sea),
        "journals" to Kit.book(Sea), "leftovers" to Kit.cube(Teal)
    ),
    "faraway" to design(
        Kit.sun(Purple),
        *(8 downTo 1).map { "region-$it" to Kit.card(Purple, Gold) }.toTypedArray(),
        "sanctuaries" to Kit.temple(Grey), "tie.duration" to Kit.hourglass(Purple)
    ),
    "five-tribes" to design(
        Kit.meeple(Gold),
        "coins" to Kit.coins(), "viziers" to Kit.meeple(Yellow), "viziers.count" to Kit.meeple(Yellow),
        "elders" to Kit.meeple(White), "djinns" to Kit.potion(Blue), "palms" to Kit.tree(Green),
        "palaces" to Kit.castle(Cream), "tiles" to Kit.square(Sand), "merchandise" to Kit.cards(Sand)
    ),
    "forest-shuffle" to design(
        Kit.tree(Green),
        "trees" to Kit.tree(Green), "top" to Kit.bird(Teal), "bottom" to Kit.flower(Pink),
        "left" to Kit.animal(Brown), "right" to Kit.animal(Orange), "cave" to Kit.mountain(Grey)
    ),
    "gaia-project" to design(
        Kit.planet(Teal),
        "track" to Kit.track(Teal), "final" to Kit.target(Teal), "final.tile-1" to Kit.target(Teal),
        "final.tile-2" to Kit.target(Teal), "research" to Kit.flask(Teal), "resources" to Kit.cube(Grey)
    ),
    "great-western-trail" to design(
        Kit.animal(Brown),
        "dollars" to Kit.coins(Green), "buildings" to Kit.house(Brown, Brown), "cities" to Kit.building(Red),
        "stations" to Kit.train(), "hazards" to Kit.mountain(Rust), "cattle" to Kit.animal(Brown),
        "objectives" to Kit.target(), "stationmasters" to Kit.meeple(Blue), "other" to Kit.star()
    ),
    "harmonies" to design(
        Kit.leafG(Green),
        "trees" to Kit.tree(Green), "mountains" to Kit.mountain(), "fields" to Kit.grain(),
        "buildings" to Kit.house(Red, Red), "water" to Kit.water(), "animals" to Kit.paw(Orange),
        "spirit" to Kit.sun(Purple), "tie.cubes" to Kit.cube(Orange)
    ),
    "its-a-wonderful-world" to design(
        Kit.planet(Blue),
        "gross" to Kit.card(Blue, Gold), "combo" to Kit.cards(Blue), "generals" to Kit.meeple(Red),
        "financiers" to Kit.meeple(Blue), "tie.cards" to Kit.cards(Blue), "tie.characters" to Kit.people(Gold)
    ),
    "kingdomino" to design(
        Kit.crown(),
        "wheat" to Kit.grain(), "forest" to Kit.pine(Green), "lake" to Kit.water(), "grassland" to Kit.square(Olive),
        "swamp" to Kit.square(Brown), "mine" to Kit.mountain(), "middle" to Kit.castle(Grey),
        "harmony" to Kit.square(Gold), "tie.territory" to Kit.square(Green)
    ),
    "kutna-hora" to design(
        Kit.temple(Grey),
        "track" to Kit.track(), "stars" to Kit.star(), "income" to Kit.coins(Grey), "reputation" to Kit.heart(Red),
        "extras" to Kit.plus(), "tie.groschen" to Kit.coin(Grey)
    ),
    "le-havre" to design(
        Kit.ship(Grey, White),
        "francs" to Kit.coins(), "buildings" to Kit.building(Brown), "ships" to Kit.ship(Grey, White),
        "loans" to Kit.minus()
    ),
    "lost-cities" to design(
        Kit.mountain(Orange),
        "expeditions" to Kit.flag(Gold),
        "expeditions.yellow" to Kit.flag(Yellow), "expeditions.blue" to Kit.flag(Blue),
        "expeditions.white" to Kit.flag(White), "expeditions.green" to Kit.flag(Green),
        "expeditions.red" to Kit.flag(Red), "purple" to Kit.flag(Purple), "purple.purple" to Kit.flag(Purple)
    ),
    "lost-ruins-of-arnak" to design(
        Kit.temple(Gold),
        "research" to Kit.book(Gold), "temple" to Kit.temple(Gold), "idols" to Kit.gem(Gold),
        "guardians" to Kit.skull(Green), "cards" to Kit.card(Brown, Gold), "fear" to Kit.card(Dark, Red),
        "fear-tiles" to Kit.minus(), "tie.temple-first" to Kit.temple(Gold)
    ),
    "lords-of-waterdeep" to design(
        Kit.castle(Grey),
        "track" to Kit.track(), "lord" to Kit.crown(), "adventurers" to Kit.cube(Purple), "gold" to Kit.coins()
    ),
    "on-mars" to design(
        Kit.planet(Rust),
        "track" to Kit.track(Rust), "progress" to Kit.cube(Rust), "ships" to Kit.rocket(), "colonists" to Kit.people(White),
        "tech" to Kit.gear(Teal), "advanced" to Kit.building(Rust), "blueprints" to Kit.minus(),
        "scientists" to Kit.flask(Teal), "contracts" to Kit.scroll(Sand), "tie.crystals" to Kit.gem(Teal),
        "tie.buildings" to Kit.building(Rust), "tie.cubes" to Kit.cube(Rust)
    ),
    "orleans" to design(
        Kit.castle(Cream),
        "coins" to Kit.coins(), "goods" to Kit.barrel(), "development" to Kit.house(Blue, Blue),
        "tie.development" to Kit.track(Blue)
    ),
    "paladins" to design(
        Kit.shield(Navy),
        "orders" to Kit.scroll(Sand), "attributes" to Kit.track(Navy), "commissions" to Kit.meeple(Purple),
        "fortifies" to Kit.castle(Grey), "develops" to Kit.house(Brown, Brown), "garrisons" to Kit.swords(),
        "absolves" to Kit.temple(Cream), "converts" to Kit.people(Gold), "debts" to Kit.minus(),
        "leftovers" to Kit.coins(Grey), "tie.orders" to Kit.scroll(Sand), "tie.suspicion" to Kit.eye(Red)
    ),
    "parks" to design(
        Kit.mountain(Green),
        "parks" to Kit.mountain(Green), "photos" to Kit.square(Cream), "year" to Kit.card(Orange, Gold),
        "tie.visited" to Kit.pine(Green)
    ),
    "patchwork" to design(
        Kit.square(Pink),
        "buttons" to Kit.coin(Blue), "bonus" to Kit.square(Gold), "empty" to Kit.minus(), "tie.first" to Kit.hourglass()
    ),
    "puerto-rico" to design(
        Kit.ship(Brown, Cream),
        "chips" to Kit.coin(Gold), "buildings" to Kit.building(Purple), "large" to Kit.castle(Purple),
        "tie.money" to Kit.coins()
    ),
    "race-for-the-galaxy" to design(
        Kit.planet(Purple),
        "cards" to Kit.cards(Purple), "chips" to Kit.hexes(Gold), "six" to Kit.card(Purple, Gold),
        "tie.hand" to Kit.hand()
    ),
    "revive" to design(
        Kit.gear(Teal),
        "track" to Kit.track(Teal), "artifacts" to Kit.gem(Purple), "artifact-card" to Kit.card(Purple),
        "technologies" to Kit.gear(Teal), "progress" to Kit.track(Purple), "locations" to Kit.house(Teal, Teal),
        "tie.resources" to Kit.resources(), "tie.machines" to Kit.gear(Grey)
    ),
    "sagrada" to design(
        Kit.dice(Blue),
        "public" to Kit.card(Blue), "private" to Kit.card(Purple), "favor" to Kit.gem(Teal), "open" to Kit.minus(),
        "tie.private" to Kit.card(Purple), "tie.favor" to Kit.gem(Teal)
    ),
    "scythe" to design(
        Kit.gear(Red),
        "coins" to Kit.coins(), "stars" to Kit.star(), "territories" to Kit.hexes(Olive), "resources" to Kit.resources(),
        "structures" to Kit.factory(Grey), "tie.units" to Kit.meeple(Red), "tie.power" to Kit.bolt(Red),
        "tie.popularity" to Kit.heart(Red), "tie.owned" to Kit.resources(), "tie.controlled" to Kit.hexes(Olive),
        "tie.placed" to Kit.star()
    ),
    "seti" to design(
        Kit.eye(Purple),
        "track" to Kit.track(Purple), "gold" to Kit.medal(Gold, Purple)
    ),
    "stone-age" to design(
        Kit.house(Brown, Brown),
        "track" to Kit.track(), "culture" to Kit.card(Green), "farmers" to Kit.grain(), "toolmakers" to Kit.gear(Grey),
        "builders" to Kit.house(Brown, Brown), "shamans" to Kit.people(Orange), "resources" to Kit.resources(),
        "tie.sum" to Kit.plus()
    ),
    "sushi-go" to design(
        Kit.fish(Pink),
        "round-1" to Kit.fish(Pink), "round-2" to Kit.fish(Orange), "round-3" to Kit.fish(Red),
        "pudding" to Kit.egg(Yellow), "pudding.count" to Kit.egg(Yellow), "tie.pudding" to Kit.egg(Yellow)
    ),
    "sushi-go-party" to design(
        Kit.fish(Orange),
        "round-1" to Kit.fish(Pink), "round-2" to Kit.fish(Orange), "round-3" to Kit.fish(Red),
        "dessert" to Kit.egg(Yellow), "tie.dessert" to Kit.egg(Yellow)
    ),
    "takenoko" to design(
        Kit.leafG(Green),
        "plots" to Kit.tile(Green), "gardener" to Kit.meeple(Yellow), "panda" to Kit.paw(White),
        "emperor" to Kit.crown(), "tie.panda" to Kit.paw(White)
    ),
    "terraforming-mars" to design(
        Kit.planet(Rust),
        "tr" to Kit.track(Rust), "awards" to Kit.trophy(), "milestones" to Kit.medal(Gold, Rust),
        "greenery" to Kit.tile(Green), "cities" to Kit.building(Grey), "cards" to Kit.card(Rust, Gold),
        "tie.megacredits" to Kit.coin()
    ),
    "ticket-to-ride" to ticketDesign(),
    "ticket-to-ride-europe" to ticketDesign(),
    "welcome-to" to design(
        Kit.house(Cream, Blue),
        "plans" to Kit.scroll(Sand), "parks" to Kit.tree(Green), "pools" to Kit.water(), "temps" to Kit.meeple(Orange),
        "temps.boxes" to Kit.meeple(Orange), "estates" to Kit.house(Cream, Blue), "bis" to Kit.minus(Orange),
        "refusals" to Kit.minus(), "tie.estates" to Kit.house(Cream, Blue)
    )
)

private fun agricolaDesign() = design(
    Kit.house(Sand, Brown),
    "fields" to Kit.field(), "pastures" to Kit.fence(), "grain" to Kit.grain(), "vegetables" to Kit.vegetable(),
    "sheep" to Kit.sheep(), "boar" to Kit.animal(Dark), "cattle" to Kit.animal(Brown), "unused" to Kit.minus(Grey),
    "stables" to Kit.house(Brown, Brown), "clay" to Kit.house(Terracotta, Terracotta), "stone" to Kit.house(Grey, Grey),
    "family" to Kit.people(Orange), "cards" to Kit.cards(Yellow), "bonus" to Kit.star(), "begging" to Kit.minus(Purple),
    "tie.building" to Kit.resources()
)

private fun mysticaDesign(c: Color) = design(
    Kit.tower(c),
    "track" to Kit.track(c), "tracks" to Kit.flask(c), "tracks.banking" to Kit.coins(), "tracks.law" to Kit.book(Navy),
    "tracks.engineering" to Kit.gear(), "tracks.medicine" to Kit.flask(Green), "tracks.fire" to Kit.sun(Red),
    "tracks.water" to Kit.water(), "tracks.earth" to Kit.mountain(Brown), "tracks.air" to Kit.bird(White),
    "area" to Kit.hexes(c), "area.buildings" to Kit.house(c, c), "coins" to Kit.coins()
)

private fun clankDesign(c: Color) = design(
    Kit.gem(c),
    "artifacts" to Kit.gem(c), "gold" to Kit.coins(), "credits" to Kit.coins(Teal), "tokens" to Kit.crown(),
    "cards" to Kit.cards(c), "mastery" to Kit.medal(Gold, c), "tie.artifact" to Kit.gem(c)
)

private fun eclipseDesign() = design(
    Kit.planet(Navy),
    "reputation" to Kit.hexes(Gold), "ambassadors" to Kit.people(Blue), "sectors" to Kit.tile(Navy),
    "discoveries" to Kit.gem(Teal), "monoliths" to Kit.tower(Grey), "tech" to Kit.flask(Teal),
    "traitor" to Kit.minus(), "species" to Kit.star(), "alliance" to Kit.shield(Navy),
    "tie.resources" to Kit.resources()
)

private fun ticketDesign() = design(
    Kit.train(),
    "routes" to Kit.route(), "completed" to Kit.ticket(), "failed" to Kit.minus(), "stations" to Kit.house(Grey, Red),
    "longest" to Kit.train(), "longest.length" to Kit.route(), "tie.tickets" to Kit.ticket(),
    "tie.stations-used" to Kit.house(Grey, Red)
)
