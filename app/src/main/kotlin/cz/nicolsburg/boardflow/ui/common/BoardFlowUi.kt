package cz.nicolsburg.boardflow.ui.common

import cz.nicolsburg.boardflow.ui.theme.PlayerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerColors
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import cz.nicolsburg.boardflow.ui.theme.Dimens
import kotlin.math.roundToInt

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

object BoardFlowSurfaceTokens {
    val CornerRadius = 12.dp
    val Shape = BoardFlowShape.Control
    /** Larger rounded shape for prominent feature content surfaces (session cards, play cards, banners). */
    val ContentCardShape = BoardFlowShape.Card
    val CardContentPadding = 16.dp
    val FilterControlHeight = 36.dp
    val FilterControlHorizontalPadding = 14.dp
    val FilterIconSize = 16.dp
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    accented: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(
        containerColor = if (accented) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface
    )
    // Cards separate by tone; only the accented one carries a line.
    val border = if (accented) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)) else null
    val cardModifier = modifier
        .fillMaxWidth()
        .animateContentSize(animationSpec = boardFlowTween(BoardFlowMotion.ContentResizeDuration))
    val columnContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(BoardFlowSurfaceTokens.CardContentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = BoardFlowSurfaceTokens.ContentCardShape,
            colors = colors,
            border = border,
            content = columnContent
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = BoardFlowSurfaceTokens.ContentCardShape,
            colors = colors,
            border = border,
            content = columnContent
        )
    }
}

@Composable
fun BoardFlowFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.defaultMinSize(minHeight = BoardFlowSurfaceTokens.FilterControlHeight),
        enabled = enabled,
        leadingIcon = leadingIcon,
        shape = BoardFlowShape.Pill,
        colors = boardFlowFilterChipColors()
    )
}

@Composable
fun BoardFlowFilterSection(
    label: String,
    detail: String,
    content: @Composable () -> Unit
) {
    // A heading and its chips, straight on the sheet: no box around each group.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
    }
}

@Composable
fun boardFlowFilterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    selectedLabelColor = MaterialTheme.colorScheme.primary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
)

object BoardFlowModalTokens {
    val TopDismissDragAreaHeight = 36.dp
    val DismissThreshold = 96.dp
    val BottomSheetShape = BoardFlowShape.SheetTop
    const val DismissGestureRegionFraction = 0.25f
}

/**
 * Dialog wrapper that animates content in on entry (scale + fade from 0.92).
 * Exit uses the platform default dialog dismiss animation.
 */
@Composable
fun AnimatedDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    backdrop: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        var visible by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val dismissThresholdPx = with(density) { BoardFlowModalTokens.DismissThreshold.toPx() }
        val dismissGestureFallbackPx = with(density) { BoardFlowModalTokens.TopDismissDragAreaHeight.toPx() }
        val dismissSlopPx = with(density) { 8.dp.toPx() }
        val offsetY = remember { Animatable(0f) }
        val maxH = LocalConfiguration.current.screenHeightDp.dp * 0.85f
        var modalHeightPx by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) { visible = true }
        AnimatedVisibility(
            visible = visible,
            enter = boardFlowFadeIn(BoardFlowMotion.DialogDuration) + androidx.compose.animation.scaleIn(
                boardFlowTween(BoardFlowMotion.DialogDuration),
                initialScale = BoardFlowMotion.DialogInitialScale,
            ),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .heightIn(max = maxH)
                    .onGloballyPositioned { modalHeightPx = it.size.height }
                    .pointerInput(onDismissRequest, modalHeightPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial
                            )
                            val dismissGestureHeight = (modalHeightPx * BoardFlowModalTokens.DismissGestureRegionFraction)
                                .takeIf { it > 0f }
                                ?: dismissGestureFallbackPx
                            if (down.position.y > dismissGestureHeight) return@awaitEachGesture

                            var pointerId = down.id
                            var totalDragY = 0f
                            var totalDragX = 0f
                            var dismissDragActive = false

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: event.changes.firstOrNull()
                                    ?: break
                                pointerId = change.id
                                if (!change.pressed) break

                                val dragAmount = change.positionChange()
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y

                                if (!dismissDragActive &&
                                    totalDragY > dismissSlopPx &&
                                    kotlin.math.abs(totalDragY) > kotlin.math.abs(totalDragX)
                                ) {
                                    dismissDragActive = true
                                }

                                if (dismissDragActive) {
                                    change.consume()
                                    scope.launch {
                                        offsetY.snapTo(
                                            (offsetY.value + dragAmount.y).coerceAtLeast(0f)
                                        )
                                    }
                                }
                            }

                            if (dismissDragActive) {
                                scope.launch {
                                    if (offsetY.value > dismissThresholdPx) {
                                        onDismissRequest()
                                    } else {
                                        offsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = boardFlowTween(BoardFlowMotion.VisibilityDuration)
                                        )
                                    }
                                }
                            }
                        }
                    },
                shape = BoardFlowShape.Sheet,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    backdrop?.invoke()
                    if (backdrop != null) {
                        // Over artwork the handle floats on top, so the title starts near
                        // the top edge instead of below an empty strip.
                        Box(modifier = Modifier.padding(top = 20.dp)) { content() }
                        BoardFlowDismissDragHandle(Modifier.height(20.dp))
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            BoardFlowDismissDragHandle()

                            // Content weight(fill=false) gives it bounded height so inner
                            // LazyColumns scroll correctly, while short dialogs stay compact.
                            Box(modifier = Modifier.weight(1f, fill = false)) {
                                content()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardFlowModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        // No tonal elevation: it would tint every surface inside the sheet amber.
        tonalElevation = 0.dp,
        shape = BoardFlowModalTokens.BottomSheetShape,
        dragHandle = { BoardFlowDismissDragHandle() },
        content = content
    )
}

