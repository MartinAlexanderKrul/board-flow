package cz.nicolsburg.boardflow.ui.common

import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.alpha
import cz.nicolsburg.boardflow.ui.theme.BoardFlowColors
import androidx.compose.ui.graphics.Color
import cz.nicolsburg.boardflow.util.toFlexibleLocalDateOrNull
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.Spacing

// The shared building blocks. A screen should be assembled from these (plus the buttons,
// sheets and dialogs in BoardFlowUi.kt) instead of styling its own surfaces and fields.
//
// Colour rule: amber (colorScheme.primary) means "you can tap this" or "this is selected":
// buttons, links, tappable titles, the icons of editable rows. The one exception is gold for
// the winner (trophy and score). Other colour comes from game art and player avatars, which
// never look like buttons. Labels and headings that do nothing are onSurface or onSurfaceVariant.

// ---------------------------------------------------------------------------
// Surfaces
// ---------------------------------------------------------------------------

/** The one card. [emphasized] is for the single thing on screen that needs attention. */
@Composable
fun BoardFlowCard(
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Spacing.md),
    content: @Composable ColumnScope.() -> Unit
) {
    // Surfaces separate by tone, not by outlines. Only the emphasized card gets a line.
    val border = if (emphasized) {
        BorderStroke(Dimens.Hairline, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    } else {
        null
    }
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = BoardFlowShape.Card,
            color = MaterialTheme.colorScheme.surface,
            border = border,
            content = body
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = BoardFlowShape.Card,
            color = MaterialTheme.colorScheme.surface,
            border = border,
            content = body
        )
    }
}

/** Heading for a group of content on a screen, with an optional action on the right. */
@Composable
fun BoardFlowSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (!supporting.isNullOrBlank()) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke(this)
    }
}

/**
 * One row of a settings or actions list (Settings, Sync): icon, title with a one-line detail, then
 * a value, a switch or a chevron. [destructive] rows (clear, delete) are red. Rows sit in a
 * [BoardFlowFormGroup], under a [BoardFlowSectionTitle].
 */
@Composable
fun BoardFlowSettingRow(
    icon: ImageVector,
    title: String,
    detail: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.45f)
            .heightIn(min = 64.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.Icon), tint = accent)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (!detail.isNullOrBlank()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The current value at the end of a [BoardFlowSettingRow]; [chevron] when tapping changes it. */
@Composable
fun BoardFlowSettingValue(text: String, chevron: Boolean = true) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 140.dp)
    )
    if (chevron) {
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------
// Games
// ---------------------------------------------------------------------------

/** Box cover for a game, with the first letter as a stand-in when there is no image. */
@Composable
fun GameCover(
    name: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val shape = MaterialTheme.shapes.extraSmall
    if (!thumbnailUrl.isNullOrBlank()) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.trim().take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The one row for "a game you can pick": cover, name, optional second line, trailing actions. */
@Composable
fun GameListRow(
    name: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = BoardFlowShape.Control,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(start = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            GameCover(name = name, thumbnailUrl = thumbnailUrl)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!supporting.isNullOrBlank()) {
                    Text(
                        supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing()
        }
    }
}

// ---------------------------------------------------------------------------
// Inputs
// ---------------------------------------------------------------------------

/**
 * A group of form rows on one tonal surface, separated by [BoardFlowFormDivider].
 * Forms are built from these instead of one outlined box per field.
 */
@Composable
fun BoardFlowFormGroup(
    modifier: Modifier = Modifier,
    // Inside a dialog the group sits on the dialog surface and needs the next tone up.
    raised: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BoardFlowShape.Card,
        color = if (raised) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
    ) {
        Column(content = content)
    }
}

@Composable
fun BoardFlowFormDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = Spacing.lg),
        thickness = Dimens.Hairline,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * One line of a form: icon, label, then the value or control.
 * A null [labelWidth] lets the label take the free space (for rows that end in a switch).
 */
@Composable
fun BoardFlowFormRow(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    labelWidth: Dp? = 88.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.Icon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = if (labelWidth != null) Modifier.width(labelWidth) else Modifier.weight(1f)
        )
        content()
    }
}

/**
 * Borderless text input for use inside a [BoardFlowFormRow] or a small value box.
 * It tracks its own text, so fast typing never works from a stale value.
 */
