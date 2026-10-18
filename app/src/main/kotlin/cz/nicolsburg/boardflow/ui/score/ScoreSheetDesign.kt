package cz.nicolsburg.boardflow.ui.score

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily

/**
 * The look of one game's score sheet: paper and ink like its printed pad, a header of its own and
 * a drawn icon per row. The grid and its behaviour are shared (`ScoreSheetScreen`); everything a
 * player sees as "this game's pad" comes from here.
 */
@Immutable
class ScoreSheetDesign(
    val paper: Color,
    /** Every other row, like the tinted bands on a printed pad. */
    val paperBand: Color,
    val ink: Color,
    val inkMuted: Color,
    val line: Color,
    /** Fill of an input cell. */
    val cell: Color,
    val accent: Color,
    val onAccent: Color,
    /** The winning column's total and crown. */
    val winner: Color,
    val onWinner: Color,
    val titleFont: FontFamily = FontFamily.Serif,
    val header: @Composable () -> Unit,
    /** Drawings keyed by row id, MajorityRow group key or tiebreaker input key. */
    val glyphs: Map<String, Glyph> = emptyMap()
)

/** A row icon drawn in a square of side `s`. */
typealias Glyph = DrawScope.(s: Float) -> Unit

@Composable
fun SheetGlyph(glyph: Glyph?, modifier: Modifier = Modifier) {
    if (glyph == null) return
    Canvas(modifier) { glyph(size.minDimension) }
}

/** The design for a sheet id; every sheet in `ScoreSheets` has one. */
fun scoreSheetDesign(sheetId: String): ScoreSheetDesign = when (sheetId) {
    "wingspan" -> WingspanDesign
    "everdell" -> EverdellDesign
    else -> error("No score sheet design for $sheetId")
}
