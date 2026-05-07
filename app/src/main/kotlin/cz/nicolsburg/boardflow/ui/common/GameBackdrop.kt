package cz.nicolsburg.boardflow.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Crisp full-bleed game artwork pinned at the top of a screen or dialog.
 * Renders behind content — place content in a sibling Box over this.
 * Returns early (renders nothing) when imageUrl is null or blank.
 */
@Composable
fun GameBackdrop(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
) {
    if (imageUrl.isNullOrBlank()) return
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Cinematic gradient: crisp art at top, progressively darker toward content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f    to Color.Transparent,
                            0.28f to Color.Black.copy(alpha = 0.22f),
                            0.62f to Color.Black.copy(alpha = 0.55f),
                            0.86f to Color.Black.copy(alpha = 0.72f),
                            1f    to surfaceColor
                        )
                    )
                )
        )
    }
}
