package cz.nicolsburg.boardflow.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

object BoardFlowMotion {
    const val PressDuration = 120
    const val QuickDuration = 140
    const val VisibilityDuration = 180
    const val DialogDuration = 200
    const val ShimmerDuration = 800
    const val ChartBaseDuration = 700
    const val ChartRowDuration = 600
    const val ChartRowStagger = 35
    const val PlayerChartRowDuration = 550
    const val PlayerChartRowStagger = 30
    const val PullRefreshScaleDuration = 180
    const val ContentResizeDuration = 180
    const val PressedScalePct = 0.97f
    const val DialogInitialScale = 0.92f
}

fun <T> boardFlowTween(durationMillis: Int = BoardFlowMotion.VisibilityDuration) =
    tween<T>(durationMillis = durationMillis, easing = FastOutSlowInEasing)

fun boardFlowFadeIn(durationMillis: Int = BoardFlowMotion.VisibilityDuration): EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing))

fun boardFlowFadeOut(durationMillis: Int = BoardFlowMotion.QuickDuration): ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing))

fun boardFlowPanelEnter(): EnterTransition =
    boardFlowFadeIn() + expandVertically(
        animationSpec = tween(BoardFlowMotion.VisibilityDuration, easing = FastOutSlowInEasing),
        expandFrom = Alignment.Top
    )

fun boardFlowPanelExit(): ExitTransition =
    boardFlowFadeOut() + shrinkVertically(
        animationSpec = tween(BoardFlowMotion.QuickDuration, easing = FastOutSlowInEasing),
        shrinkTowards = Alignment.Top
    )

@Composable
fun BoardFlowAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = boardFlowPanelEnter(),
    exit: ExitTransition = boardFlowPanelExit(),
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        content = content
    )
}

@Composable
fun rememberBoardFlowPressScale(
    isPressed: Boolean,
    label: String = "boardFlowPressScale"
): Float {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) BoardFlowMotion.PressedScalePct else 1f,
        animationSpec = tween(BoardFlowMotion.PressDuration, easing = FastOutSlowInEasing),
        label = label
    )
    return scale
}

@Composable
fun rememberBoardFlowShimmerAlpha(
    label: String = "boardFlowShimmerAlpha"
): Float {
    val transition = rememberInfiniteTransition(label = label)
    val alpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(BoardFlowMotion.ShimmerDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = label
    )
    return alpha
}

@Composable
fun BoardFlowPullRefreshContainer(
    isRefreshing: Boolean,
    isAtTop: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val refreshThresholdPx = with(density) { 72.dp.toPx() }
    val onRefreshState by rememberUpdatedState(onRefresh)
    val isAtTopState by rememberUpdatedState(isAtTop)
    val isRefreshingState by rememberUpdatedState(isRefreshing)
    var pullDistance by remember { mutableFloatStateOf(0f) }
    var refreshTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing, refreshThresholdPx) {
        if (isRefreshing) {
            pullDistance = refreshThresholdPx
        } else {
            pullDistance = 0f
            refreshTriggered = false
        }
    }

    val refreshConnection = remember(refreshThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y >= 0f || pullDistance <= 0f) {
                    return Offset.Zero
                }
                val consumed = minOf(-available.y, pullDistance)
                pullDistance -= consumed
                return Offset(0f, -consumed)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (
                    source != NestedScrollSource.UserInput ||
                    available.y <= 0f ||
                    !isAtTopState ||
                    isRefreshingState ||
                    refreshTriggered
                ) {
                    return Offset.Zero
                }

                pullDistance = (pullDistance + available.y * 0.5f).coerceAtMost(refreshThresholdPx)
                if (pullDistance >= refreshThresholdPx) {
                    refreshTriggered = true
                    onRefreshState()
                }
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!isRefreshingState) {
                    pullDistance = 0f
                    refreshTriggered = false
                }
                return Velocity.Zero
            }
        }
    }

    Box(modifier = modifier.nestedScroll(refreshConnection)) {
        content()
        if (isRefreshing || pullDistance > 0f) {
            val indicatorScale = if (isRefreshing) {
                1f
            } else {
                (pullDistance / refreshThresholdPx).coerceIn(0.35f, 1f)
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .scale(indicatorScale)
                    .size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}
