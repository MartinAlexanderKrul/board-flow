package cz.nicolsburg.boardflow.ui.score

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Small helpers for row icons: coordinates are fractions of the icon's side `s`.

internal fun at(s: Float, x: Float, y: Float) = Offset(x * s, y * s)

internal fun DrawScope.card(s: Float, x: Float, y: Float, w: Float, h: Float, fill: Color, outline: Color) {
    drawRoundRect(fill, at(s, x, y), Size(w * s, h * s), CornerRadius(0.08f * s))
    drawRoundRect(outline, at(s, x, y), Size(w * s, h * s), CornerRadius(0.08f * s), style = Stroke(0.05f * s))
}

internal fun DrawScope.disc(s: Float, x: Float, y: Float, r: Float, fill: Color, outline: Color? = null) {
    drawCircle(fill, r * s, at(s, x, y))
    if (outline != null) drawCircle(outline, r * s, at(s, x, y), style = Stroke(0.05f * s))
}

internal fun path(s: Float, vararg points: Pair<Float, Float>): Path = Path().apply {
    points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * s, y * s) else lineTo(x * s, y * s) }
    close()
}

/** A five-pointed star centred on (x, y). */
internal fun star(s: Float, x: Float, y: Float, outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = -PI / 2 + i * PI / 5
        val px = (x + r * cos(a).toFloat()) * s
        val py = (y + r * sin(a).toFloat()) * s
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

/** A leaf from (x1, y1) to (x2, y2), bulging by `width` either side. */
internal fun leaf(s: Float, x1: Float, y1: Float, x2: Float, y2: Float, width: Float): Path = Path().apply {
    val mx = (x1 + x2) / 2
    val my = (y1 + y2) / 2
    val dx = y2 - y1
    val dy = -(x2 - x1)
    moveTo(x1 * s, y1 * s)
    quadraticBezierTo((mx + dx * width) * s, (my + dy * width) * s, x2 * s, y2 * s)
    quadraticBezierTo((mx - dx * width) * s, (my - dy * width) * s, x1 * s, y1 * s)
    close()
}
