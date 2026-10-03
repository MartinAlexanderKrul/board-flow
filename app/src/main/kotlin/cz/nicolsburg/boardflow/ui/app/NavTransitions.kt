package cz.nicolsburg.boardflow.ui.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import cz.nicolsburg.boardflow.core.navigation.AppRoutes

/**
 * Screen-to-screen motion (Material motion patterns):
 * - between bottom bar destinations: fade through (old fades out fast, new fades in and settles
 *   from 96%), since they are siblings with no direction;
 * - into and out of pushed screens (Log play form, Scan, Quick Setup, imports): a short shared
 *   X-axis slide with a fade, forward to the left and back to the right.
 */
internal object NavTransitions {
    private val TopLevel = setOf(AppRoutes.NEW_PLAY, AppRoutes.HISTORY, AppRoutes.COLLECTION, AppRoutes.SETTINGS)

    private const val OutMs = 90
    private const val InMs = 210
    private const val SlideMs = 300

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTopLevel(): Boolean =
        initialState.destination.route in TopLevel && targetState.destination.route in TopLevel

    private val fadeThroughIn: EnterTransition =
        fadeIn(tween(InMs, delayMillis = OutMs, easing = LinearOutSlowInEasing)) +
            scaleIn(tween(InMs, delayMillis = OutMs, easing = LinearOutSlowInEasing), initialScale = 0.96f)
    private val fadeThroughOut: ExitTransition =
        fadeOut(tween(OutMs, easing = FastOutLinearInEasing))

    // Slide distance: an eighth of the width reads as movement without dragging the whole page.
    private fun slideIn(forward: Boolean): EnterTransition =
        slideInHorizontally(tween(SlideMs, easing = FastOutSlowInEasing)) { width -> if (forward) width / 8 else -width / 8 } +
            fadeIn(tween(InMs, delayMillis = OutMs, easing = LinearOutSlowInEasing))
    private fun slideOut(forward: Boolean): ExitTransition =
        slideOutHorizontally(tween(SlideMs, easing = FastOutSlowInEasing)) { width -> if (forward) -width / 8 else width / 8 } +
            fadeOut(tween(OutMs, easing = FastOutLinearInEasing))

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { if (betweenTopLevel()) fadeThroughIn else slideIn(forward = true) }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { if (betweenTopLevel()) fadeThroughOut else slideOut(forward = true) }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { if (betweenTopLevel()) fadeThroughIn else slideIn(forward = false) }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { if (betweenTopLevel()) fadeThroughOut else slideOut(forward = false) }
}
