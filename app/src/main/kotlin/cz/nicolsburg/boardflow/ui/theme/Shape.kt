package cz.nicolsburg.boardflow.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Three radii and a pill, nothing else:
//   Control (12)  fields, chips, small buttons, list rows
//   Card    (16)  cards, banners, primary buttons
//   Sheet   (24)  dialogs and bottom sheets
//   Pill          avatars, badges, status pills
// Do not write RoundedCornerShape(n.dp) in a screen; use one of these.
object BoardFlowShape {
    val Control = RoundedCornerShape(12.dp)
    val Card = RoundedCornerShape(16.dp)
    val Sheet = RoundedCornerShape(24.dp)
    val SheetTop = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val Pill = CircleShape
}

// Material components read these, so default buttons, menus and dialogs follow the
// same radii without per-call overrides.
val BoardFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = BoardFlowShape.Control,
    medium = BoardFlowShape.Card,
    large = BoardFlowShape.Card,
    extraLarge = BoardFlowShape.Sheet,
)
