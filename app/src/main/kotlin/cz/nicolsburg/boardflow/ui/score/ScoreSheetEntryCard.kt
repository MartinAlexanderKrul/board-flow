package cz.nicolsburg.boardflow.ui.score

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.ui.common.BoardFlowCard
import cz.nicolsburg.boardflow.ui.theme.Spacing

/** Log Play's way into a game's score sheet: a plain card with three of the sheet's row icons. */
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
    BoardFlowCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                sheet.rows.take(3).forEach { row ->
                    SheetGlyph(design.glyphs[row.id], Modifier.size(24.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (hasValues) "Continue score sheet" else "Score sheet",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
