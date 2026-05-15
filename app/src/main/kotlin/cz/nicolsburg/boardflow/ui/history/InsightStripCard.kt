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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.model.InsightRarity

@Composable
fun PlayInsightStrip(
    insight: PlayInsight,
    modifier: Modifier = Modifier
) {
    InsightStripCard(
        text     = insight.text,
        icon     = Icons.Default.Star,
        rarity   = insight.rarity,
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
        InsightType.Rivalry        -> Icons.Default.EmojiEvents
        InsightType.Session        -> Icons.Default.Schedule
        InsightType.Dormant        -> Icons.Default.History
        InsightType.Milestone      -> Icons.Default.Star
    }
    InsightStripCard(
        text     = insight.text,
        icon     = icon,
        rarity   = insight.rarity,
        ambient  = ambient,
        modifier = modifier
    )
}

@Composable
private fun InsightStripCard(
    text: String,
    icon: ImageVector,
    rarity: InsightRarity = InsightRarity.COMMON,
    ambient: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Background opacity scales gently with rarity
    val bgAlpha = when (rarity) {
        InsightRarity.COMMON    -> if (ambient) 0.12f else 0.42f
        InsightRarity.NOTABLE   -> if (ambient) 0.16f else 0.48f
        InsightRarity.RARE,
        InsightRarity.EPIC,
        InsightRarity.LEGENDARY -> if (ambient) 0.20f else 0.55f
    }

    // Border becomes more visible and shifts colour for Rare+
    val borderColor = when (rarity) {
        InsightRarity.LEGENDARY -> Color(0xFFF0A500)             // amber
        InsightRarity.EPIC      -> MaterialTheme.colorScheme.tertiary
        InsightRarity.RARE      -> MaterialTheme.colorScheme.primary
        else                    -> MaterialTheme.colorScheme.primary
    }
    val borderAlpha = when (rarity) {
        InsightRarity.COMMON  -> if (ambient) 0.05f else 0.08f
        InsightRarity.NOTABLE -> if (ambient) 0.10f else 0.14f
        InsightRarity.RARE    -> 0.28f
        InsightRarity.EPIC    -> 0.38f
        InsightRarity.LEGENDARY -> 0.50f
    }

    // Icon tint: more opaque and colour-shifted for higher tiers
    val iconTint = when (rarity) {
        InsightRarity.LEGENDARY -> Color(0xFFF0A500)
        InsightRarity.EPIC      -> MaterialTheme.colorScheme.tertiary
        InsightRarity.RARE      -> MaterialTheme.colorScheme.primary
        InsightRarity.NOTABLE   -> MaterialTheme.colorScheme.primary
        InsightRarity.COMMON    -> MaterialTheme.colorScheme.primary
    }
    val iconAlpha = when (rarity) {
        InsightRarity.COMMON  -> if (ambient) 0.62f else 0.72f
        InsightRarity.NOTABLE -> 0.85f
        else                  -> 1.0f
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.medium,
        color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = bgAlpha),
        border   = BorderStroke(0.5.dp, borderColor.copy(alpha = borderAlpha))
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 12.dp, vertical = if (ambient) 5.dp else 7.dp),
            horizontalArrangement = Arrangement.spacedBy(if (ambient) 6.dp else 8.dp),
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .size(if (ambient) 12.dp else 14.dp)
                    .alpha(iconAlpha),
                tint = iconTint
            )
            Text(
                text      = text,
                style     = MaterialTheme.typography.labelMedium,
                color     = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                    alpha = if (ambient) 0.74f else 0.90f
                ),
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis
            )
        }
    }
}
