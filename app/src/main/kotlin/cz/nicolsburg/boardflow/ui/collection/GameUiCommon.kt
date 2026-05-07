package cz.nicolsburg.boardflow.ui.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.model.GameItem
import cz.nicolsburg.boardflow.ui.common.withTabularNumbers

data class SectionStat(
    val label: String,
    val value: String
)

data class HeaderChip(
    val label: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun InlineStat(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    large: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = androidx.compose.ui.Modifier.size(if (large) 18.dp else 14.dp),
            tint = tint
        )
        Text(
            text = label,
            style = if (large) {
                MaterialTheme.typography.titleMedium.withTabularNumbers()
            } else {
                MaterialTheme.typography.labelMedium.withTabularNumbers()
            },
            fontWeight = if (large) FontWeight.SemiBold else null,
            color = tint
        )
    }
}

fun playerLabel(game: GameItem): String? {
    return when {
        game.minPlayers != null && game.maxPlayers != null && game.minPlayers != game.maxPlayers ->
            "${game.minPlayers}-${game.maxPlayers} players"

        game.minPlayers != null ->
            "${game.minPlayers} players"

        else -> null
    }
}

fun formatDecimal(value: Double): String = String.format("%.1f", value)

fun gameWeightLabel(weight: Double): String = when {
    weight < 1.5 -> "Light"
    weight < 2.0 -> "Casual"
    weight < 2.5 -> "Medium-Light"
    weight < 3.0 -> "Medium"
    weight < 3.5 -> "Medium-Heavy"
    weight < 4.0 -> "Heavy"
    else         -> "Expert"
}

fun overviewStats(game: GameItem): List<SectionStat> {
    val stats = mutableListOf<SectionStat>()
    game.yearPublished?.toString()?.let { stats += SectionStat("Year", it) }
    playerLabel(game)?.let { stats += SectionStat("Players", it) }
    compactPlayTime(game)?.let { stats += SectionStat("Play time", it) }
    game.recommendedAge?.takeIf { it.isNotBlank() }?.let { stats += SectionStat("Age", it) }
    return stats
}

fun ratingStats(game: GameItem): List<SectionStat> {
    return listOfNotNull(
        game.rating?.let { SectionStat("BGG rating", formatDecimal(it)) },
        game.bayesAverage?.let { SectionStat("Bayes rating", formatDecimal(it)) },
        game.weight?.let { SectionStat("Weight", gameWeightLabel(it)) },
    )
}

fun playerPreferenceStats(game: GameItem): List<SectionStat> {
    return listOfNotNull(
        game.bestPlayers?.takeIf { it.isNotBlank() }?.let { SectionStat("Best for", it) },
        game.recommendedPlayers?.takeIf { it.isNotBlank() }?.let { SectionStat("Recommended for", it) }
    )
}

fun customDetailRows(game: GameItem): List<SectionStat> {
    val handledKeys = setOf(
        "objectid",
        "collid",
        "objectname",
        "game",
        "objecttype",
        "originalname",
        "yearpublished",
        "year",
        "rank",
        "average",
        "score",
        "communityrating",
        "baverage",
        "avgweight",
        "weight",
        "minplayers",
        "maxplayers",
        "playingtime",
        "minplaytime",
        "maxplaytime",
        "numowned",
        "numplays",
        "thumbnail",
        "shareurl",
        "share_url",
        "share url",
        "qrimage",
        "qr_image",
        "qr image",
        "drive",
        "language",
        "languagedependence",
        "bgglanguagedependence",
        "bggbestplayers",
        "bggrecplayers",
        "bggrecagerange",
        "bggurl",
        "own",
        "wishlist",
        "numowned",
        "price",
        "origprice",
        "orig price",
        "sleeved",
        "sleeves",
        "sleevejson"
    )

    return game.spreadsheetValues.entries
        .filter { (key, value) -> value.isNotBlank() && key !in handledKeys }
        .sortedBy { it.key }
        .map { (key, value) -> SectionStat(formatSourceKey(key), value) }
}

fun bggSleevesUrl(game: GameItem): String? {
    val baseUrl = game.bggUrl?.substringBefore('?')?.trimEnd('/')
    if (!baseUrl.isNullOrBlank()) {
        return if (baseUrl.endsWith("/sleeves", ignoreCase = true)) baseUrl else "$baseUrl/sleeves"
    }

    val objectId = game.objectId.takeIf { it.isNotBlank() } ?: return null
    val objectType = game.spreadsheetValues["objecttype"] ?: game.bggValues["objecttype"]

    val route = when (objectType?.trim()?.lowercase()) {
        "boardgameexpansion" -> "boardgameexpansion"
        "boardgameaccessory" -> "boardgameaccessory"
        else -> "boardgame"
    }

    return "https://boardgamegeek.com/$route/$objectId/sleeves"
}

fun formatSourceKey(key: String): String {
    if (key.equals("origprice", ignoreCase = true) || key.equals("orig price", ignoreCase = true)) {
        return "Price"
    }

    return key
        .replace('_', ' ')
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .trim()
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.lowercase().replaceFirstChar { it.titlecase() }
        }
}

fun compactPlayTime(game: GameItem): String? {
    val playTime = game.playingTime
    val minPlayTime = game.minPlayTime
    val maxPlayTime = game.maxPlayTime

    return when {
        playTime != null -> "${playTime} min"
        minPlayTime != null && maxPlayTime != null && minPlayTime != maxPlayTime -> "$minPlayTime-$maxPlayTime min"
        minPlayTime != null -> "${minPlayTime} min"
        maxPlayTime != null -> "${maxPlayTime} min"
        else -> null
    }
}

internal enum class SheetSleeveStatus {
    SLEEVED,
    TO_SLEEVE,
    UNSLEEVED,
    UNKNOWN
}

internal fun sheetSleeveStatus(game: GameItem): SheetSleeveStatus {
    val value = game.spreadsheetValues.entries
        .firstOrNull { (key, _) -> key.equals("sleeved", ignoreCase = true) }
        ?.value
        ?.trim()

    return when (value?.lowercase()) {
        "1", "1.0", "true", "yes", "y" -> SheetSleeveStatus.SLEEVED
        "!", "to sleeve", "tosleeve" -> SheetSleeveStatus.TO_SLEEVE
        "0", "0.0", "false", "no", "n" -> SheetSleeveStatus.UNSLEEVED
        else -> SheetSleeveStatus.UNKNOWN
    }
}

fun isSleeved(game: GameItem): Boolean =
    sheetSleeveStatus(game) == SheetSleeveStatus.SLEEVED

fun headerStatusChips(
    game: GameItem,
    primary: Color,
    secondary: Color
): List<HeaderChip> {
    return buildList {
        when (sheetSleeveStatus(game)) {
            SheetSleeveStatus.SLEEVED -> add(HeaderChip("Sleeved", Icons.Default.Check, primary))
            SheetSleeveStatus.TO_SLEEVE -> add(HeaderChip("To sleeve", Icons.Default.WarningAmber, secondary))
            SheetSleeveStatus.UNSLEEVED,
            SheetSleeveStatus.UNKNOWN -> Unit
        }
        if (game.isOwned) {
            add(HeaderChip("Owned", Icons.Default.Inventory2, primary))
        }
        if (game.isWishlisted) {
            add(HeaderChip("Wishlist", Icons.Default.Bookmark, secondary))
        }
    }
}
