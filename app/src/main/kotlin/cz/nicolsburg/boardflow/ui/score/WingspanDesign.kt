package cz.nicolsburg.boardflow.ui.score

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cream card stock, teal and gold, after the Wingspan score pad. Drawn here, not copied from it.
private val Paper = Color(0xFFF4ECDB)
private val Band = Color(0xFFEBE0C9)
private val Ink = Color(0xFF2F2A22)
private val InkMuted = Color(0xFF7A6F5E)
private val Line = Color(0xFFD8CAAF)
private val Teal = Color(0xFF2C7275)
private val TealDeep = Color(0xFF1D5254)
private val Gold = Color(0xFFC8892B)
private val Plum = Color(0xFF5B4A6B)
private val Sky = Color(0xFF9FC7C9)
private val Rose = Color(0xFFD5577A)
private val Honey = Color(0xFFF2C14E)
private val Straw = Color(0xFFDCC69A)
private val Berry = Color(0xFFB8413B)

private val bird: Glyph = { s ->
    drawPath(path(s, 0.24f to 0.54f, 0.02f to 0.44f, 0.08f to 0.66f), Plum)
    drawOval(Teal, at(s, 0.16f, 0.36f), Size(0.58f * s, 0.38f * s))
    drawOval(TealDeep, at(s, 0.28f, 0.44f), Size(0.32f * s, 0.18f * s))
    disc(s, 0.70f, 0.36f, 0.15f, Teal)
    drawPath(path(s, 0.82f to 0.31f, 0.99f to 0.37f, 0.82f to 0.42f), Gold)
    disc(s, 0.73f, 0.33f, 0.03f, Paper)
    drawLine(Ink, at(s, 0.40f, 0.72f), at(s, 0.38f, 0.90f), 0.04f * s, StrokeCap.Round)
    drawLine(Ink, at(s, 0.52f, 0.72f), at(s, 0.54f, 0.90f), 0.04f * s, StrokeCap.Round)
}

private val bonusCard: Glyph = { s ->
    card(s, 0.20f, 0.08f, 0.60f, 0.84f, Straw, Ink)
    drawRect(Gold, at(s, 0.20f, 0.26f), Size(0.60f * s, 0.14f * s))
    drawPath(star(s, 0.50f, 0.64f, 0.16f, 0.07f), Teal)
}

private val goal: Glyph = { s ->
    drawPath(path(s, 0.34f to 0.56f, 0.24f to 0.94f, 0.40f to 0.84f, 0.48f to 0.98f, 0.50f to 0.60f), TealDeep)
    drawPath(path(s, 0.66f to 0.56f, 0.76f to 0.94f, 0.60f to 0.84f, 0.52f to 0.98f, 0.50f to 0.60f), TealDeep)
    disc(s, 0.50f, 0.40f, 0.30f, Teal)
    disc(s, 0.50f, 0.40f, 0.18f, Paper)
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
    drawPath(shape, Ink, style = Stroke(0.05f * s))
    disc(s, 0.38f, 0.40f, 0.07f, Color.White.copy(alpha = 0.7f))
}

private val foodToken: Glyph = { s ->
    disc(s, 0.50f, 0.50f, 0.42f, Straw, Ink)
    // A wheat ear: a stem and paired grains.
    drawLine(Ink, at(s, 0.50f, 0.80f), at(s, 0.50f, 0.24f), 0.04f * s, StrokeCap.Round)
    for (i in 0..2) {
        val y = 0.30f + i * 0.15f
        drawPath(leaf(s, 0.50f, y + 0.12f, 0.34f, y, 0.35f), Gold)
        drawPath(leaf(s, 0.50f, y + 0.12f, 0.66f, y, 0.35f), Gold)
    }
}

private val tucked: Glyph = { s ->
    card(s, 0.14f, 0.20f, 0.50f, 0.68f, Band, InkMuted)
    card(s, 0.36f, 0.10f, 0.50f, 0.68f, Paper, Ink)
    drawPath(path(s, 0.50f to 0.36f, 0.72f to 0.36f, 0.61f to 0.52f), Teal)
}