@Composable
private fun BoardFlowDismissDragHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BoardFlowModalTokens.TopDismissDragAreaHeight)
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    shape = CircleShape
                )
        )
    }
}

fun TextStyle.withTabularNumbers(): TextStyle = copy(fontFeatureSettings = "tnum")

object BoardFlowActionTokens {
    // Buttons are compact pills that hug their label. Do not stretch them with fillMaxWidth;
    // a wide slab reads as a banner, not as something to press.
    // 40dp visible; Material still reserves a 48dp touch target around it.
    val ButtonMinHeight = 40.dp
    val ButtonShape = BoardFlowShape.Pill
    val ButtonContentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
    val InlineActionContentPadding = ButtonDefaults.TextButtonContentPadding
    val SecondaryButtonMinHeight = 32.dp
    // Compact pills (32dp): the game detail header buttons and its BGG / Rules / Drive links.
    val SecondaryButtonContentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    val IconButtonSize = Dimens.MinTouchTarget
    // Translucent black: always a step darker than the card, dialog or page underneath.
    val SecondaryContainer = Color.Black.copy(alpha = 0.34f)
    val IconSize = 20.dp
    val CompactIconSize = 18.dp
    val IconTextSpacing = 8.dp
}

enum class BoardFlowConfirmationKind {
    POSITIVE,
    NEUTRAL,
    DESTRUCTIVE
}

private object BoardFlowConfirmationTokens {
    val MaxWidth = 360.dp
    val Shape = BoardFlowShape.Sheet
    val OuterPadding = 32.dp
    val ContentPadding = 20.dp
    val ContentPaddingBottom = 12.dp
    val IconContainerSize = 32.dp
    val IconSize = 16.dp
    val HeaderSpacing = 12.dp
    val MessageSpacing = 20.dp
    val ActionSpacing = 4.dp
}

