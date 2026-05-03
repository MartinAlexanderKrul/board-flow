package cz.nicolsburg.boardflow.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

fun Modifier.clickableRow(
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: () -> Unit,
): Modifier = this
    .clip(shape)
    .clickable(onClick = onClick)

fun Modifier.swipeToNavigateTabs(
    tabCount: Int,
    selectedIndex: Int,
    onNavigate: (Int) -> Unit
): Modifier = pointerInput(tabCount, selectedIndex) {
    var totalDrag = 0f
    detectHorizontalDragGestures(
        onDragStart = { totalDrag = 0f },
        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
        onDragEnd = {
            if (totalDrag > 100f && selectedIndex > 0) onNavigate(selectedIndex - 1)
            else if (totalDrag < -100f && selectedIndex < tabCount - 1) onNavigate(selectedIndex + 1)
            totalDrag = 0f
        },
        onDragCancel = { totalDrag = 0f }
    )
}
