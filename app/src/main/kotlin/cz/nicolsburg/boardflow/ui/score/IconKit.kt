package cz.nicolsburg.boardflow.ui.score

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared row icons for the score sheets that do not need hand-drawn ones: simple shapes, each
 * tinted with the game's own colour. Light outlines so they read on the app's dark surfaces.
 * A game's design picks from these (`Kit.coin(gold)`, `Kit.building(red)`...) next to any icons of
 * its own.
 */
object Kit {
    val Edge = Color(0xFFD9CFBA)
    val Cream = Color(0xFFEFE3C6)
    val Gold = Color(0xFFE0A53A)
    val Red = Color(0xFFD0524B)
    val Blue = Color(0xFF4F8FD0)
    val Green = Color(0xFF5FA36A)
    val Yellow = Color(0xFFE8C547)
    val Purple = Color(0xFF9466B5)
    val Brown = Color(0xFF9C7148)
    val Grey = Color(0xFF9A948A)
    val Teal = Color(0xFF3E9A9C)
    val Orange = Color(0xFFE8915A)
    val Pink = Color(0xFFE0688A)
    val White = Color(0xFFF0EEE6)
    val Dark = Color(0xFF3A3A3C)

    fun coin(c: Color = Gold): Glyph = { s ->
        disc(s, 0.50f, 0.50f, 0.38f, c, Edge)
        drawCircle(Color.Black.copy(alpha = 0.25f), 0.26f * s, at(s, 0.50f, 0.50f), style = Stroke(0.04f * s))
    }

    fun coins(c: Color = Gold): Glyph = { s ->
        disc(s, 0.36f, 0.62f, 0.26f, c, Edge)
        disc(s, 0.64f, 0.40f, 0.26f, c, Edge)
    }

    fun card(c: Color, band: Color? = null): Glyph = { s ->
        card(s, 0.20f, 0.08f, 0.60f, 0.84f, c, Edge)
        if (band != null) drawRect(band, at(s, 0.20f, 0.22f), Size(0.60f * s, 0.16f * s))
    }

    fun cards(c: Color, back: Color = Dark): Glyph = { s ->
        card(s, 0.14f, 0.20f, 0.50f, 0.68f, back, Edge.copy(alpha = 0.5f))
        card(s, 0.36f, 0.10f, 0.50f, 0.68f, c, Edge)
    }

    fun star(c: Color = Gold): Glyph = { s -> drawPath(star(s, 0.50f, 0.52f, 0.42f, 0.18f), c) }

    fun laurel(c: Color = Gold): Glyph = { s ->
        for (side in listOf(-1f, 1f)) {
            for (i in 0..3) {
                val y = 0.78f - i * 0.17f
                val x = 0.50f + side * (0.18f + i * 0.04f)
                drawPath(leaf(s, x, y, x + side * 0.16f, y - 0.10f, 0.35f), c)
            }
        }
        disc(s, 0.50f, 0.82f, 0.06f, c)
    }

    fun shield(c: Color): Glyph = { s ->
        val p = path(s, 0.18f to 0.12f, 0.82f to 0.12f, 0.82f to 0.52f, 0.50f to 0.92f, 0.18f to 0.52f)
        drawPath(p, c)
        drawPath(p, Edge, style = Stroke(0.04f * s))
    }

    fun swords(c: Color = Grey): Glyph = { s ->
        drawLine(c, at(s, 0.16f, 0.84f), at(s, 0.84f, 0.16f), 0.08f * s, StrokeCap.Round)
        drawLine(c, at(s, 0.84f, 0.84f), at(s, 0.16f, 0.16f), 0.08f * s, StrokeCap.Round)
        drawLine(Brown, at(s, 0.18f, 0.66f), at(s, 0.34f, 0.82f), 0.07f * s, StrokeCap.Round)
        drawLine(Brown, at(s, 0.82f, 0.66f), at(s, 0.66f, 0.82f), 0.07f * s, StrokeCap.Round)
    }

