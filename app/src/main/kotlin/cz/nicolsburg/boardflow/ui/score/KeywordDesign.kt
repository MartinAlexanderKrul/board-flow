package cz.nicolsburg.boardflow.ui.score

import androidx.compose.ui.graphics.Color
import cz.nicolsburg.boardflow.model.ExpeditionRow
import cz.nicolsburg.boardflow.model.MajorityRow
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.SetsRow

/**
 * A design for a sheet with no hand-made one (the user's AI drafts and imports): each row gets a
 * kit icon picked from words in its label, tinted from a small palette so neighbours differ.
 * A row whose label matches nothing gets a plain numbered marker.
 */
internal fun keywordDesign(sheet: ScoreSheet): ScoreSheetDesign {
    val glyphs = mutableMapOf<String, Glyph>()
    var n = 0
    fun pick(text: String): Glyph = glyphFor(text, PALETTE[n++ % PALETTE.size])
    sheet.rows.forEach { row ->
        glyphs[row.id] = pick("${row.label} ${row.id}")
        when (row) {
            is MajorityRow -> row.groups.forEach { glyphs[row.groupKey(it)] = pick("${it.label} ${row.label}") }
            is SetsRow -> row.groups.forEach { glyphs[row.groupKey(it)] = pick(it.label) }
            is ExpeditionRow -> row.groups.forEach { glyphs["${row.id}.${it.id}"] = Kit.flag(colorFor(it.label) ?: PALETTE[n++ % PALETTE.size]) }
            else -> Unit
        }
    }
    sheet.tiebreakers.filter { it.asks }.forEach { glyphs[it.inputKey] = pick(it.label) }
    return ScoreSheetDesign(emblem = glyphFor(sheet.gameName, Kit.Gold, fallback = Kit.dice()), glyphs = glyphs)
}

private val PALETTE = listOf(Kit.Gold, Kit.Teal, Kit.Red, Kit.Blue, Kit.Green, Kit.Purple, Kit.Orange, Kit.Pink)

