package cz.nicolsburg.boardflow.ui.score

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.SheetOrigin

/**
 * What makes one game's score sheet its own: a drawn icon per row and an emblem for the header,
 * in the colours of the game. Everything else (surfaces, type, the winner, buttons) is the app's,
 * from the shared grid in `ScoreSheetScreen`, so the sheet sits in BoardFlow like any screen.
 */
@Immutable
class ScoreSheetDesign(
    /** Drawn next to the game name in the header (a bird, a sprig of leaves). */
    val emblem: Glyph,
    /** Drawings keyed by row id, `<row>.<group>` for a MajorityRow group, or `tie.<id>`. */
    val glyphs: Map<String, Glyph> = emptyMap()
)

/** A row icon drawn in a square of side `s`, in colours that read on the app's dark surfaces. */
typealias Glyph = DrawScope.(s: Float) -> Unit

@Composable
fun SheetGlyph(glyph: Glyph?, modifier: Modifier = Modifier) {
    if (glyph == null) return
    Canvas(modifier) { glyph(size.minDimension) }
}

/**
 * A sheet's design: its hand-made one when it has one (every built-in sheet does), otherwise icons
 * picked by keyword (the user's own sheets).
 */
fun scoreSheetDesign(sheet: ScoreSheet): ScoreSheetDesign =
    if (sheet.origin == SheetOrigin.BUILT_IN) builtInDesign(sheet.id) ?: keywordDesign(sheet) else keywordDesign(sheet)

/** The hand-made design for a built-in sheet id, or null. */
internal fun builtInDesign(sheetId: String): ScoreSheetDesign? = when (sheetId) {
    "wingspan" -> WingspanDesign
    "everdell" -> EverdellDesign
    "farshore" -> FarshoreDesign
    "everdell-duo" -> EverdellDuoDesign
    "silverfrost" -> SilverfrostDesign
    "emerland" -> EmerlandDesign
    "finspan" -> FinspanDesign
    "wyrmspan" -> WyrmspanDesign
    else -> guideDesigns[sheetId]
}
