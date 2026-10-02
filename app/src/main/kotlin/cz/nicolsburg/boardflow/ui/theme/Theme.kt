package cz.nicolsburg.boardflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Dark palette ───────────────────────────────────────────────────────────
// Surfaces are neutral-cool graphite so gold accents pop by contrast.
// Containers are warm graphite — cohesive with gold without going brown.
private val AmberGold        = Color(0xFFFEB316)
private val AmberDark        = Color(0xFFC98C00)
private val AmberContainer   = Color(0xFF252420)  // warm graphite (primary/tertiary container)
private val AmberLight       = Color(0xFFFFDFA0)
private val AmberDeepDark    = Color(0xFF201E18)  // deep warm graphite (secondary container)

// Each surface step is about 1.2:1 against the one below, so cards read without borders.
private val Background       = Color(0xFF0E0E0F)
private val Surface          = Color(0xFF1C1C1E)
private val SurfaceVariant   = Color(0xFF2A2A2C)
private val Outline          = Color(0xFF3D3D3E)
private val OutlineVariant   = Color(0xFF2E2E2F)

private val OnSurface        = Color(0xFFF0F0F0)
private val OnSurfaceMedium  = Color(0xFF9E9E9E)
private val OnAmber          = Color(0xFF131314)

private val ErrorRed         = Color(0xFFFF5252)
private val ErrorContainer   = Color(0xFF4D1010)
private val OnErrorContainer = Color(0xFFFFB4AB)

private val DarkColorScheme = darkColorScheme(
    primary              = AmberGold,
    onPrimary            = OnAmber,
    primaryContainer     = AmberContainer,
    onPrimaryContainer   = AmberLight,
    secondary            = AmberGold,
    onSecondary          = OnAmber,
    secondaryContainer   = AmberDeepDark,
    onSecondaryContainer = AmberLight,
    tertiary             = AmberDark,
    onTertiary           = OnAmber,
    tertiaryContainer    = AmberContainer,
    onTertiaryContainer  = AmberLight,
    background           = Background,
    onBackground         = OnSurface,
    surface              = Surface,
    onSurface            = OnSurface,
    surfaceVariant       = SurfaceVariant,
    onSurfaceVariant     = OnSurfaceMedium,

    surfaceContainerLowest  = Background,
    surfaceContainerLow     = Background,
    surfaceContainer        = Surface,
    surfaceContainerHigh    = SurfaceVariant,
    surfaceContainerHighest = Outline,
    surfaceBright           = SurfaceVariant,
    surfaceDim              = Background,

    outline              = Outline,
    outlineVariant       = OutlineVariant,
    error                = ErrorRed,
    onError              = OnAmber,
    errorContainer       = ErrorContainer,
    onErrorContainer     = OnErrorContainer,
    inverseSurface       = OnSurface,
    inverseOnSurface     = Background,
    inversePrimary       = AmberDark,
    scrim                = Color(0xFF000000),
)

// ── Theme ───────────────────────────────────────────────────────────────────

/**
 * BoardFlow has a single look: dark graphite with amber. Colours, type and shapes all
 * come from here so screens never define their own.
 */
@Composable
fun BggCombinedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = BoardFlowTypography,
        shapes = BoardFlowShapes,
        content = content
    )
}

/**
 * Meaning colours that Material's scheme has no slot for. Use these instead of
 * hard-coded greens, oranges and blues.
 */
object BoardFlowColors {
    val Success = Color(0xFF5BC98A)
    val SuccessContainer = Color(0xFF16301F)
    val Warning = Color(0xFFFF9F43)
    val WarningContainer = Color(0xFF3A2610)
    val Info = Color(0xFF7FB4E6)
    val InfoContainer = Color(0xFF14283A)
}
