package cz.nicolsburg.boardflow.ui.theme

import androidx.compose.ui.unit.dp

// A 4dp grid. Use these instead of raw dp values for padding and gaps.
object Spacing {
    val xs  = 4.dp   // label-to-value, tight icon gap
    val sm  = 8.dp   // between related items, list item spacing
    val md  = 12.dp  // between sections within a card
    val lg  = 16.dp  // card internal padding, screen horizontal padding
    val xl  = 24.dp  // between major sections
    val xxl = 32.dp  // before primary CTA, modal bottom padding
}

// Sizes that are about hit areas and icons rather than rhythm.
object Dimens {
    val MinTouchTarget = 48.dp  // every tappable thing is at least this in both axes
    val ButtonHeight = 48.dp
    val FieldHeight = 48.dp
    val IconSmall = 16.dp       // inline with small text
    val Icon = 20.dp            // default inside buttons and rows
    val IconLarge = 24.dp       // standalone icon buttons, navigation
    val Hairline = 1.dp         // borders and dividers
}
