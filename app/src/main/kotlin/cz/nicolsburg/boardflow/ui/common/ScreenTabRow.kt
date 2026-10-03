package cz.nicolsburg.boardflow.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.nicolsburg.boardflow.ui.theme.Dimens

/**
 * Every tab label in the app and the most tabs any screen has. Tab text is sized once from these,
 * so it is the same size on every screen and moving between screens feels steady. Add new labels
 * here (a label passed to [ScreenTabRow] that is missing still fits, but may shrink that row).
 */
object ScreenTabs {
    const val MaxTabsPerRow = 4
    val AllLabels = listOf(
        "Log Play", "Quick Guides",                      // Log Play
        "Plays", "Challenges", "Stats", "Players",       // Journal
        "My Shelf", "Sleeves",                           // Collection
        "Sync", "Preferences", "Scan", "Data"            // Settings
    )
}

// Material's Tab pads its text by 16dp on each side.
private val TabTextPadding = 16.dp
private const val MinTabFontSp = 11f

@Composable
fun ScreenTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val base = MaterialTheme.typography.labelLarge
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        // The widest label in the app must fit one line in a quarter of the screen: that size,
        // never larger than the theme's, is used by every tab row.
        val labelStyle: TextStyle = remember(maxWidth, base, density, tabs) {
            val available = with(density) { (maxWidth / ScreenTabs.MaxTabsPerRow - TabTextPadding * 2).toPx() }
            val labels = (ScreenTabs.AllLabels + tabs).distinct()
            var size = base.fontSize.value
            fun widest(sp: Float): Int {
                val style = base.copy(fontSize = sp.sp)
                return labels.maxOf { measurer.measure(it, style, maxLines = 1, softWrap = false).size.width }
            }
            while (size > MinTabFontSp && widest(size) > available) size -= 0.5f
            base.copy(fontSize = size.sp)
        }
        TabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.height(Dimens.MinTouchTarget),
                    // Only the selected tab is amber, so the row shows where you are.
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(label, style = labelStyle, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
                    }
                )
            }
        }
    }
}
