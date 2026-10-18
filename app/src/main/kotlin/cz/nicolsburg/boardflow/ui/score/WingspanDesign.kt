package cz.nicolsburg.boardflow.ui.score

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

// Wingspan's teal, gold and pastel eggs, drawn here after the score pad (not copied from it).
// Outlines are light so the icons read on the app's dark surfaces.
private val Edge = Color(0xFFD9CFBA)
private val Teal = Color(0xFF3E9A9C)
private val TealDeep = Color(0xFF2C7275)
private val Gold = Color(0xFFE0A53A)
private val Plum = Color(0xFF8A74A0)
private val Sky = Color(0xFFA9D2D4)
private val Rose = Color(0xFFE0688A)
private val Honey = Color(0xFFF2C14E)
private val Straw = Color(0xFFDCC69A)
private val Berry = Color(0xFFD0524B)
private val Leaf = Color(0xFF6FA35E)
private val CardBack = Color(0xFF4A4A4C)

private val bird: Glyph = { s ->
    drawPath(path(s, 0.24f to 0.54f, 0.02f to 0.44f, 0.08f to 0.66f), Plum)
    drawOval(Teal, at(s, 0.16f, 0.36f), Size(0.58f * s, 0.38f * s))
    drawOval(TealDeep, at(s, 0.28f, 0.44f), Size(0.32f * s, 0.18f * s))
    disc(s, 0.70f, 0.36f, 0.15f, Teal)
    drawPath(path(s, 0.82f to 0.31f, 0.99f to 0.37f, 0.82f to 0.42f), Gold)
    disc(s, 0.73f, 0.33f, 0.03f, Color.White)
    drawLine(Edge, at(s, 0.40f, 0.72f), at(s, 0.38f, 0.90f), 0.04f * s, StrokeCap.Round)
    drawLine(Edge, at(s, 0.52f, 0.72f), at(s, 0.54f, 0.90f), 0.04f * s, StrokeCap.Round)
}

private val bonusCard: Glyph = { s ->
    card(s, 0.20f, 0.08f, 0.60f, 0.84f, Straw, Edge)
    drawRect(Gold, at(s, 0.20f, 0.26f), Size(0.60f * s, 0.14f * s))
    drawPath(star(s, 0.50f, 0.64f, 0.16f, 0.07f), TealDeep)
}

private val goal: Glyph = { s ->
    drawPath(path(s, 0.34f to 0.56f, 0.24f to 0.94f, 0.40f to 0.84f, 0.48f to 0.98f, 0.50f to 0.60f), TealDeep)
    drawPath(path(s, 0.66f to 0.56f, 0.76f to 0.94f, 0.60f to 0.84f, 0.52f to 0.98f, 0.50f to 0.60f), TealDeep)
    disc(s, 0.50f, 0.40f, 0.30f, Teal)
    disc(s, 0.50f, 0.40f, 0.18f, Straw)
    drawPath(star(s, 0.50f, 0.40f, 0.12f, 0.05f), Gold)
}

private val egg: Glyph = { s ->
    val shape = Path().apply {
        moveTo(0.50f * s, 0.06f * s)
        cubicTo(0.76f * s, 0.06f * s, 0.88f * s, 0.50f * s, 0.88f * s, 0.64f * s)
        cubicTo(0.88f * s, 0.86f * s, 0.70f * s, 0.96f * s, 0.50f * s, 0.96f * s)
        cubicTo(0.30f * s, 0.96f * s, 0.12f * s, 0.86f * s, 0.12f * s, 0.64f * s)
        cubicTo(0.12f * s, 0.50f * s, 0.24f * s, 0.06f * s, 0.50f * s, 0.06f * s)
        close()
    }
    drawPath(shape, Sky)
    disc(s, 0.38f, 0.40f, 0.07f, Color.White.copy(alpha = 0.7f))
}

private val foodToken: Glyph = { s ->
    disc(s, 0.50f, 0.50f, 0.42f, Straw)
    // A wheat ear: a stem and paired grains.
    drawLine(TealDeep, at(s, 0.50f, 0.80f), at(s, 0.50f, 0.24f), 0.04f * s, StrokeCap.Round)
    for (i in 0..2) {
        val y = 0.30f + i * 0.15f
        drawPath(leaf(s, 0.50f, y + 0.12f, 0.34f, y, 0.35f), Gold)
        drawPath(leaf(s, 0.50f, y + 0.12f, 0.66f, y, 0.35f), Gold)
    }
}

