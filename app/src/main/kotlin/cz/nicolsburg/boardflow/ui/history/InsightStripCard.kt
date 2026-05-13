package cz.nicolsburg.boardflow.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun PlayInsightStrip(
    text: String,
    modifier: Modifier = Modifier
) {
    InsightStripCard(
        text = text,
        icon = Icons.Default.Star,
        modifier = modifier
    )
}

@Composable
fun ContextualInsightStrip(
    insight: ContextualInsight,
    ambient: Boolean = false,
    modifier: Modifier = Modifier
) {
    val icon = when (insight.type) {
        InsightType.RecentActivity -> Icons.AutoMirrored.Filled.TrendingUp
        InsightType.Rivalry -> Icons.Default.EmojiEvents
        InsightType.Session -> Icons.Default.Schedule
        InsightType.Dormant -> Icons.Default.History
        InsightType.Milestone -> Icons.Default.Star
    }
    InsightStripCard(
        text = insight.text,
        icon = icon,
        ambient = ambient,
        modifier = modifier
    )
}

@Composable
private fun InsightStripCard(
    text: String,
    icon: ImageVector,
    ambient: Boolean = false,
    modifier: Modifier = Modifier
) {
    val background = if (ambient) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
    }
    val border = if (ambient) {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
    } else {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = background,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (ambient) 5.dp else 7.dp),
            horizontalArrangement = Arrangement.spacedBy(if (ambient) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .size(if (ambient) 12.dp else 14.dp)
                    .alpha(if (ambient) 0.62f else 0.82f),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (ambient) 0.74f else 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
