package cz.nicolsburg.boardflow.ui.score

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

// Finspan and Wyrmspan, Wingspan's siblings: drawn after their score pads, not copied from them.
// Light outlines so the icons read on the app's dark surfaces.

private val Edge = Color(0xFFD9CFBA)
private val Gold = Color(0xFFE0A53A)
private val Cream = Color(0xFFEFE3C6)

// --- Finspan ------------------------------------------------------------------------------

private val Ocean = Color(0xFF3F8FC0)
private val OceanDeep = Color(0xFF2A6890)
private val Fin = Color(0xFFE8915A)
private val Kelp = Color(0xFF5FA36A)
private val Roe = Color(0xFFF2B8A0)
private val CoralPink = Color(0xFFE06A8A)
private val CoralBlue = Color(0xFF5FA8E0)
private val CoralPurple = Color(0xFF9B6BC8)
private val CoralGreen = Color(0xFF6CC27A)

/** A fish facing right. */
private fun fishPath(s: Float, x: Float, y: Float, w: Float, h: Float): Path = Path().apply {
    moveTo((x + w) * s, (y + h / 2) * s)
    quadraticBezierTo((x + w * 0.55f) * s, (y - h * 0.15f) * s, (x + w * 0.22f) * s, (y + h / 2) * s)
    quadraticBezierTo((x + w * 0.55f) * s, (y + h * 1.15f) * s, (x + w) * s, (y + h / 2) * s)
    close()
    moveTo((x + w * 0.24f) * s, (y + h / 2) * s)
    lineTo(x * s, (y + h * 0.1f) * s)
    lineTo(x * s, (y + h * 0.9f) * s)
    close()
}

private val fish: Glyph = { s ->
    drawPath(fishPath(s, 0.06f, 0.28f, 0.86f, 0.44f), Fin)
    disc(s, 0.78f, 0.47f, 0.04f, Color.White)
}

private val achievement: Glyph = { s ->
    drawPath(path(s, 0.34f to 0.56f, 0.24f to 0.94f, 0.40f to 0.84f, 0.48f to 0.98f, 0.50f to 0.60f), OceanDeep)
    drawPath(path(s, 0.66f to 0.56f, 0.76f to 0.94f, 0.60f to 0.84f, 0.52f to 0.98f, 0.50f to 0.60f), OceanDeep)
    disc(s, 0.50f, 0.40f, 0.30f, Ocean)
    drawPath(star(s, 0.50f, 0.40f, 0.16f, 0.07f), Gold)
}

private val gameEndCard: Glyph = { s ->
    card(s, 0.20f, 0.08f, 0.60f, 0.84f, Cream, Edge)
    drawRect(Gold, at(s, 0.20f, 0.60f), Size(0.60f * s, 0.18f * s))
    drawPath(fishPath(s, 0.30f, 0.20f, 0.42f, 0.24f), Fin)
}

private val consumed: Glyph = { s ->
    drawPath(fishPath(s, 0.08f, 0.38f, 0.50f, 0.26f), Edge.copy(alpha = 0.5f))
    drawPath(fishPath(s, 0.30f, 0.20f, 0.64f, 0.44f), Fin)
}

private val fishEgg: Glyph = { s ->
    for ((x, y) in listOf(0.34f to 0.40f, 0.62f to 0.36f, 0.48f to 0.64f)) {
        disc(s, x, y, 0.15f, Roe)
        disc(s, x - 0.03f, y - 0.03f, 0.05f, Color.White.copy(alpha = 0.8f))
    }
}

private val young: Glyph = { s ->
    drawPath(fishPath(s, 0.10f, 0.20f, 0.40f, 0.20f), Fin)
    drawPath(fishPath(s, 0.50f, 0.44f, 0.40f, 0.20f), Fin)
    drawPath(fishPath(s, 0.16f, 0.66f, 0.40f, 0.20f), Fin)
}