    fun crown(c: Color = Gold): Glyph = { s ->
        drawPath(path(s, 0.12f to 0.78f, 0.12f to 0.30f, 0.32f to 0.52f, 0.50f to 0.22f, 0.68f to 0.52f, 0.88f to 0.30f, 0.88f to 0.78f), c)
        drawRect(c, at(s, 0.12f, 0.74f), Size(0.76f * s, 0.10f * s))
    }

    fun trophy(c: Color = Gold): Glyph = { s ->
        drawArc(c, 0f, 180f, true, at(s, 0.24f, 0.02f), Size(0.52f * s, 0.62f * s))
        drawRect(c, at(s, 0.24f, 0.16f), Size(0.52f * s, 0.18f * s))
        drawRect(c, at(s, 0.44f, 0.60f), Size(0.12f * s, 0.18f * s))
        drawRect(c, at(s, 0.30f, 0.78f), Size(0.40f * s, 0.10f * s))
    }

    fun medal(c: Color = Gold, ribbon: Color = Red): Glyph = { s ->
        drawPath(path(s, 0.30f to 0.06f, 0.46f to 0.06f, 0.56f to 0.44f, 0.40f to 0.44f), ribbon)
        drawPath(path(s, 0.70f to 0.06f, 0.54f to 0.06f, 0.44f to 0.44f, 0.60f to 0.44f), ribbon)
        disc(s, 0.50f, 0.64f, 0.26f, c, Edge)
    }

    fun house(c: Color, roof: Color = Red): Glyph = { s ->
        drawRect(c, at(s, 0.22f, 0.46f), Size(0.56f * s, 0.42f * s))
        drawPath(path(s, 0.12f to 0.50f, 0.50f to 0.14f, 0.88f to 0.50f), roof)
        drawRect(Dark, at(s, 0.44f, 0.64f), Size(0.12f * s, 0.24f * s))
    }

    fun building(c: Color): Glyph = { s ->
        drawRect(c, at(s, 0.14f, 0.36f), Size(0.32f * s, 0.54f * s))
        drawRect(c, at(s, 0.50f, 0.16f), Size(0.36f * s, 0.74f * s))
        for (r in 0..2) for (k in 0..1) drawRect(Dark, at(s, 0.56f + k * 0.14f, 0.24f + r * 0.18f), Size(0.08f * s, 0.10f * s))
        drawRect(Dark, at(s, 0.22f, 0.46f), Size(0.08f * s, 0.10f * s))
    }

    fun castle(c: Color = Grey): Glyph = { s ->
        drawRect(c, at(s, 0.16f, 0.36f), Size(0.68f * s, 0.54f * s))
        for (i in 0..3) drawRect(c, at(s, 0.16f + i * 0.19f, 0.24f), Size(0.11f * s, 0.14f * s))
        drawArc(Dark, 180f, 180f, true, at(s, 0.40f, 0.62f), Size(0.20f * s, 0.20f * s))
        drawRect(Dark, at(s, 0.40f, 0.72f), Size(0.20f * s, 0.18f * s))
    }

    fun temple(c: Color = Cream): Glyph = { s ->
        drawPath(path(s, 0.10f to 0.32f, 0.50f to 0.08f, 0.90f to 0.32f), c)
        for (i in 0..3) drawRect(c, at(s, 0.16f + i * 0.20f, 0.36f), Size(0.08f * s, 0.44f * s))
        drawRect(c, at(s, 0.10f, 0.80f), Size(0.80f * s, 0.10f * s))
    }

    fun tower(c: Color = Grey): Glyph = { s ->
        drawRect(c, at(s, 0.34f, 0.30f), Size(0.32f * s, 0.62f * s))
        drawPath(path(s, 0.28f to 0.32f, 0.50f to 0.04f, 0.72f to 0.32f), Red)
    }