private val tucked: Glyph = { s ->
    card(s, 0.14f, 0.20f, 0.50f, 0.68f, CardBack, Edge.copy(alpha = 0.5f))
    card(s, 0.36f, 0.10f, 0.50f, 0.68f, Straw, Edge)
    drawPath(path(s, 0.50f to 0.36f, 0.72f to 0.36f, 0.61f to 0.52f), TealDeep)
}

private val nectar: Glyph = { s ->
    for (i in 0 until 5) {
        val a = -Math.PI / 2 + i * 2 * Math.PI / 5
        disc(s, 0.50f + 0.22f * Math.cos(a).toFloat(), 0.50f + 0.22f * Math.sin(a).toFloat(), 0.18f, Rose)
    }
    disc(s, 0.50f, 0.50f, 0.12f, Honey)
}

private val habitatForest: Glyph = { s ->
    drawPath(path(s, 0.50f to 0.08f, 0.86f to 0.70f, 0.14f to 0.70f), Leaf)
    drawRect(Color(0xFF9C7148), at(s, 0.44f, 0.70f), Size(0.12f * s, 0.22f * s))
}

private val habitatGrassland: Glyph = { s ->
    for ((x, h) in listOf(0.22f to 0.50f, 0.40f to 0.70f, 0.58f to 0.56f, 0.76f to 0.66f)) {
        drawPath(leaf(s, x, 0.92f, x + 0.04f, 0.92f - h, 0.12f), Color(0xFFCDB45A))
    }
}

private val habitatWetland: Glyph = { s ->
    for (i in 0..2) {
        val y = 0.34f + i * 0.20f
        drawArc(Color(0xFF6AA8D0), 180f, 180f, false, at(s, 0.10f, y), Size(0.40f * s, 0.20f * s), style = Stroke(0.07f * s, cap = StrokeCap.Round))
        drawArc(Color(0xFF6AA8D0), 0f, 180f, false, at(s, 0.50f, y), Size(0.40f * s, 0.20f * s), style = Stroke(0.07f * s, cap = StrokeCap.Round))
    }
}

private val unusedFood: Glyph = { s ->
    drawLine(Leaf, at(s, 0.50f, 0.16f), at(s, 0.34f, 0.50f), 0.05f * s, StrokeCap.Round)
    drawLine(Leaf, at(s, 0.50f, 0.16f), at(s, 0.68f, 0.52f), 0.05f * s, StrokeCap.Round)
    drawPath(leaf(s, 0.50f, 0.16f, 0.74f, 0.10f, 0.3f), Leaf)
    disc(s, 0.32f, 0.66f, 0.20f, Berry)
    disc(s, 0.68f, 0.68f, 0.20f, Berry)
}

/** Americas: a hummingbird hovering at a flower. */
private val hummingbird: Glyph = { s ->
    drawOval(Teal, at(s, 0.30f, 0.40f), Size(0.36f * s, 0.20f * s))
    disc(s, 0.66f, 0.42f, 0.09f, Color(0xFF5FB86A))
    drawLine(Edge, at(s, 0.74f, 0.42f), at(s, 0.98f, 0.36f), 0.035f * s, StrokeCap.Round)
    drawPath(leaf(s, 0.44f, 0.42f, 0.28f, 0.10f, 0.35f), Sky)
    drawPath(leaf(s, 0.42f, 0.44f, 0.58f, 0.12f, 0.35f), Sky)
    drawPath(path(s, 0.32f to 0.50f, 0.06f to 0.64f, 0.12f to 0.48f), Rose)
}

/** Asia Duet mode: a half-black, half-white token. */
private val duetToken: Glyph = { s ->
    disc(s, 0.50f, 0.50f, 0.40f, Color.White, Edge)
    drawArc(Color(0xFF2B2B2D), 90f, 180f, true, at(s, 0.10f, 0.10f), Size(0.80f * s, 0.80f * s))
    disc(s, 0.50f, 0.30f, 0.20f, Color(0xFF2B2B2D))
    disc(s, 0.50f, 0.70f, 0.20f, Color.White)
}

val WingspanDesign = ScoreSheetDesign(
    emblem = bird,
    glyphs = mapOf(
        "birds" to bird,
        "bonus" to bonusCard,
        "goals" to goal,
        "eggs" to egg,
        "cached" to foodToken,
        "tucked" to tucked,
        "nectar" to nectar,
        "nectar.forest" to habitatForest,
        "nectar.grassland" to habitatGrassland,
        "nectar.wetland" to habitatWetland,
        "hummingbirds" to hummingbird,
        "duet" to duetToken,
        "tie.food" to unusedFood
    )
)
