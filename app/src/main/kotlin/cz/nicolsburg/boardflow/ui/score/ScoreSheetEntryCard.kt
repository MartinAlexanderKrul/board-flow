package cz.nicolsburg.boardflow.ui.score

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.model.ScoreSheet

/** Log Play's way into a game's score sheet, in that sheet's own paper and ink. */
@Composable
fun ScoreSheetEntryCard(
    sheet: ScoreSheet,
    hasValues: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val design = remember(sheet.id) { scoreSheetDesign(sheet.id) }
    // The first two base rows show what the sheet counts: "Birds, bonus cards and more".
    val preview = remember(sheet.id) {
        val labels = sheet.rows.filter { it.module == null }.map { it.label }
        labels.take(2).mapIndexed { i, l -> if (i == 0) l else l.lowercase() }.joinToString(", ") +
            if (labels.size > 2) " and more" else ""
    }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = design.paper,
        border = BorderStroke(1.dp, design.line)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                sheet.rows.take(3).forEach { row ->
                    SheetGlyph(design.glyphs[row.id], Modifier.size(26.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (hasValues) "Continue score sheet" else "${sheet.gameName} score sheet",
                    color = design.ink,
                    fontFamily = design.titleFont,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    preview,
                    color = design.inkMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = design.accent
            )
        }
    }
}
