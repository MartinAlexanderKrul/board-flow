package cz.nicolsburg.boardflow.ui.score

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Icons for Everdell's expansion rows and its standalone games (Farshore, Duo, Silverfrost,
// Emerland), drawn after their rulebooks and pads, not copied from them. The base game's icons and
// colours are in EverdellDesign.kt and shared here.

private val Sea = Color(0xFF4A8FC0)
private val SeaDeep = Color(0xFF2F6A94)
private val Sail = Color(0xFFEFE3C6)
private val Ice = Color(0xFFBFE3F2)
private val IceDeep = Color(0xFF7FB8D6)
private val Flame = Color(0xFFE8763A)
private val Emerald = Color(0xFF3DBE7C)
private val EmeraldDeep = Color(0xFF248A55)
private val Jungle = Color(0xFF3F8A4A)

// --- Everdell expansions -------------------------------------------------------------

/** Spirecrest: a trail of map tiles over a peak. */
internal val everdellExpedition: Glyph = { s ->
    drawPath(path(s, 0.10f to 0.86f, 0.46f to 0.20f, 0.82f to 0.86f), EverdellStone)
    drawPath(path(s, 0.46f to 0.20f, 0.56f to 0.38f, 0.36f to 0.38f), Color.White.copy(alpha = 0.9f))
    for (i in 0..2) drawRect(EverdellParchment, at(s, 0.08f + i * 0.30f, 0.80f), Size(0.24f * s, 0.14f * s))
}

/** Spirecrest: a discovery card with a compass star. */
internal val everdellDiscovery: Glyph = { s ->
    card(s, 0.20f, 0.08f, 0.60f, 0.84f, EverdellParchment, EverdellEdge)
    drawPath(star(s, 0.50f, 0.50f, 0.24f, 0.08f), EverdellTeal)
}

/** Bellfaire: a garland of leaves with a bloom. */
internal val everdellGarland: Glyph = { s ->
    drawArc(EverdellMoss, 20f, 140f, false, at(s, 0.08f, 0.10f), Size(0.84f * s, 0.70f * s), style = Stroke(0.08f * s, cap = StrokeCap.Round))
    for (i in 0..4) {
        val a = (20 + i * 35) * PI / 180
        val x = 0.50f + 0.42f * cos(a).toFloat()
        val y = 0.45f + 0.35f * sin(a).toFloat()
        drawPath(leaf(s, x, y, x + 0.06f, y - 0.12f, 0.4f), EverdellForest)
    }
    disc(s, 0.50f, 0.80f, 0.11f, EverdellBerry)
    disc(s, 0.50f, 0.80f, 0.05f, EverdellGold)
}

/** Newleaf: a visitor's ticket. */
internal val everdellVisitor: Glyph = { s ->
    card(s, 0.10f, 0.26f, 0.80f, 0.48f, EverdellParchment, EverdellEdge)
    drawLine(EverdellBark, at(s, 0.34f, 0.30f), at(s, 0.34f, 0.70f), 0.03f * s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.05f * s, 0.04f * s)))
    disc(s, 0.62f, 0.50f, 0.12f, EverdellBerry)
}

// --- Farshore -------------------------------------------------------------------------

private val treasure: Glyph = { s ->
    drawRect(EverdellBark, at(s, 0.14f, 0.40f), Size(0.72f * s, 0.46f * s))
    drawArc(EverdellBark, 180f, 180f, true, at(s, 0.14f, 0.20f), Size(0.72f * s, 0.40f * s))
    drawRect(EverdellGold, at(s, 0.14f, 0.40f), Size(0.72f * s, 0.07f * s))
    drawRect(EverdellGold, at(s, 0.44f, 0.36f), Size(0.12f * s, 0.18f * s))
}

private val seaMap: Glyph = { s ->
    drawPath(path(s, 0.10f to 0.20f, 0.36f to 0.12f, 0.64f to 0.22f, 0.90f to 0.14f, 0.90f to 0.82f, 0.64f to 0.90f, 0.36f to 0.80f, 0.10f to 0.88f), EverdellParchment)
    drawLine(EverdellBerry, at(s, 0.24f, 0.70f), at(s, 0.70f, 0.36f), 0.05f * s, StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.06f * s, 0.05f * s)))
    drawLine(EverdellBerry, at(s, 0.66f, 0.26f), at(s, 0.78f, 0.40f), 0.05f * s, StrokeCap.Round)
    drawLine(EverdellBerry, at(s, 0.78f, 0.26f), at(s, 0.66f, 0.40f), 0.05f * s, StrokeCap.Round)
}