private val nectar: Glyph = { s ->
    for (i in 0 until 5) {
        val a = -Math.PI / 2 + i * 2 * Math.PI / 5
        disc(s, 0.50f + 0.22f * Math.cos(a).toFloat(), 0.50f + 0.22f * Math.sin(a).toFloat(), 0.18f, Rose)
    }
    disc(s, 0.50f, 0.50f, 0.12f, Honey)
}

private val habitatForest: Glyph = { s ->
    drawPath(path(s, 0.50f to 0.08f, 0.86f to 0.70f, 0.14f to 0.70f), Color(0xFF3F6B3A))
    drawRect(Color(0xFF6B4A2B), at(s, 0.44f, 0.70f), Size(0.12f * s, 0.22f * s))
}

private val habitatGrassland: Glyph = { s ->
    for ((x, h) in listOf(0.22f to 0.50f, 0.40f to 0.70f, 0.58f to 0.56f, 0.76f to 0.66f)) {
        drawPath(leaf(s, x, 0.92f, x + 0.04f, 0.92f - h, 0.12f), Color(0xFFB59A3A))
    }
}

private val habitatWetland: Glyph = { s ->
    for (i in 0..2) {
        val y = 0.34f + i * 0.20f
        drawArc(Color(0xFF3F7FA6), 180f, 180f, false, at(s, 0.10f, y), Size(0.40f * s, 0.20f * s), style = Stroke(0.07f * s, cap = StrokeCap.Round))
        drawArc(Color(0xFF3F7FA6), 0f, 180f, false, at(s, 0.50f, y), Size(0.40f * s, 0.20f * s), style = Stroke(0.07f * s, cap = StrokeCap.Round))
    }
}

private val unusedFood: Glyph = { s ->
    drawLine(Color(0xFF3F6B3A), at(s, 0.50f, 0.16f), at(s, 0.34f, 0.50f), 0.05f * s, StrokeCap.Round)
    drawLine(Color(0xFF3F6B3A), at(s, 0.50f, 0.16f), at(s, 0.68f, 0.52f), 0.05f * s, StrokeCap.Round)
    drawPath(leaf(s, 0.50f, 0.16f, 0.74f, 0.10f, 0.3f), Color(0xFF5E8F4E))
    disc(s, 0.32f, 0.66f, 0.20f, Berry, Ink)
    disc(s, 0.68f, 0.68f, 0.20f, Berry, Ink)
}

val WingspanDesign = ScoreSheetDesign(
    paper = Paper,
    paperBand = Band,
    ink = Ink,
    inkMuted = InkMuted,
    line = Line,
    cell = Color(0xFFFFFCF5),
    accent = Teal,
    onAccent = Paper,
    winner = Gold,
    onWinner = Color(0xFF1E1A12),
    titleFont = FontFamily.Serif,
    header = { WingspanHeader() },
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
        "tie.food" to unusedFood
    )
)

@Composable
private fun WingspanHeader() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Teal, TealDeep)))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FeatherLine(Modifier.width(36.dp).height(12.dp), flip = false)
                Spacer(Modifier.width(10.dp))
                Text(
                    "WINGSPAN",
                    color = Paper,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 5.sp,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.width(10.dp))
                FeatherLine(Modifier.width(36.dp).height(12.dp), flip = true)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                SheetGlyph(bird, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "FINAL SCORING",
                    color = Honey,
                    letterSpacing = 3.sp,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun FeatherLine(modifier: Modifier, flip: Boolean) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val y = h / 2
        drawLine(Honey, Offset(0f, y), Offset(w, y), 1.5f, StrokeCap.Round)
        val tip = if (flip) w else 0f
        val dir = if (flip) -1f else 1f
        for (i in 1..3) {
            val x = tip + dir * i * w / 5
            drawLine(Honey, Offset(x, y), Offset(x - dir * w / 8, y - h / 2.4f), 1.5f, StrokeCap.Round)
            drawLine(Honey, Offset(x, y), Offset(x - dir * w / 8, y + h / 2.4f), 1.5f, StrokeCap.Round)
        }
    }
}