    fun factory(c: Color = Grey): Glyph = { s ->
        drawPath(path(s, 0.10f to 0.90f, 0.10f to 0.50f, 0.36f to 0.36f, 0.36f to 0.50f, 0.62f to 0.36f, 0.62f to 0.50f, 0.90f to 0.36f, 0.90f to 0.90f), c)
        drawRect(c, at(s, 0.72f, 0.12f), Size(0.10f * s, 0.30f * s))
    }

    fun ship(c: Color = Brown, sail: Color = Cream): Glyph = { s ->
        drawPath(path(s, 0.10f to 0.62f, 0.90f to 0.62f, 0.76f to 0.84f, 0.24f to 0.84f), c)
        drawLine(Edge, at(s, 0.50f, 0.10f), at(s, 0.50f, 0.62f), 0.04f * s)
        drawPath(path(s, 0.52f to 0.14f, 0.84f to 0.54f, 0.52f to 0.54f), sail)
        drawPath(path(s, 0.48f to 0.22f, 0.48f to 0.54f, 0.22f to 0.54f), sail)
    }

    fun train(c: Color = Red): Glyph = { s ->
        drawRect(c, at(s, 0.12f, 0.36f), Size(0.56f * s, 0.34f * s))
        drawRect(c, at(s, 0.52f, 0.20f), Size(0.20f * s, 0.20f * s))
        drawRect(Dark, at(s, 0.68f, 0.46f), Size(0.20f * s, 0.24f * s))
        disc(s, 0.26f, 0.76f, 0.10f, Dark, Edge)
        disc(s, 0.54f, 0.76f, 0.10f, Dark, Edge)
    }

    fun route(c: Color = Gold): Glyph = { s ->
        for (i in 0..3) {
            val x = 0.06f + i * 0.23f
            drawRect(c, at(s, x, 0.42f), Size(0.18f * s, 0.16f * s))
        }
    }