private val ship: Glyph = { s ->
    drawPath(path(s, 0.10f to 0.62f, 0.90f to 0.62f, 0.76f to 0.84f, 0.24f to 0.84f), EverdellBark)
    drawLine(EverdellEdge, at(s, 0.50f, 0.10f), at(s, 0.50f, 0.62f), 0.04f * s)
    drawPath(path(s, 0.52f to 0.14f, 0.84f to 0.54f, 0.52f to 0.54f), Sail)
    drawPath(path(s, 0.48f to 0.22f, 0.48f to 0.54f, 0.22f to 0.54f), Sail)
    drawArc(Sea, 0f, 180f, false, at(s, 0.04f, 0.82f), Size(0.30f * s, 0.12f * s), style = Stroke(0.04f * s))
    drawArc(Sea, 0f, 180f, false, at(s, 0.66f, 0.82f), Size(0.30f * s, 0.12f * s), style = Stroke(0.04f * s))
}

private val farshoreEmblem: Glyph = { s ->
    drawArc(SeaDeep, 180f, 180f, false, at(s, 0.06f, 0.56f), Size(0.44f * s, 0.30f * s), style = Stroke(0.08f * s, cap = StrokeCap.Round))
    drawArc(Sea, 180f, 180f, false, at(s, 0.50f, 0.56f), Size(0.44f * s, 0.30f * s), style = Stroke(0.08f * s, cap = StrokeCap.Round))
    disc(s, 0.70f, 0.30f, 0.14f, EverdellGold)
}

val FarshoreDesign = ScoreSheetDesign(
    emblem = farshoreEmblem,
    glyphs = mapOf(
        "cards" to everdellCards,
        "prosperity" to everdellProsperity,
        "tokens" to everdellPointToken,
        "treasures" to treasure,
        "maps" to seaMap,
        "ship" to ship,
        "tie.maps" to seaMap,
        "tie.resources" to everdellResources
    )
)

// --- Duo ----------------------------------------------------------------------------------

private val duoEmblem: Glyph = { s ->
    disc(s, 0.36f, 0.50f, 0.26f, EverdellBerry)
    disc(s, 0.64f, 0.50f, 0.26f, EverdellForest)
    disc(s, 0.50f, 0.50f, 0.10f, EverdellParchment)
}

private val cardsInHand: Glyph = { s ->
    card(s, 0.12f, 0.24f, 0.44f, 0.62f, EverdellStone, EverdellEdge)
    card(s, 0.30f, 0.16f, 0.44f, 0.62f, EverdellParchment, EverdellEdge)
    card(s, 0.48f, 0.24f, 0.44f, 0.62f, EverdellParchment, EverdellEdge)
}

val EverdellDuoDesign = ScoreSheetDesign(
    emblem = duoEmblem,
    glyphs = mapOf(
        "cards" to everdellCards,
        "prosperity" to everdellProsperity,
        "tokens" to everdellPointToken,
        "events" to everdellEvent,
        "tie.events" to everdellEvent,
        "tie.resources" to everdellResources,
        "tie.city" to everdellCards,
        "tie.hand" to cardsInHand
    )
)

// --- Silverfrost ------------------------------------------------------------------------

private val snowflake: Glyph = { s ->
    for (i in 0 until 6) {
        val a = i * PI / 3
        val x = 0.50f + 0.40f * cos(a).toFloat()
        val y = 0.50f + 0.40f * sin(a).toFloat()
        drawLine(Ice, at(s, 0.50f, 0.50f), at(s, x, y), 0.07f * s, StrokeCap.Round)
        val bx = 0.50f + 0.24f * cos(a).toFloat()
        val by = 0.50f + 0.24f * sin(a).toFloat()
        for (d in listOf(-0.6, 0.6)) {
            val ex = bx + 0.12f * cos(a + d).toFloat()
            val ey = by + 0.12f * sin(a + d).toFloat()
            drawLine(IceDeep, at(s, bx, by), at(s, ex, ey), 0.05f * s, StrokeCap.Round)
        }
    }
}

