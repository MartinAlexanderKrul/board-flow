package cz.nicolsburg.boardflow.ui.score

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

// Everdell's forest green, berry red and woodland browns, drawn here after the score pad (not
// copied from it). Outlines are light so the icons read on the app's dark surfaces.
private val Edge = Color(0xFFD9CFBA)
private val Parchment = Color(0xFFEFE3C6)
private val Forest = Color(0xFF5E8A4E)
private val Moss = Color(0xFF8DB06A)
private val Berry = Color(0xFFD0524B)
private val Bark = Color(0xFF9C7148)
private val Gold = Color(0xFFD9A945)
private val GoldDeep = Color(0xFF9A7128)
private val Purple = Color(0xFF9466B5)
private val Stone = Color(0xFFA89C88)
private val StoneDeep = Color(0xFF7D7262)
private val Pearl = Color(0xFFE8EEF2)
private val PearlEdge = Color(0xFF8FA6B5)
private val Teal = Color(0xFF4FA6A6)
private val Resin = Color(0xFFE39B2D)

private val cards: Glyph = { s ->
    card(s, 0.20f, 0.08f, 0.60f, 0.84f, Parchment, Edge)
    drawRect(Forest, at(s, 0.20f, 0.20f), Size(0.60f * s, 0.18f * s))
    disc(s, 0.30f, 0.16f, 0.12f, Gold, GoldDeep)
    drawPath(leaf(s, 0.36f, 0.76f, 0.66f, 0.52f, 0.3f), Forest)
}

private val pointToken: Glyph = { s ->
    disc(s, 0.50f, 0.50f, 0.42f, Gold)
    drawCircle(GoldDeep, 0.30f * s, at(s, 0.50f, 0.50f), style = Stroke(0.04f * s))
    drawPath(leaf(s, 0.34f, 0.66f, 0.66f, 0.34f, 0.32f), Forest)
}

private val prosperity: Glyph = { s ->
    card(s, 0.20f, 0.08f, 0.60f, 0.84f, Purple, Edge)
    drawPath(star(s, 0.50f, 0.50f, 0.22f, 0.09f), Gold)
}

private val journey: Glyph = { s ->
    drawPath(path(s, 0.04f to 0.88f, 0.38f to 0.24f, 0.70f to 0.88f), Stone)
    drawPath(path(s, 0.36f to 0.88f, 0.66f to 0.40f, 0.96f to 0.88f), StoneDeep)
    drawPath(path(s, 0.38f to 0.24f, 0.46f to 0.40f, 0.30f to 0.40f), Color.White.copy(alpha = 0.9f))
    drawLine(
        Gold, at(s, 0.10f, 0.96f), at(s, 0.62f, 0.56f), 0.05f * s, StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.08f * s, 0.06f * s))
    )
}

private val event: Glyph = { s ->
    drawLine(Bark, at(s, 0.26f, 0.08f), at(s, 0.26f, 0.94f), 0.06f * s, StrokeCap.Round)
    drawPath(path(s, 0.29f to 0.12f, 0.86f to 0.12f, 0.70f to 0.30f, 0.86f to 0.48f, 0.29f to 0.48f), Berry)
}

private val pearl: Glyph = { s ->
    disc(s, 0.50f, 0.54f, 0.32f, Pearl, PearlEdge)
    disc(s, 0.40f, 0.44f, 0.09f, Color.White)
}

private val wonder: Glyph = { s ->
    drawRect(Stone, at(s, 0.32f, 0.36f), Size(0.36f * s, 0.56f * s))
    drawPath(path(s, 0.26f to 0.38f, 0.50f to 0.06f, 0.74f to 0.38f), Forest)
    drawRect(StoneDeep, at(s, 0.44f, 0.62f), Size(0.12f * s, 0.30f * s))
    drawRect(StoneDeep, at(s, 0.45f, 0.44f), Size(0.10f * s, 0.10f * s))
}

private val adornment: Glyph = { s ->
    drawPath(path(s, 0.50f to 0.10f, 0.86f to 0.42f, 0.50f to 0.92f, 0.14f to 0.42f), Teal)
    drawLine(Color.White.copy(alpha = 0.6f), at(s, 0.14f, 0.42f), at(s, 0.86f, 0.42f), 0.04f * s)
}

private val expansion: Glyph = { s ->
    drawCircle(Edge, 0.40f * s, at(s, 0.50f, 0.50f), style = Stroke(0.06f * s))
    drawLine(Edge, at(s, 0.50f, 0.30f), at(s, 0.50f, 0.70f), 0.08f * s, StrokeCap.Round)
    drawLine(Edge, at(s, 0.30f, 0.50f), at(s, 0.70f, 0.50f), 0.08f * s, StrokeCap.Round)
}

private val resources: Glyph = { s ->
    drawLine(Bark, at(s, 0.08f, 0.30f), at(s, 0.60f, 0.16f), 0.09f * s, StrokeCap.Round)
    drawOval(Stone, at(s, 0.08f, 0.56f), Size(0.40f * s, 0.30f * s))
    disc(s, 0.70f, 0.66f, 0.18f, Purple)
    disc(s, 0.70f, 0.34f, 0.12f, Resin)
}

/** A twig with two leaves and a berry. */
private val sprig: Glyph = { s ->
    drawLine(Bark, at(s, 0.14f, 0.86f), at(s, 0.80f, 0.20f), 0.06f * s, StrokeCap.Round)
    drawPath(leaf(s, 0.40f, 0.60f, 0.10f, 0.36f, 0.32f), Moss)
    drawPath(leaf(s, 0.58f, 0.42f, 0.86f, 0.64f, 0.32f), Forest)
    disc(s, 0.80f, 0.20f, 0.12f, Berry)
}

val EverdellDesign = ScoreSheetDesign(
    emblem = sprig,
    glyphs = mapOf(
        "cards" to cards,
        "tokens" to pointToken,
        "prosperity" to prosperity,
        "journey" to journey,
        "events" to event,
        "pearls" to pearl,
        "wonders" to wonder,
        "adornments" to adornment,
        "expansion" to expansion,
        "tie.events" to event,
        "tie.resources" to resources
    )
)