@Composable
fun BoardFlowInlineField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    textAlign: TextAlign = TextAlign.Start,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    // [value] arrives a frame or more after we report an edit, so it can lag behind the
    // field. Only adopt it when it is not one of our own edits coming back.
    val echo = remember { EchoTracker(value) }
    if (echo.isExternalChange(value, fieldValue.text)) {
        fieldValue = TextFieldValue(value, TextRange(value.length))
    }
    val style = textStyle.copy(color = textColor, textAlign = textAlign)
    BasicTextField(
        value = fieldValue,
        onValueChange = { incoming ->
            val text = incoming.text
            fieldValue = incoming
            if (text != value) {
                echo.reported(text)
                onValueChange(text)
            }
        },
        modifier = modifier,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { inner ->
            Box(
                contentAlignment = when (textAlign) {
                    TextAlign.Center -> Alignment.Center
                    TextAlign.End -> Alignment.CenterEnd
                    else -> Alignment.CenterStart
                }
            ) {
                if (fieldValue.text.isEmpty()) {
                    Text(
                        placeholder,
                        style = style,
                        // Full secondary grey: a fainter placeholder falls below readable contrast.
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                inner()
            }
        }
    )
}

/** Tells a field's own edits coming back through state apart from a value set elsewhere. */
internal class EchoTracker(initial: String) {
    private var lastSeen = initial
    private val reported = ArrayDeque<String>()

    fun reported(text: String) {
        reported.addLast(text)
    }

    /** True when [value] was set from outside and the field should adopt it. */
    fun isExternalChange(value: String, fieldText: String): Boolean {
        if (value == lastSeen) return false
        lastSeen = value
        val index = reported.indexOf(value)
        if (index >= 0) {
            repeat(index + 1) { reported.removeFirst() }
            return false
        }
        reported.clear()
        return value != fieldText
    }
}

// ---------------------------------------------------------------------------
// States and feedback
// ---------------------------------------------------------------------------

/** Centered "nothing here" state with a next step. */
@Composable
fun BoardFlowEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        action?.invoke()
    }
}

/** Inline error: a plain sentence, optionally with a way out. */
@Composable
fun BoardFlowErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BoardFlowShape.Control,
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            action?.invoke()
        }
    }
}

/**
 * Short confirmation after an action ("Play updated"). Provided by the app shell. Pass an
 * [actionLabel] and [onAction] for an Undo; the snackbar then stays a little longer.
 */
interface BoardFlowMessenger {
    fun show(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null)
}

val LocalBoardFlowMessenger = staticCompositionLocalOf<BoardFlowMessenger> {
    object : BoardFlowMessenger {
        override fun show(message: String, actionLabel: String?, onAction: (() -> Unit)?) = Unit
    }
}

@Composable
fun BoardFlowSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = BoardFlowShape.Control,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            actionColor = MaterialTheme.colorScheme.primary
        )
    }
}

// ---------------------------------------------------------------------------
// Facts and highlights
// ---------------------------------------------------------------------------

/**
 * A small fact with an icon ("2", "45 min"). [onArt] darkens it for use over game art.
 * A row of these must fit on one line: keep the text short and give the one that can be
 * long (a location) `Modifier.weight(1f, fill = false)` so it ellipsizes instead of scrolling.
 */
@Composable
fun BoardFlowInfoPill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onArt: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = BoardFlowShape.Pill,
        color = if (onArt) Color.Black.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A mood on a saved play. Blue, so it reads as a memory and not as something to tap. */
@Composable
fun BoardFlowMoodChip(label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = BoardFlowShape.Pill, color = BoardFlowColors.InfoContainer) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = BoardFlowColors.Info,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Formatting
// ---------------------------------------------------------------------------

private val DisplayDateFormat =
    java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH)

/** Dates are stored as "2026-10-01" and always shown as "Oct 1, 2026". Unparseable text is shown as is. */
fun formatDisplayDate(raw: String): String =
    raw.toFlexibleLocalDateOrNull()?.format(DisplayDateFormat) ?: raw

private val ChipDateFormat =
    java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.ENGLISH)

/** Date for a chip in a one-line row: the year is dropped when it is the current year. */
fun formatChipDate(raw: String): String {
    val date = raw.toFlexibleLocalDateOrNull() ?: return raw
    return if (date.year == java.time.LocalDate.now().year) date.format(ChipDateFormat) else date.format(DisplayDateFormat)
}

/**
 * A labelled text field for settings and sign-in dialogs: tonal fill with the label inside,
 * no outline or underline. Takes the same slots as Material's text fields. Inside a form group use
 * [BoardFlowInlineField] instead.
 */
@Composable
fun BoardFlowTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions =
        androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions =
        androidx.compose.foundation.text.KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1
) {
    val fill = MaterialTheme.colorScheme.surfaceContainerHigh
    androidx.compose.material3.TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = BoardFlowShape.Control,
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = fill,
            unfocusedContainerColor = fill,
            disabledContainerColor = fill.copy(alpha = 0.5f),
            errorContainerColor = fill,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor = MaterialTheme.colorScheme.primary
        )
    )
}
