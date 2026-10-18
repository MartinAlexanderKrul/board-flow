package cz.nicolsburg.boardflow.ui.score

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Parchment, forest green and berry red, after the Everdell score pad. Drawn here, not copied from it.
private val Parchment = Color(0xFFF1E6CC)
private val Band = Color(0xFFE7D7B4)
private val Ink = Color(0xFF2E2A1E)
private val InkMuted = Color(0xFF75684E)
private val Line = Color(0xFFCDB98F)
private val Forest = Color(0xFF3F5E36)
private val ForestDeep = Color(0xFF2A4224)
private val Moss = Color(0xFF7E9A5A)
private val Berry = Color(0xFFA33B3B)
private val Bark = Color(0xFF6B4A2B)
private val Gold = Color(0xFFC99A3B)
private val GoldDeep = Color(0xFF8A6420)
private val Purple = Color(0xFF7B4E9A)
private val Stone = Color(0xFF9C8F7A)
private val Pearl = Color(0xFFE8EEF2)
private val PearlEdge = Color(0xFF8FA6B5)
private val Teal = Color(0xFF3D8C8C)
private val Resin = Color(0xFFE39B2D)

private val cards: Glyph = { s ->
    card(s, 0.20f, 0.08f, 0.60f, 0.84f, Color(0xFFF8F1DE), Ink)
    drawRect(Forest, at(s, 0.20f, 0.20f), Size(0.60f * s, 0.18f * s))
    disc(s, 0.30f, 0.16f, 0.12f, Gold, GoldDeep)
    drawPath(leaf(s, 0.36f, 0.76f, 0.66f, 0.52f, 0.3f), Moss)
}

private val pointToken: Glyph = { s ->
    disc(s, 0.50f, 0.50f, 0.42f, Gold, GoldDeep)
    drawCircle(GoldDeep, 0.30f * s, at(s, 0.50f, 0.50f), style = Stroke(0.04f * s))
    drawPath(leaf(s, 0.34f, 0.66f, 0.66f, 0.34f, 0.32f), Forest)
}

private val prosperity: Glyph = { s ->
    card(s, 0.20f, 0.08f, 0.60f, 0.84f, Purple, Ink)
    drawPath(star(s, 0.50f, 0.50f, 0.22f, 0.09f), Gold)
}

private val journey: Glyph = { s ->
    drawPath(path(s, 0.04f to 0.88f, 0.38f to 0.24f, 0.70f to 0.88f), Stone)
    drawPath(path(s, 0.36f to 0.88f, 0.66f to 0.40f, 0.96f to 0.88f), Color(0xFF6F6553))
    drawPath(path(s, 0.38f to 0.24f, 0.46f to 0.40f, 0.30f to 0.40f), Color.White.copy(alpha = 0.85f))
    drawLine(
        Bark, at(s, 0.10f, 0.96f), at(s, 0.62f, 0.56f), 0.05f * s, StrokeCap.Round,
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
    drawRect(Ink, at(s, 0.44f, 0.62f), Size(0.12f * s, 0.30f * s))
    drawRect(Ink, at(s, 0.45f, 0.44f), Size(0.10f * s, 0.10f * s))
}

private val adornment: Glyph = { s ->
    val gem = path(s, 0.50f to 0.10f, 0.86f to 0.42f, 0.50f to 0.92f, 0.14f to 0.42f)
    drawPath(gem, Teal)
    drawPath(gem, Ink, style = Stroke(0.05f * s))
    drawLine(Color.White.copy(alpha = 0.6f), at(s, 0.14f, 0.42f), at(s, 0.86f, 0.42f), 0.04f * s)
}

private val expansion: Glyph = { s ->
    disc(s, 0.50f, 0.50f, 0.40f, Band, Bark)
    drawLine(Bark, at(s, 0.50f, 0.30f), at(s, 0.50f, 0.70f), 0.08f * s, StrokeCap.Round)
    drawLine(Bark, at(s, 0.30f, 0.50f), at(s, 0.70f, 0.50f), 0.08f * s, StrokeCap.Round)
}

private val resources: Glyph = { s ->
    drawLine(Bark, at(s, 0.08f, 0.30f), at(s, 0.60f, 0.16f), 0.09f * s, StrokeCap.Round)
    drawOval(Stone, at(s, 0.08f, 0.56f), Size(0.40f * s, 0.30f * s))
    disc(s, 0.70f, 0.66f, 0.18f, Purple, Ink)
    disc(s, 0.70f, 0.34f, 0.12f, Resin)
}

val EverdellDesign = ScoreSheetDesign(
    paper = Parchment,
    paperBand = Band,
    ink = Ink,
    inkMuted = InkMuted,
    line = Line,
    cell = Color(0xFFFBF6EA),
    accent = Forest,
    onAccent = Parchment,
    winner = Berry,
    onWinner = Parchment,
    titleFont = FontFamily.Serif,
    header = { EverdellHeader() },
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

@Composable
private fun EverdellHeader() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Forest, ForestDeep)))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LeafSprig(Modifier.size(width = 40.dp, height = 24.dp), flip = false)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Everdell",
                    color = Parchment,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.width(8.dp))
                LeafSprig(Modifier.size(width = 40.dp, height = 24.dp), flip = true)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "SCORE SHEET",
                color = Gold,
                letterSpacing = 3.sp,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/** A twig with leaves and a berry, pointing towards the title. */
@Composable
private fun LeafSprig(modifier: Modifier, flip: Boolean) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        fun x(f: Float) = if (flip) w * (1 - f) else w * f
        drawLine(Gold, Offset(x(0.02f), h * 0.62f), Offset(x(0.96f), h * 0.50f), 2f, StrokeCap.Round)
        for ((f, up) in listOf(0.20f to true, 0.42f to false, 0.62f to true, 0.80f to false)) {
            val path = Path().apply {
                val bx = x(f)
                val by = h * (0.60f - f * 0.1f)
                val tx = x(f + 0.14f)
                val ty = if (up) h * 0.10f else h * 0.98f
                moveTo(bx, by)
                quadraticBezierTo(x(f + 0.02f), (by + ty) / 2, tx, ty)
                quadraticBezierTo(x(f + 0.16f), (by + ty) / 2, bx, by)
                close()
            }
            drawPath(path, Moss)
        }
        drawCircle(Berry, h * 0.14f, Offset(x(0.96f), h * 0.50f))
    }
}