private val school: Glyph = { s ->
    disc(s, 0.50f, 0.50f, 0.44f, OceanDeep)
    for ((x, y) in listOf(0.18f to 0.26f, 0.46f to 0.20f, 0.30f to 0.48f, 0.56f to 0.46f, 0.36f to 0.70f)) {
        drawPath(fishPath(s, x, y, 0.28f, 0.14f), Cream)
    }
}

private val coral: Glyph = { s ->
    drawLine(CoralPink, at(s, 0.50f, 0.92f), at(s, 0.50f, 0.30f), 0.10f * s, StrokeCap.Round)
    drawLine(CoralPink, at(s, 0.50f, 0.62f), at(s, 0.24f, 0.36f), 0.09f * s, StrokeCap.Round)
    drawLine(CoralPink, at(s, 0.50f, 0.54f), at(s, 0.78f, 0.24f), 0.09f * s, StrokeCap.Round)
    drawLine(CoralPink, at(s, 0.24f, 0.36f), at(s, 0.18f, 0.16f), 0.07f * s, StrokeCap.Round)
}

private val reefs: Glyph = { s ->
    disc(s, 0.24f, 0.62f, 0.18f, CoralBlue)
    disc(s, 0.50f, 0.42f, 0.18f, CoralPurple)
    disc(s, 0.76f, 0.62f, 0.18f, CoralGreen)
}

private val cardsInHand: Glyph = { s ->
    card(s, 0.12f, 0.24f, 0.44f, 0.62f, OceanDeep, Edge)
    card(s, 0.30f, 0.16f, 0.44f, 0.62f, Cream, Edge)
    card(s, 0.48f, 0.24f, 0.44f, 0.62f, Cream, Edge)
}

val FinspanDesign = ScoreSheetDesign(
    emblem = fish,
    glyphs = mapOf(
        "achievements" to achievement,
        "game-end" to gameEndCard,
        "fish" to fish,
        "consumed" to consumed,
        "eggs" to fishEgg,
        "young" to young,
        "schools" to school,
        "coral" to coral,
        "reefs" to reefs,
        "tie.hand" to cardsInHand
    )
)

// --- Wyrmspan -----------------------------------------------------------------------------

private val Wyrm = Color(0xFFB07AD0)
private val WyrmDeep = Color(0xFF7A4FA0)
private val Crimson = Color(0xFFC8505A)
private val Crystal = Color(0xFF6FC3D8)
private val Meat = Color(0xFFD86A5A)
private val Milk = Color(0xFFF0EEE6)
private val Coin = Color(0xFFE0B040)
private val Stone = Color(0xFF8A8278)

private val dragon: Glyph = { s ->
    drawPath(leaf(s, 0.50f, 0.52f, 0.12f, 0.12f, 0.35f), WyrmDeep)
    drawPath(leaf(s, 0.50f, 0.52f, 0.88f, 0.12f, 0.35f), WyrmDeep)
    drawOval(Wyrm, at(s, 0.34f, 0.38f), Size(0.32f * s, 0.46f * s))
    disc(s, 0.50f, 0.30f, 0.12f, Wyrm)
    drawLine(Wyrm, at(s, 0.50f, 0.82f), at(s, 0.66f, 0.96f), 0.06f * s, StrokeCap.Round)
    disc(s, 0.46f, 0.28f, 0.03f, Color.White)
    disc(s, 0.54f, 0.28f, 0.03f, Color.White)
}

private val guild: Glyph = { s ->
    drawPath(path(s, 0.18f to 0.14f, 0.82f to 0.14f, 0.82f to 0.62f, 0.50f to 0.92f, 0.18f to 0.62f), Crimson)
    drawPath(path(s, 0.18f to 0.14f, 0.82f to 0.14f, 0.82f to 0.62f, 0.50f to 0.92f, 0.18f to 0.62f), Edge, style = Stroke(0.04f * s))
    drawPath(star(s, 0.50f, 0.46f, 0.18f, 0.08f), Gold)
}