    fun ticket(c: Color = Cream): Glyph = { s ->
        card(s, 0.10f, 0.26f, 0.80f, 0.48f, c, Edge)
        disc(s, 0.30f, 0.50f, 0.07f, Red)
        disc(s, 0.70f, 0.50f, 0.07f, Red)
        drawLine(Dark, at(s, 0.36f, 0.50f), at(s, 0.64f, 0.50f), 0.04f * s, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(0.05f * s, 0.04f * s)))
    }

    fun meeple(c: Color): Glyph = { s ->
        disc(s, 0.50f, 0.24f, 0.14f, c)
        drawPath(path(s, 0.50f to 0.36f, 0.88f to 0.50f, 0.86f to 0.60f, 0.66f to 0.56f, 0.76f to 0.92f, 0.56f to 0.92f, 0.50f to 0.74f, 0.44f to 0.92f, 0.24f to 0.92f, 0.34f to 0.56f, 0.14f to 0.60f, 0.12f to 0.50f), c)
    }

    fun people(c: Color): Glyph = { s ->
        disc(s, 0.32f, 0.30f, 0.12f, c)
        disc(s, 0.68f, 0.30f, 0.12f, c)
        drawArc(c, 180f, 180f, true, at(s, 0.12f, 0.46f), Size(0.40f * s, 0.46f * s))
        drawArc(c, 180f, 180f, true, at(s, 0.48f, 0.46f), Size(0.40f * s, 0.46f * s))
    }

    fun animal(c: Color): Glyph = { s ->
        drawOval(c, at(s, 0.18f, 0.32f), Size(0.56f * s, 0.34f * s))
        disc(s, 0.76f, 0.34f, 0.13f, c)
        for (x in listOf(0.26f, 0.38f, 0.56f, 0.66f)) drawLine(c, at(s, x, 0.60f), at(s, x, 0.86f), 0.07f * s, StrokeCap.Round)
        drawLine(c, at(s, 0.20f, 0.44f), at(s, 0.08f, 0.56f), 0.05f * s, StrokeCap.Round)
    }

    fun sheep(): Glyph = { s ->
        for ((x, y) in listOf(0.30f to 0.44f, 0.46f to 0.36f, 0.60f to 0.46f, 0.40f to 0.56f)) disc(s, x, y, 0.16f, White)
        disc(s, 0.78f, 0.40f, 0.11f, Dark)
        for (x in listOf(0.32f, 0.58f)) drawLine(Dark, at(s, x, 0.66f), at(s, x, 0.88f), 0.06f * s, StrokeCap.Round)
    }

    fun fish(c: Color = Orange): Glyph = { s ->
        val p = Path().apply {
            moveTo(0.92f * s, 0.50f * s)
            quadraticBezierTo(0.55f * s, 0.18f * s, 0.24f * s, 0.50f * s)
            quadraticBezierTo(0.55f * s, 0.82f * s, 0.92f * s, 0.50f * s)
            close()
            moveTo(0.26f * s, 0.50f * s); lineTo(0.06f * s, 0.32f * s); lineTo(0.06f * s, 0.68f * s); close()
        }
        drawPath(p, c)
        disc(s, 0.78f, 0.47f, 0.04f, Color.White)
    }

    fun bird(c: Color = Teal): Glyph = { s ->
        drawOval(c, at(s, 0.16f, 0.38f), Size(0.56f * s, 0.34f * s))
        disc(s, 0.70f, 0.36f, 0.14f, c)
        drawPath(path(s, 0.82f to 0.32f, 0.98f to 0.38f, 0.82f to 0.42f), Gold)
        drawPath(path(s, 0.24f to 0.52f, 0.04f to 0.42f, 0.10f to 0.66f), c)
    }

    fun tree(c: Color = Green): Glyph = { s ->
        drawRect(Brown, at(s, 0.44f, 0.60f), Size(0.12f * s, 0.32f * s))
        disc(s, 0.50f, 0.40f, 0.30f, c)
    }

    fun pine(c: Color = Green): Glyph = { s ->
        drawPath(path(s, 0.50f to 0.06f, 0.84f to 0.70f, 0.16f to 0.70f), c)
        drawRect(Brown, at(s, 0.44f, 0.70f), Size(0.12f * s, 0.22f * s))
    }

    fun leafG(c: Color = Green): Glyph = { s ->
        drawPath(leaf(s, 0.18f, 0.84f, 0.84f, 0.16f, 0.32f), c)
        drawLine(Color.Black.copy(alpha = 0.25f), at(s, 0.22f, 0.80f), at(s, 0.80f, 0.20f), 0.03f * s)
    }

    fun flower(c: Color = Pink): Glyph = { s ->
        for (i in 0 until 5) {
            val a = -PI / 2 + i * 2 * PI / 5
            disc(s, 0.50f + 0.22f * cos(a).toFloat(), 0.50f + 0.22f * sin(a).toFloat(), 0.17f, c)
        }
        disc(s, 0.50f, 0.50f, 0.12f, Yellow)
    }

    fun grain(c: Color = Yellow): Glyph = { s ->
        drawLine(Brown, at(s, 0.50f, 0.92f), at(s, 0.50f, 0.20f), 0.04f * s, StrokeCap.Round)
        for (i in 0..3) {
            val y = 0.22f + i * 0.15f
            drawPath(leaf(s, 0.50f, y + 0.12f, 0.32f, y, 0.35f), c)
            drawPath(leaf(s, 0.50f, y + 0.12f, 0.68f, y, 0.35f), c)
        }
    }

    fun vegetable(c: Color = Orange): Glyph = { s ->
        drawPath(path(s, 0.30f to 0.30f, 0.70f to 0.30f, 0.50f to 0.94f), c)
        drawPath(leaf(s, 0.50f, 0.32f, 0.36f, 0.06f, 0.3f), Green)
        drawPath(leaf(s, 0.50f, 0.32f, 0.64f, 0.06f, 0.3f), Green)
    }

    fun field(c: Color = Brown): Glyph = { s ->
        drawRect(c, at(s, 0.10f, 0.20f), Size(0.80f * s, 0.60f * s))
        for (i in 0..3) drawLine(Yellow, at(s, 0.16f, 0.30f + i * 0.14f), at(s, 0.84f, 0.30f + i * 0.14f), 0.04f * s)
    }

    fun fence(c: Color = Brown): Glyph = { s ->
        for (x in listOf(0.18f, 0.50f, 0.82f)) drawLine(c, at(s, x, 0.20f), at(s, x, 0.86f), 0.08f * s, StrokeCap.Round)
        drawLine(c, at(s, 0.12f, 0.40f), at(s, 0.88f, 0.40f), 0.06f * s)
        drawLine(c, at(s, 0.12f, 0.66f), at(s, 0.88f, 0.66f), 0.06f * s)
    }

    fun cube(c: Color): Glyph = { s ->
        drawPath(path(s, 0.50f to 0.12f, 0.86f to 0.30f, 0.50f to 0.48f, 0.14f to 0.30f), c.copy(alpha = 1f))
        drawPath(path(s, 0.14f to 0.30f, 0.50f to 0.48f, 0.50f to 0.90f, 0.14f to 0.72f), c.copy(red = c.red * 0.75f, green = c.green * 0.75f, blue = c.blue * 0.75f))
        drawPath(path(s, 0.86f to 0.30f, 0.86f to 0.72f, 0.50f to 0.90f, 0.50f to 0.48f), c.copy(red = c.red * 0.55f, green = c.green * 0.55f, blue = c.blue * 0.55f))
    }

    fun resources(): Glyph = { s ->
        disc(s, 0.30f, 0.34f, 0.15f, Red)
        disc(s, 0.68f, 0.32f, 0.15f, Yellow)
        disc(s, 0.32f, 0.70f, 0.15f, Brown)
        disc(s, 0.70f, 0.70f, 0.15f, Grey)
    }

    fun gem(c: Color = Teal): Glyph = { s ->
        drawPath(path(s, 0.28f to 0.16f, 0.72f to 0.16f, 0.90f to 0.40f, 0.50f to 0.90f, 0.10f to 0.40f), c)
        drawLine(Color.White.copy(alpha = 0.5f), at(s, 0.10f, 0.40f), at(s, 0.90f, 0.40f), 0.04f * s)
    }

    fun scroll(c: Color = Cream): Glyph = { s ->
        card(s, 0.20f, 0.10f, 0.60f, 0.80f, c, Edge)
        for (i in 0..2) drawLine(Dark, at(s, 0.30f, 0.30f + i * 0.16f), at(s, 0.70f, 0.30f + i * 0.16f), 0.04f * s, StrokeCap.Round)
    }

    fun flag(c: Color = Red): Glyph = { s ->
        drawLine(Brown, at(s, 0.26f, 0.08f), at(s, 0.26f, 0.94f), 0.06f * s, StrokeCap.Round)
        drawPath(path(s, 0.29f to 0.12f, 0.86f to 0.12f, 0.70f to 0.30f, 0.86f to 0.48f, 0.29f to 0.48f), c)
    }

    fun tile(c: Color): Glyph = { s ->
        val p = Path().apply {
            for (i in 0 until 6) {
                val a = PI / 6 + i * PI / 3
                val x = (0.50f + 0.40f * cos(a).toFloat()) * s
                val y = (0.50f + 0.40f * sin(a).toFloat()) * s
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(p, c)
        drawPath(p, Edge, style = Stroke(0.04f * s))
    }

    fun square(c: Color): Glyph = { s ->
        drawRect(c, at(s, 0.16f, 0.16f), Size(0.68f * s, 0.68f * s))
        drawRect(Edge, at(s, 0.16f, 0.16f), Size(0.68f * s, 0.68f * s), style = Stroke(0.04f * s))
    }

    fun mountain(c: Color = Grey): Glyph = { s ->
        drawPath(path(s, 0.04f to 0.88f, 0.38f to 0.24f, 0.70f to 0.88f), c)
        drawPath(path(s, 0.36f to 0.88f, 0.66f to 0.40f, 0.96f to 0.88f), c.copy(red = c.red * 0.75f, green = c.green * 0.75f, blue = c.blue * 0.75f))
        drawPath(path(s, 0.38f to 0.24f, 0.46f to 0.40f, 0.30f to 0.40f), Color.White.copy(alpha = 0.9f))
    }

    fun water(c: Color = Blue): Glyph = { s ->
        for (i in 0..2) {
            val y = 0.30f + i * 0.20f
            drawArc(c, 180f, 180f, false, at(s, 0.10f, y), Size(0.40f * s, 0.20f * s), style = Stroke(0.07f * s, cap = StrokeCap.Round))
            drawArc(c, 0f, 180f, false, at(s, 0.50f, y), Size(0.40f * s, 0.20f * s), style = Stroke(0.07f * s, cap = StrokeCap.Round))
        }
    }

    fun sun(c: Color = Yellow): Glyph = { s ->
        for (i in 0 until 8) {
            val a = i * PI / 4
            drawLine(c, at(s, 0.50f + 0.28f * cos(a).toFloat(), 0.50f + 0.28f * sin(a).toFloat()),
                at(s, 0.50f + 0.44f * cos(a).toFloat(), 0.50f + 0.44f * sin(a).toFloat()), 0.06f * s, StrokeCap.Round)
        }
        disc(s, 0.50f, 0.50f, 0.22f, c)
    }

    fun planet(c: Color = Purple): Glyph = { s ->
        disc(s, 0.50f, 0.50f, 0.28f, c)
        drawOval(Edge, at(s, 0.08f, 0.40f), Size(0.84f * s, 0.20f * s), style = Stroke(0.05f * s))
    }

    fun rocket(c: Color = White): Glyph = { s ->
        drawOval(c, at(s, 0.36f, 0.08f), Size(0.28f * s, 0.66f * s))
        drawPath(path(s, 0.36f to 0.52f, 0.18f to 0.80f, 0.38f to 0.70f), Red)
        drawPath(path(s, 0.64f to 0.52f, 0.82f to 0.80f, 0.62f to 0.70f), Red)
        drawPath(path(s, 0.42f to 0.74f, 0.58f to 0.74f, 0.50f to 0.96f), Orange)
        disc(s, 0.50f, 0.34f, 0.07f, Blue)
    }

    fun gear(c: Color = Grey): Glyph = { s ->
        for (i in 0 until 8) {
            val a = i * PI / 4
            drawLine(c, at(s, 0.50f, 0.50f), at(s, 0.50f + 0.42f * cos(a).toFloat(), 0.50f + 0.42f * sin(a).toFloat()), 0.12f * s)
        }
        disc(s, 0.50f, 0.50f, 0.30f, c)
        disc(s, 0.50f, 0.50f, 0.12f, Dark)
    }

    fun flask(c: Color = Green): Glyph = { s ->
        drawRect(Edge, at(s, 0.42f, 0.08f), Size(0.16f * s, 0.28f * s))
        drawPath(path(s, 0.42f to 0.34f, 0.58f to 0.34f, 0.86f to 0.88f, 0.14f to 0.88f), c)
    }

    fun book(c: Color = Blue): Glyph = { s ->
        drawRect(c, at(s, 0.18f, 0.14f), Size(0.64f * s, 0.74f * s))
        drawRect(Cream, at(s, 0.24f, 0.78f), Size(0.58f * s, 0.10f * s))
        drawLine(Edge, at(s, 0.26f, 0.14f), at(s, 0.26f, 0.80f), 0.04f * s)
    }

    fun hourglass(c: Color = Gold): Glyph = { s ->
        drawRect(Brown, at(s, 0.20f, 0.08f), Size(0.60f * s, 0.08f * s))
        drawRect(Brown, at(s, 0.20f, 0.84f), Size(0.60f * s, 0.08f * s))
        drawPath(path(s, 0.28f to 0.16f, 0.72f to 0.16f, 0.50f to 0.50f), c)
        drawPath(path(s, 0.50f to 0.50f, 0.72f to 0.84f, 0.28f to 0.84f), c.copy(alpha = 0.6f))
    }

    fun target(c: Color = Red): Glyph = { s ->
        disc(s, 0.50f, 0.50f, 0.40f, c)
        disc(s, 0.50f, 0.50f, 0.27f, White)
        disc(s, 0.50f, 0.50f, 0.14f, c)
    }

    fun track(c: Color = Gold): Glyph = { s ->
        for (i in 0..4) drawRect(if (i == 3) c else Grey, at(s, 0.08f + i * 0.17f, 0.40f), Size(0.13f * s, 0.20f * s))
    }

    fun minus(c: Color = Red): Glyph = { s ->
        disc(s, 0.50f, 0.50f, 0.38f, c)
        drawLine(Color.White, at(s, 0.30f, 0.50f), at(s, 0.70f, 0.50f), 0.10f * s, StrokeCap.Round)
    }

    fun plus(c: Color = Green): Glyph = { s ->
        disc(s, 0.50f, 0.50f, 0.38f, c)
        drawLine(Color.White, at(s, 0.30f, 0.50f), at(s, 0.70f, 0.50f), 0.10f * s, StrokeCap.Round)
        drawLine(Color.White, at(s, 0.50f, 0.30f), at(s, 0.50f, 0.70f), 0.10f * s, StrokeCap.Round)
    }

    fun skull(c: Color = White): Glyph = { s ->
        disc(s, 0.50f, 0.42f, 0.30f, c)
        drawRect(c, at(s, 0.34f, 0.56f), Size(0.32f * s, 0.26f * s))
        disc(s, 0.39f, 0.42f, 0.08f, Dark)
        disc(s, 0.61f, 0.42f, 0.08f, Dark)
    }

    fun heart(c: Color = Red): Glyph = { s ->
        disc(s, 0.34f, 0.38f, 0.18f, c)
        disc(s, 0.66f, 0.38f, 0.18f, c)
        drawPath(path(s, 0.17f to 0.44f, 0.83f to 0.44f, 0.50f to 0.88f), c)
    }

    fun bolt(c: Color = Yellow): Glyph = { s ->
        drawPath(path(s, 0.58f to 0.06f, 0.24f to 0.54f, 0.48f to 0.54f, 0.40f to 0.94f, 0.76f to 0.42f, 0.52f to 0.42f), c)
    }

    fun eye(c: Color = Teal): Glyph = { s ->
        drawOval(White, at(s, 0.08f, 0.28f), Size(0.84f * s, 0.44f * s))
        disc(s, 0.50f, 0.50f, 0.18f, c)
        disc(s, 0.50f, 0.50f, 0.07f, Dark)
    }

    fun hand(c: Color = Cream): Glyph = { s ->
        card(s, 0.12f, 0.24f, 0.44f, 0.62f, Dark, Edge)
        card(s, 0.30f, 0.16f, 0.44f, 0.62f, c, Edge)
        card(s, 0.48f, 0.24f, 0.44f, 0.62f, c, Edge)
    }

    fun dice(c: Color = White): Glyph = { s ->
        card(s, 0.16f, 0.16f, 0.68f, 0.68f, c, Edge)
        for ((x, y) in listOf(0.32f to 0.32f, 0.68f to 0.32f, 0.50f to 0.50f, 0.32f to 0.68f, 0.68f to 0.68f)) disc(s, x, y, 0.06f, Dark)
    }

    fun egg(c: Color = Cream): Glyph = { s ->
        drawOval(c, at(s, 0.22f, 0.10f), Size(0.56f * s, 0.80f * s))
    }

    fun wine(c: Color = Purple): Glyph = { s ->
        drawPath(path(s, 0.26f to 0.10f, 0.74f to 0.10f, 0.68f to 0.46f, 0.32f to 0.46f), c)
        drawLine(Edge, at(s, 0.50f, 0.46f), at(s, 0.50f, 0.82f), 0.05f * s)
        drawLine(Edge, at(s, 0.32f, 0.86f), at(s, 0.68f, 0.86f), 0.06f * s, StrokeCap.Round)
    }

    fun barrel(c: Color = Brown): Glyph = { s ->
        drawOval(c, at(s, 0.18f, 0.10f), Size(0.64f * s, 0.80f * s))
        drawLine(Grey, at(s, 0.20f, 0.32f), at(s, 0.80f, 0.32f), 0.05f * s)
        drawLine(Grey, at(s, 0.20f, 0.68f), at(s, 0.80f, 0.68f), 0.05f * s)
    }

    fun potion(c: Color = Pink): Glyph = { s ->
        drawRect(Brown, at(s, 0.42f, 0.06f), Size(0.16f * s, 0.12f * s))
        drawRect(Edge, at(s, 0.40f, 0.16f), Size(0.20f * s, 0.18f * s))
        disc(s, 0.50f, 0.62f, 0.30f, c)
    }

    fun lightbulb(c: Color = Yellow): Glyph = { s ->
        disc(s, 0.50f, 0.40f, 0.28f, c)
        drawRect(Grey, at(s, 0.38f, 0.64f), Size(0.24f * s, 0.20f * s))
    }

    fun paw(c: Color = Brown): Glyph = { s ->
        drawOval(c, at(s, 0.30f, 0.46f), Size(0.40f * s, 0.36f * s))
        for ((x, y) in listOf(0.24f to 0.38f, 0.40f to 0.24f, 0.60f to 0.24f, 0.76f to 0.38f)) disc(s, x, y, 0.09f, c)
    }

    fun turtle(c: Color = Green): Glyph = { s ->
        drawArc(c, 180f, 180f, true, at(s, 0.18f, 0.28f), Size(0.64f * s, 0.56f * s))
        disc(s, 0.86f, 0.54f, 0.08f, c)
        drawLine(c, at(s, 0.28f, 0.56f), at(s, 0.24f, 0.70f), 0.07f * s, StrokeCap.Round)
        drawLine(c, at(s, 0.72f, 0.56f), at(s, 0.76f, 0.70f), 0.07f * s, StrokeCap.Round)
    }

    fun hexes(c: Color): Glyph = { s ->
        for ((x, y) in listOf(0.32f to 0.32f, 0.68f to 0.32f, 0.50f to 0.66f)) {
            val p = Path().apply {
                for (i in 0 until 6) {
                    val a = PI / 6 + i * PI / 3
                    val px = (x + 0.18f * cos(a).toFloat()) * s
                    val py = (y + 0.18f * sin(a).toFloat()) * s
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(p, c)
        }
    }

    fun ring(c: Color = Gold): Glyph = { s ->
        drawCircle(c, 0.30f * s, at(s, 0.50f, 0.56f), style = Stroke(0.10f * s))
        drawPath(path(s, 0.40f to 0.22f, 0.60f to 0.22f, 0.50f to 0.08f), Teal)
    }
}

/** A design from (row key to icon) pairs. */
fun design(emblem: Glyph, vararg glyphs: Pair<String, Glyph>) = ScoreSheetDesign(emblem = emblem, glyphs = mapOf(*glyphs))
