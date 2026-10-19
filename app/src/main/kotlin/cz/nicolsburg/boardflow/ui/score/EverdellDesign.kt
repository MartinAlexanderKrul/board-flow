package cz.nicolsburg.boardflow.ui.score

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

// Everdell's forest green, berry red and woodland browns, drawn here after the score pad (not
// copied from it). Outlines are light so the icons read on the app's dark surfaces.
internal val EverdellEdge = Color(0xFFD9CFBA)
internal val EverdellParchment = Color(0xFFEFE3C6)
internal val EverdellForest = Color(0xFF5E8A4E)
internal val EverdellMoss = Color(0xFF8DB06A)
internal val EverdellBerry = Color(0xFFD0524B)
internal val EverdellBark = Color(0xFF9C7148)
internal val EverdellGold = Color(0xFFD9A945)
internal val EverdellGoldDeep = Color(0xFF9A7128)
internal val EverdellPurple = Color(0xFF9466B5)
internal val EverdellStone = Color(0xFFA89C88)
internal val EverdellStoneDeep = Color(0xFF7D7262)
internal val EverdellPearl = Color(0xFFE8EEF2)
internal val EverdellPearlEdge = Color(0xFF8FA6B5)
internal val EverdellTeal = Color(0xFF4FA6A6)
internal val EverdellResin = Color(0xFFE39B2D)

internal val everdellCards: Glyph = { s ->
    card(s, 0.20f, 0.08f, 0.60f, 0.84f, EverdellParchment, EverdellEdge)
    drawRect(EverdellForest, at(s, 0.20f, 0.20f), Size(0.60f * s, 0.18f * s))
    disc(s, 0.30f, 0.16f, 0.12f, EverdellGold, EverdellGoldDeep)
    drawPath(leaf(s, 0.36f, 0.76f, 0.66f, 0.52f, 0.3f), EverdellForest)
}

internal val everdellPointToken: Glyph = { s ->
    disc(s, 0.50f, 0.50f, 0.42f, EverdellGold)
    drawCircle(EverdellGoldDeep, 0.30f * s, at(s, 0.50f, 0.50f), style = Stroke(0.04f * s))
    drawPath(leaf(s, 0.34f, 0.66f, 0.66f, 0.34f, 0.32f), EverdellForest)
}

internal val everdellProsperity: Glyph = { s ->
    card(s, 0.20f, 0.08f, 0.60f, 0.84f, EverdellPurple, EverdellEdge)
    drawPath(star(s, 0.50f, 0.50f, 0.22f, 0.09f), EverdellGold)
}

internal val everdellJourney: Glyph = { s ->
    drawPath(path(s, 0.04f to 0.88f, 0.38f to 0.24f, 0.70f to 0.88f), EverdellStone)
    drawPath(path(s, 0.36f to 0.88f, 0.66f to 0.40f, 0.96f to 0.88f), EverdellStoneDeep)
    drawPath(path(s, 0.38f to 0.24f, 0.46f to 0.40f, 0.30f to 0.40f), Color.White.copy(alpha = 0.9f))
    drawLine(
        EverdellGold, at(s, 0.10f, 0.96f), at(s, 0.62f, 0.56f), 0.05f * s, StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.08f * s, 0.06f * s))
    )
}

internal val everdellEvent: Glyph = { s ->
    drawLine(EverdellBark, at(s, 0.26f, 0.08f), at(s, 0.26f, 0.94f), 0.06f * s, StrokeCap.Round)
    drawPath(path(s, 0.29f to 0.12f, 0.86f to 0.12f, 0.70f to 0.30f, 0.86f to 0.48f, 0.29f to 0.48f), EverdellBerry)
}

internal val everdellPearl: Glyph = { s ->
    disc(s, 0.50f, 0.54f, 0.32f, EverdellPearl, EverdellPearlEdge)
    disc(s, 0.40f, 0.44f, 0.09f, Color.White)
}

internal val everdellWonder: Glyph = { s ->
    drawRect(EverdellStone, at(s, 0.32f, 0.36f), Size(0.36f * s, 0.56f * s))
    drawPath(path(s, 0.26f to 0.38f, 0.50f to 0.06f, 0.74f to 0.38f), EverdellForest)
    drawRect(EverdellStoneDeep, at(s, 0.44f, 0.62f), Size(0.12f * s, 0.30f * s))
    drawRect(EverdellStoneDeep, at(s, 0.45f, 0.44f), Size(0.10f * s, 0.10f * s))
}

internal val everdellAdornment: Glyph = { s ->
    drawPath(path(s, 0.50f to 0.10f, 0.86f to 0.42f, 0.50f to 0.92f, 0.14f to 0.42f), EverdellTeal)
    drawLine(Color.White.copy(alpha = 0.6f), at(s, 0.14f, 0.42f), at(s, 0.86f, 0.42f), 0.04f * s)
}

internal val everdellExpansion: Glyph = { s ->
    drawCircle(EverdellEdge, 0.40f * s, at(s, 0.50f, 0.50f), style = Stroke(0.06f * s))
    drawLine(EverdellEdge, at(s, 0.50f, 0.30f), at(s, 0.50f, 0.70f), 0.08f * s, StrokeCap.Round)
    drawLine(EverdellEdge, at(s, 0.30f, 0.50f), at(s, 0.70f, 0.50f), 0.08f * s, StrokeCap.Round)
}

internal val everdellResources: Glyph = { s ->
    drawLine(EverdellBark, at(s, 0.08f, 0.30f), at(s, 0.60f, 0.16f), 0.09f * s, StrokeCap.Round)
    drawOval(EverdellStone, at(s, 0.08f, 0.56f), Size(0.40f * s, 0.30f * s))
    disc(s, 0.70f, 0.66f, 0.18f, EverdellPurple)
    disc(s, 0.70f, 0.34f, 0.12f, EverdellResin)
}

/** A twig with two leaves and a berry. */
internal val everdellSprig: Glyph = { s ->
    drawLine(EverdellBark, at(s, 0.14f, 0.86f), at(s, 0.80f, 0.20f), 0.06f * s, StrokeCap.Round)
    drawPath(leaf(s, 0.40f, 0.60f, 0.10f, 0.36f, 0.32f), EverdellMoss)
    drawPath(leaf(s, 0.58f, 0.42f, 0.86f, 0.64f, 0.32f), EverdellForest)
    disc(s, 0.80f, 0.20f, 0.12f, EverdellBerry)
}

val EverdellDesign = ScoreSheetDesign(
    emblem = everdellSprig,
    glyphs = mapOf(
        "cards" to everdellCards,
        "tokens" to everdellPointToken,
        "prosperity" to everdellProsperity,
        "journey" to everdellJourney,
        "events" to everdellEvent,
        "pearls" to everdellPearl,
        "wonders" to everdellWonder,
        "adornments" to everdellAdornment,
        "expansion" to everdellExpansion,
        "expedition" to everdellExpedition,
        "discoveries" to everdellDiscovery,
        "garland" to everdellGarland,
        "garland.cards" to everdellCards,
        "visitors" to everdellVisitor,
        "tie.events" to everdellEvent,
        "tie.resources" to everdellResources
    )
)