/** First matching keyword wins; the order puts specific words before general ones. */
private val KEYWORDS: List<Pair<List<String>, (Color) -> Glyph>> = listOf(
    listOf("penalt", "debt", "negative", "minus", "lost", "loss", "unused", "empty", "missing", "failed", "fear", "begging") to { _ -> Kit.minus() },
    listOf("coin", "gold", "money", "credit", "dollar", "silver", "franc", "cash", "treasur", "income", "groschen", "sestert") to { _ -> Kit.coins() },
    listOf("ticket", "destination") to { c -> Kit.ticket() },
    listOf("route", "road", "path", "rail") to { c -> Kit.route(c) },
    listOf("train", "station", "locomotive") to { c -> Kit.train(c) },
    listOf("ship", "boat", "fleet", "naval", "harbo", "port") to { c -> Kit.ship() },
    listOf("bird") to { c -> Kit.bird(c) },
    listOf("fish", "sushi") to { c -> Kit.fish(c) },
    listOf("egg") to { _ -> Kit.egg() },
    listOf("sheep") to { _ -> Kit.sheep() },
    listOf("animal", "cattle", "cow", "boar", "pig", "horse", "creature", "wildlife", "elk", "fox") to { c -> Kit.animal(c) },
    listOf("bear", "paw", "panda") to { c -> Kit.paw(c) },
    listOf("flower", "nectar", "blossom") to { c -> Kit.flower(c) },
    listOf("tree", "forest", "wood") to { c -> Kit.tree(Kit.Green) },
    listOf("grain", "wheat", "field", "harvest", "crop", "food") to { _ -> Kit.grain() },
    listOf("vegetable", "carrot") to { _ -> Kit.vegetable() },
    listOf("leaf", "plant", "garden", "nature", "park") to { _ -> Kit.leafG() },
    listOf("castle", "fort", "keep", "palace") to { c -> Kit.castle() },
    listOf("temple", "church", "cathedral", "shrine", "monument", "wonder") to { c -> Kit.temple() },
    listOf("tower") to { c -> Kit.tower() },
    listOf("factory", "industry", "mine") to { c -> Kit.factory() },
    listOf("house", "home", "dwelling", "hut", "room") to { c -> Kit.house(Kit.Cream, c) },
    listOf("building", "city", "cities", "district", "construction", "structure") to { c -> Kit.building(c) },
    listOf("gem", "crystal", "jewel", "ruby", "emerald", "diamond", "artifact", "artefact", "idol", "relic") to { c -> Kit.gem(c) },
    listOf("science", "research", "tech", "lab", "knowledge") to { c -> Kit.flask(c) },
    listOf("book", "journal", "codex", "library") to { c -> Kit.book(c) },
    listOf("objective", "goal", "mission", "quest", "contract", "order") to { c -> Kit.target(c) },
    listOf("achievement", "milestone", "medal", "award") to { c -> Kit.medal(Kit.Gold, c) },
    listOf("trophy", "victory") to { _ -> Kit.trophy() },
    listOf("crown", "king", "queen", "lord", "royal", "noble") to { _ -> Kit.crown() },
    listOf("military", "war", "battle", "combat", "army", "attack", "conflict", "sword") to { _ -> Kit.swords() },
    listOf("shield", "defen", "guard", "wall") to { c -> Kit.shield(c) },
    listOf("mountain", "expedition", "explor", "journey", "travel", "peak") to { c -> Kit.mountain() },
    listOf("water", "river", "lake", "ocean", "sea", "pool", "wetland") to { _ -> Kit.water() },
    listOf("planet", "galaxy", "space", "star system", "world", "mars") to { c -> Kit.planet(c) },
    listOf("rocket", "spaceship") to { _ -> Kit.rocket() },
    listOf("energy", "power", "electric") to { _ -> Kit.bolt() },
    listOf("heart", "morale", "happiness", "popularity", "love") to { _ -> Kit.heart() },
    listOf("time", "turn", "round", "hour") to { c -> Kit.hourglass(c) },
    listOf("dice", "die ") to { _ -> Kit.dice() },
    listOf("worker", "people", "citizen", "colonist", "follower", "population", "family", "member") to { c -> Kit.people(c) },
    listOf("meeple", "character", "hero", "leader", "adventurer", "explorer") to { c -> Kit.meeple(c) },
    listOf("resource", "goods", "cube", "material") to { _ -> Kit.resources() },
    listOf("track", "progress", "level", "score track") to { c -> Kit.track(c) },
    listOf("tile", "territory", "area", "region", "land", "hex", "province", "habitat", "sector") to { c -> Kit.tile(c) },
    listOf("set", "collection", "combo", "hand") to { c -> Kit.cards(c) },
    listOf("card") to { c -> Kit.card(c) },
    listOf("sun", "light") to { _ -> Kit.sun() },
    listOf("bonus", "star", "fame", "prestige", "glory", "reputation", "honor", "honour", "influence", "point", "vp") to { c -> Kit.star(c) }
)

private fun glyphFor(text: String, color: Color, fallback: Glyph? = null): Glyph {
    val t = " ${text.lowercase()} "
    KEYWORDS.firstOrNull { (words, _) -> words.any { it in t } }?.let { return it.second(colorFor(text) ?: color) }
    return fallback ?: Kit.square(color)
}

/** A colour named in the label wins ("Red expedition", "Blue cards"). */
private fun colorFor(text: String): Color? {
    val t = text.lowercase()
    return when {
        "red" in t -> Kit.Red
        "blue" in t -> Kit.Blue
        "green" in t -> Kit.Green
        "yellow" in t -> Kit.Yellow
        "purple" in t || "violet" in t -> Kit.Purple
        "white" in t -> Kit.White
        "orange" in t -> Kit.Orange
        "pink" in t -> Kit.Pink
        "brown" in t -> Kit.Brown
        "grey" in t || "gray" in t -> Kit.Grey
        else -> null
    }
}