@Composable
fun BoardFlowConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    kind: BoardFlowConfirmationKind = BoardFlowConfirmationKind.NEUTRAL,
    icon: ImageVector? = null,
    dismissOnOutsideTap: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = dismissOnOutsideTap
        )
    ) {
        val destructive = kind == BoardFlowConfirmationKind.DESTRUCTIVE
        val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        val resolvedIcon = icon ?: if (destructive) Icons.Default.WarningAmber else null

        Surface(
            modifier = modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .widthIn(max = BoardFlowConfirmationTokens.MaxWidth),
            shape = BoardFlowShape.Sheet,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (resolvedIcon != null) {
                        Icon(
                            imageVector = resolvedIcon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(Dimens.IconLarge)
                        )
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // The way out is quiet; the action itself carries the colour.
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text(dismissLabel, style = MaterialTheme.typography.labelMedium)
                    }
                    BoardFlowSecondaryButton(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = if (destructive) MaterialTheme.colorScheme.onError
                                           else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun BoardFlowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = rememberBoardFlowPressScale(isPressed = isPressed, label = "btnScale")
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = BoardFlowActionTokens.ButtonMinHeight)
            .scale(scale),
        enabled = enabled,
        colors = colors,
        shape = BoardFlowActionTokens.ButtonShape,
        contentPadding = BoardFlowActionTokens.ButtonContentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * The secondary button: amber outline and label on a transparent fill, the same height as the
 * primary. Use for alternatives next to the one filled action of a view.
 */
@Composable
fun BoardFlowSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    ),
    // 32dp instead of 40dp, for a row of small links (game detail: BGG, Rules, Drive).
    compact: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = rememberBoardFlowPressScale(isPressed = isPressed, label = "btnScale")
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (compact) BoardFlowActionTokens.SecondaryButtonMinHeight else BoardFlowActionTokens.ButtonMinHeight)
            .scale(scale),
        enabled = enabled,
        colors = colors,
        border = BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        ),
        shape = BoardFlowActionTokens.ButtonShape,
        contentPadding = if (compact) BoardFlowActionTokens.SecondaryButtonContentPadding else BoardFlowActionTokens.ButtonContentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun BoardFlowDestructiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = rememberBoardFlowPressScale(isPressed = isPressed, label = "destructiveBtnScale")
    // Outlined red, at the primary button's size so it lines up with a Save next to it.
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = BoardFlowActionTokens.ButtonMinHeight)
            .scale(scale),
        enabled = enabled,
        shape = BoardFlowActionTokens.ButtonShape,
        contentPadding = BoardFlowActionTokens.ButtonContentPadding,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = if (enabled) 0.55f else 0.24f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
            disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
        ),
        interactionSource = interactionSource
    ) {
        content()
    }
}

@Composable
fun BoardFlowInlineAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    destructive: Boolean = false,
    // True when it sits next to a full-size button: neighbours share one size.
    large: Boolean = false,
    // Plain white text, for Cancel and Close: leaving loses nothing, so it is neither amber nor red.
    neutral: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val tint = when {
        neutral -> MaterialTheme.colorScheme.onSurface
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    TextButton(
        onClick = onClick,
        modifier = if (large) modifier.defaultMinSize(minHeight = BoardFlowActionTokens.ButtonMinHeight) else modifier,
        enabled = enabled,
        // The label takes the tint too, not just the icon.
        colors = ButtonDefaults.textButtonColors(contentColor = tint),
        contentPadding = BoardFlowActionTokens.InlineActionContentPadding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BoardFlowActionTokens.IconTextSpacing)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
            }
            // Same label size as the button it sits next to.
            ProvideTextStyle(
                if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium
            ) { content() }
        }
    }
}

@Composable
fun BoardFlowIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(BoardFlowActionTokens.IconButtonSize),
        enabled = enabled,
        colors = colors
    ) {
        content()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Popover(
    anchorCoordinates: LayoutCoordinates?,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!visible || anchorCoordinates == null) return
    val density = LocalDensity.current
    val view = LocalView.current
    // Get anchor position in window
    val anchorPos = anchorCoordinates.localToWindow(Offset.Zero)
    val anchorSize = anchorCoordinates.size
    // Estimate popover size (max width 320dp, height unknown until measured)
    val maxPopoverWidthPx = with(density) { 320.dp.roundToPx() }
    // Calculate initial popover position (below anchor)
    var x = anchorPos.x.toInt()
    var y = (anchorPos.y + anchorSize.height).toInt() + with(density) { 8.dp.roundToPx() }
    // Adjust x if popover would overflow right edge
    if (x + maxPopoverWidthPx > view.width) {
        x = (view.width - maxPopoverWidthPx - with(density) { 8.dp.roundToPx() }).coerceAtLeast(0)
    }
    // Adjust y if popover would overflow bottom edge (estimate height as 200dp if unknown)
    val estPopoverHeightPx = with(density) { 200.dp.roundToPx() }
    if (y + estPopoverHeightPx > view.height) {
        y = (anchorPos.y - estPopoverHeightPx - with(density) { 8.dp.roundToPx() }).toInt().coerceAtLeast(0)
    }
    // Fullscreen box to catch outside clicks
    Box(
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onDismissRequest()
                })
            }
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(x, y) }
                .widthIn(max = 320.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = BoardFlowShape.Card
                )
                .shadow(8.dp, BoardFlowShape.Card)
                .then(modifier)
        ) {
            content()
        }
    }
}