private val quest: Glyph = { s ->
    card(s, 0.20f, 0.10f, 0.60f, 0.80f, EverdellParchment, EverdellEdge)
    for (i in 0..2) drawLine(EverdellBark, at(s, 0.30f, 0.30f + i * 0.14f), at(s, 0.70f, 0.30f + i * 0.14f), 0.04f * s, StrokeCap.Round)
    disc(s, 0.62f, 0.74f, 0.10f, EverdellBerry)
}

private val fire: Glyph = { s ->
    drawPath(path(s, 0.50f to 0.08f, 0.76f to 0.50f, 0.70f to 0.84f, 0.30f to 0.84f, 0.24f to 0.50f), Flame)
    drawPath(path(s, 0.50f to 0.40f, 0.62f to 0.64f, 0.58f to 0.84f, 0.42f to 0.84f, 0.38f to 0.64f), EverdellGold)
}

val SilverfrostDesign = ScoreSheetDesign(
    emblem = snowflake,
    glyphs = mapOf(
        "cards" to everdellCards,
        "prosperity" to everdellProsperity,
        "quests" to quest,
        "snow-flurries" to snowflake,
        "snow-ice-storm" to snowflake,
        "tokens" to everdellPointToken,
        "journey" to everdellJourney,
        "tie.quests" to quest,
        "tie.fire" to fire,
        "tie.resources" to everdellResources
    )
)

// --- Emerland ---------------------------------------------------------------------------

private val emerald: Glyph = { s ->
    drawPath(path(s, 0.30f to 0.16f, 0.70f to 0.16f, 0.90f to 0.40f, 0.50f to 0.90f, 0.10f to 0.40f), Emerald)
    drawPath(path(s, 0.30f to 0.16f, 0.70f to 0.16f, 0.62f to 0.40f, 0.38f to 0.40f), EmeraldDeep)
}

private val artifact: Glyph = { s ->
    drawPath(path(s, 0.34f to 0.14f, 0.66f to 0.14f, 0.62f to 0.30f, 0.80f to 0.52f, 0.66f to 0.88f, 0.34f to 0.88f, 0.20f to 0.52f, 0.38f to 0.30f), EverdellResin)
    drawLine(EverdellBark, at(s, 0.24f, 0.56f), at(s, 0.76f, 0.56f), 0.05f * s)
    disc(s, 0.50f, 0.70f, 0.06f, Emerald)
}

private val lostCity: Glyph = { s ->
    drawPath(path(s, 0.10f to 0.86f, 0.90f to 0.86f, 0.74f to 0.62f, 0.26f to 0.62f), EverdellStone)
    drawPath(path(s, 0.26f to 0.62f, 0.74f to 0.62f, 0.62f to 0.40f, 0.38f to 0.40f), EverdellStoneDeep)
    drawPath(path(s, 0.38f to 0.40f, 0.62f to 0.40f, 0.50f to 0.14f), EverdellStone)
    drawRect(Jungle, at(s, 0.45f, 0.70f), Size(0.10f * s, 0.16f * s))
}

private val realm: Glyph = { s ->
    card(s, 0.10f, 0.22f, 0.80f, 0.56f, Jungle, EverdellEdge)
    drawPath(leaf(s, 0.30f, 0.66f, 0.56f, 0.34f, 0.35f), Emerald)
    drawPath(leaf(s, 0.52f, 0.66f, 0.76f, 0.40f, 0.35f), EverdellMoss)
}

private val upgraded: Glyph = { s ->
    card(s, 0.20f, 0.20f, 0.60f, 0.72f, EverdellParchment, EverdellEdge)
    drawPath(path(s, 0.50f to 0.06f, 0.74f to 0.32f, 0.26f to 0.32f), Emerald)
}

val EmerlandDesign = ScoreSheetDesign(
    emblem = emerald,
    glyphs = mapOf(
        "cards" to everdellCards,
        "prosperity" to everdellProsperity,
        "tokens" to everdellPointToken,
        "artifacts" to artifact,
        "lost-city" to lostCity,
        "realms" to realm,
        "resources" to emerald,
        "tie.upgraded" to upgraded,
        "tie.realms" to realm
    )
)