private val endGameDragon: Glyph = { s ->
    card(s, 0.20f, 0.08f, 0.60f, 0.84f, Cream, Edge)
    drawRect(Gold, at(s, 0.20f, 0.64f), Size(0.60f * s, 0.16f * s))
    disc(s, 0.50f, 0.36f, 0.16f, Wyrm)
}

private val dragonEgg: Glyph = { s ->
    val shape = Path().apply {
        moveTo(0.50f * s, 0.06f * s)
        cubicTo(0.76f * s, 0.06f * s, 0.88f * s, 0.50f * s, 0.88f * s, 0.64f * s)
        cubicTo(0.88f * s, 0.86f * s, 0.70f * s, 0.96f * s, 0.50f * s, 0.96f * s)
        cubicTo(0.30f * s, 0.96f * s, 0.12f * s, 0.86f * s, 0.12f * s, 0.64f * s)
        cubicTo(0.12f * s, 0.50f * s, 0.24f * s, 0.06f * s, 0.50f * s, 0.06f * s)
        close()
    }
    drawPath(shape, Crystal)
    for (i in 0..2) disc(s, 0.32f + i * 0.18f, 0.62f, 0.05f, WyrmDeep)
}

private val cached: Glyph = { s ->
    drawPath(path(s, 0.30f to 0.20f, 0.46f to 0.10f, 0.58f to 0.36f, 0.40f to 0.48f), Crystal)
    disc(s, 0.70f, 0.40f, 0.16f, Meat)
    disc(s, 0.36f, 0.70f, 0.18f, Milk, Edge)
    disc(s, 0.70f, 0.74f, 0.13f, Gold)
}

private val tuckedDragon: Glyph = { s ->
    card(s, 0.14f, 0.20f, 0.50f, 0.68f, WyrmDeep, Edge.copy(alpha = 0.5f))
    card(s, 0.36f, 0.10f, 0.50f, 0.68f, Cream, Edge)
    disc(s, 0.61f, 0.44f, 0.12f, Wyrm)
}

private val objective: Glyph = { s ->
    drawRect(Stone, at(s, 0.14f, 0.20f), Size(0.72f * s, 0.62f * s))
    for (i in 0..2) drawRect(Crimson, at(s, 0.24f + i * 0.20f, 0.56f - i * 0.14f), Size(0.12f * s, (0.18f + i * 0.14f) * s))
}

private val coin: Glyph = { s ->
    disc(s, 0.50f, 0.50f, 0.38f, Coin, Edge)
    drawCircle(Color(0xFFA67C20), 0.26f * s, at(s, 0.50f, 0.50f), style = Stroke(0.04f * s))
}

private val items: Glyph = { s ->
    card(s, 0.08f, 0.30f, 0.34f, 0.50f, Cream, Edge)
    disc(s, 0.58f, 0.40f, 0.14f, Meat)
    drawPath(path(s, 0.74f to 0.48f, 0.90f to 0.58f, 0.80f to 0.86f, 0.64f to 0.76f), Crystal)
}

private val fledgling: Glyph = { s ->
    drawArc(Cream, 0f, 180f, true, at(s, 0.18f, 0.36f), Size(0.64f * s, 0.56f * s))
    drawPath(path(s, 0.18f to 0.64f, 0.30f to 0.56f, 0.40f to 0.66f, 0.50f to 0.56f, 0.60f to 0.66f, 0.70f to 0.56f, 0.82f to 0.64f), Cream)
    disc(s, 0.50f, 0.36f, 0.20f, Wyrm)
    disc(s, 0.45f, 0.33f, 0.03f, Color.White)
    disc(s, 0.55f, 0.33f, 0.03f, Color.White)
}

val WyrmspanDesign = ScoreSheetDesign(
    emblem = dragon,
    glyphs = mapOf(
        "guild" to guild,
        "dragons" to dragon,
        "end-game" to endGameDragon,
        "eggs" to dragonEgg,
        "cached" to cached,
        "tucked" to tuckedDragon,
        "objectives" to objective,
        "coins" to coin,
        "items" to items,
        "fledglings" to fledgling,
        "tie.dragons" to dragon
    )
)