@Composable
fun BoardFlowPickerField(
    label: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "PickerChevron"
    )
    val labelColor = if (expanded)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$label, $value"
                role = Role.Button
            },
        // A tonal field: it only gets a line while its sheet is open.
        shape = BoardFlowShape.Control,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = if (expanded) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(chevronRotation)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> BoardFlowPickerSheet(
    title: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BoardFlowModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(options) { option ->
                val isSelected = option == selectedOption
                Card(
                    onClick = { onSelect(option) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = optionLabel(option) + if (isSelected) ", selected" else ""
                            role = Role.Button
                        },
                    shape = BoardFlowSurfaceTokens.Shape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            optionLabel(option),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------
// Shared player avatar — consistent across Players list, Rivalries, Log Play
// ---------------------------------------------------------------------------

fun playerInitials(name: String): String {
    val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> "${parts.first().take(1)}${parts.last().take(1)}".uppercase()
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "?"
    }
}

/** The colours a player can pick as their own. */
val PlayerColorChoices: List<Pair<String, String>> = listOf(
    "Red" to "#E53935", "Pink" to "#E91E63", "Purple" to "#8E24AA", "Violet" to "#7C4DFF",
    "Blue" to "#1E88E5", "Cyan" to "#00ACC1", "Teal" to "#00897B", "Green" to "#43A047",
    "Lime" to "#7CB342", "Yellow" to "#FDD835", "Orange" to "#FB8C00", "Brown" to "#6D4C41",
    "Navy" to "#283593", "Olive" to "#827717", "Maroon" to "#8D2B3A", "Taupe" to "#8D7B6E",
    "Beige" to "#D7CCB8", "Slate" to "#546E7A", "Grey" to "#757575", "Silver" to "#BDBDBD",
    "Charcoal" to "#424242", "Black" to "#1A1A1A", "White" to "#F5F5F5"
)

/**
 * Whether play rows (Journal list, play details, session cards) show the coloured initials
 * next to each player. Provided by AppShell from Settings > Preferences.
 */
val LocalShowPlayerAvatarsInPlays = compositionLocalOf { true }

fun parsePlayerColor(hex: String): Color? =
    hex.trim().takeIf { it.isNotBlank() }?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }

/**
 * Chosen avatar colours by lower-case player name and alias, provided by AppShell from the roster,
 * so every [PlayerAvatar] picks them up without each screen passing them along.
 */
val LocalPlayerColors = compositionLocalOf<Map<String, Color>> { emptyMap() }

fun playerColorMap(players: List<cz.nicolsburg.boardflow.model.Player>): Map<String, Color> = buildMap {
    players.forEach { player ->
        val color = parsePlayerColor(player.color) ?: return@forEach
        (player.aliases + player.displayName).forEach { name ->
            name.trim().lowercase().takeIf { it.isNotBlank() }?.let { put(it, color) }
        }
    }
}

fun playerInitialColor(name: String): Color {
    val palette = PlayerColors.automatic
    return palette[(name.hashCode() and 0x7FFFFFFF) % palette.size]
}

@Composable
fun BoardFlowCloseGlyph(
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    alpha: Float = 0.92f
) {
    Icon(
        imageVector = Icons.Default.Close,
        contentDescription = contentDescription,
        // Close is navigation, not emphasis: grey like the other utility icons.
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        modifier = modifier.size(iconSize)
    )
}

/**
 * Initials on a coloured circle. [color] overrides the name-based colour, e.g. with the
 * colour the player used in a play.
 */
@Composable
fun PlayerAvatar(name: String, size: Dp = 46.dp, modifier: Modifier = Modifier, color: Color? = null) {
    val initials = if (size.value <= 32f) name.trim().take(1).uppercase()
                   else playerInitials(name)
    val fill = color ?: LocalPlayerColors.current[name.trim().lowercase()] ?: playerInitialColor(name)
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = fill
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                initials,
                fontWeight = FontWeight.Bold,
                // Dark initials on light colours (white, yellow) so they stay readable.
                color = if (fill.luminance() > 0.55f) PlayerColors.DarkInk else Color.White,
                fontSize = minOf(size.value * 0.40f, 14f).sp
            )
        }
    }
}

/** Colours for every date picker: the app's surface, not Material's tinted container. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun boardFlowDatePickerColors(): DatePickerColors = DatePickerDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    headlineContentColor = MaterialTheme.colorScheme.onSurface
)
