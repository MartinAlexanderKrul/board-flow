package cz.nicolsburg.boardflow.ui.theme

import androidx.compose.ui.graphics.Color

/** Every colour a player can have, in one place. */
object PlayerColors {
    /** Colours a play can record for a player ("red", "blue"), as BGG writes them. */
    val named: Map<String, Color> = mapOf(
        "red" to Color(0xFFE53935), "blue" to Color(0xFF1E88E5), "green" to Color(0xFF43A047),
        "yellow" to Color(0xFFFDD835), "orange" to Color(0xFFFB8C00), "purple" to Color(0xFF8E24AA),
        "white" to Color(0xFFF5F5F5), "black" to Color(0xFF212121), "pink" to Color(0xFFE91E63),
        "brown" to Color(0xFF6D4C41), "gray" to Color(0xFF757575), "grey" to Color(0xFF757575),
        "cyan" to Color(0xFF00ACC1), "teal" to Color(0xFF00897B), "lime" to Color(0xFF7CB342)
    )

    /** Picked from the name when a player has no colour of their own. */
    val automatic: List<Color> = listOf(
        Color(0xFF7C4DFF), Color(0xFF448AFF), Color(0xFF00ACC1),
        Color(0xFF43A047), Color(0xFFFF8F00), Color(0xFFE91E63),
        Color(0xFF795548), Color(0xFF546E7A)
    )

    /** Initials and ticks on light fills (yellow, white), where white would disappear. */
    val DarkInk = Color(0xFF1C1C1E)

    /** A recorded colour name or "#RRGGBB", or null when it is neither (then it is a team name). */
    fun resolve(colorName: String): Color? = named[colorName.lowercase().trim()]
        ?: runCatching { Color(android.graphics.Color.parseColor(colorName.trim())) }.getOrNull()
}
