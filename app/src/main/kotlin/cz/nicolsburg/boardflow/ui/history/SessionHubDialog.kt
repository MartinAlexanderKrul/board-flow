@file:OptIn(ExperimentalLayoutApi::class)

package cz.nicolsburg.boardflow.ui.history

import cz.nicolsburg.boardflow.ui.common.GameCover
import cz.nicolsburg.boardflow.ui.common.GameBackdrop
import cz.nicolsburg.boardflow.ui.common.BoardFlowInfoPill
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import cz.nicolsburg.boardflow.ui.common.BoardFlowStatTile
import cz.nicolsburg.boardflow.ui.common.BoardFlowMoodChip
import cz.nicolsburg.boardflow.ui.theme.Spacing
import cz.nicolsburg.boardflow.ui.theme.Dimens
import cz.nicolsburg.boardflow.ui.theme.BoardFlowShape
import cz.nicolsburg.boardflow.ui.common.PlayerAvatar
import cz.nicolsburg.boardflow.ui.common.BoardFlowSectionTitle
import cz.nicolsburg.boardflow.ui.common.BoardFlowSecondaryButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineField
import cz.nicolsburg.boardflow.ui.common.BoardFlowInlineAction
import cz.nicolsburg.boardflow.ui.common.BoardFlowIconButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormRow
import cz.nicolsburg.boardflow.ui.common.BoardFlowFormDivider
import cz.nicolsburg.boardflow.ui.common.BoardFlowButton
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.model.SessionHub
import cz.nicolsburg.boardflow.ui.common.AnimatedDialog
import cz.nicolsburg.boardflow.ui.common.BoardFlowTonalButton
import cz.nicolsburg.boardflow.ui.common.BoardFlowSurfaceTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionHubDialog(
    session: SessionHub,
    players: List<Player>,
    onDismiss: () -> Unit,
    onRenameSession: ((sessionId: String, title: String) -> Unit)? = null,
    onOpenPlay: ((LoggedPlay) -> Unit)? = null,
    onPlayAgain: ((SessionHub) -> Unit)? = null,
    onShareQr: ((SessionHub) -> Unit)? = null,
    thumbnailFor: (Int) -> String? = { null },
    showChronicle: Boolean = true
) {
    var isEditingTitle by remember(session.sessionId) { mutableStateOf(false) }
    var draftTitle by remember(session.sessionId, session.title) { mutableStateOf(session.title.orEmpty()) }
    LaunchedEffect(session.title) {
        if (!isEditingTitle) draftTitle = session.title.orEmpty()
    }
    val canRename = session.sessionId != null && onRenameSession != null

    val chronicleLines = if (showChronicle) {
        session.plays.mapNotNull { it.memory?.chronicleLine?.trim()?.takeIf { line -> line.isNotBlank() } }.distinct()
    } else {
        emptyList()
    }

    AnimatedDialog(
        onDismissRequest = onDismiss,
        backdrop = {
            GameBackdrop(imageUrl = thumbnailFor(session.anchorPlay.gameId), height = 180.dp, baseBlur = 6.dp)
        }
    ) {
        Column {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                buildSessionTitle(session),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                buildSessionSubtitle(session),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (onShareQr != null) {
                            BoardFlowIconButton(onClick = { onShareQr(session) }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share session",
                                    modifier = Modifier.size(Dimens.Icon),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (canRename && !isEditingTitle) {
                            BoardFlowIconButton(onClick = { isEditingTitle = true }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Rename session",
                                    modifier = Modifier.size(Dimens.Icon),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (isEditingTitle && canRename) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            SessionGroup {
                                BoardFlowFormRow(label = "Title") {
                                    BoardFlowInlineField(
                                        value = draftTitle,
                                        onValueChange = { draftTitle = it.take(48) },
                                        placeholder = buildSessionTitle(session),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BoardFlowInlineAction(destructive = true, onClick = {
                                    isEditingTitle = false
                                    draftTitle = session.title.orEmpty()
                                }) { Text("Cancel") }
                                BoardFlowSecondaryButton(onClick = {
                                    val id = session.sessionId
                                    if (id != null) onRenameSession?.invoke(id, draftTitle)
                                    isEditingTitle = false
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(Dimens.Icon))
                                    Spacer(Modifier.width(Spacing.sm))
                                    Text("Save title")
                                }
                            }
                        }
                    }
                }

                item { SessionHubSummaryCard(session = session) }

                if (chronicleLines.isNotEmpty() || session.moods.isNotEmpty() || session.quotes.isNotEmpty()) {
                    item { SessionHubMemoryCard(session = session, chronicleLines = chronicleLines) }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        BoardFlowSectionTitle(title = "Plays this session")
                        session.plays.forEach { play ->
                            SessionHubPlayCard(
                                play = play,
                                players = players,
                                thumbnailUrl = thumbnailFor(play.gameId),
                                onClick = onOpenPlay?.let { callback -> { callback(play) } }
                            )
                        }
                    }
                }
            }

            onPlayAgain?.let { callback ->
                HorizontalDivider(thickness = Dimens.Hairline, color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.End
                ) {
                    BoardFlowButton(onClick = { callback(session) }) { Text("Play session again") }
                }
            }
        }
    }
}

/** A raised group inside the dialog: one tone lighter than the dialog surface. */
@Composable
private fun SessionGroup(
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = BoardFlowShape.Card,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) { Column(content = content) }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = BoardFlowShape.Card,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) { Column(content = content) }
    }
}

@Composable
private fun SessionHubSummaryCard(session: SessionHub) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BoardFlowInfoPill(
            Icons.Default.EmojiEvents,
            if (session.totalLoggedPlays == 1) "1 game" else "${session.totalLoggedPlays} games",
            onArt = true
        )
        BoardFlowInfoPill(Icons.Default.Group, "${session.uniquePlayerNames.size}", onArt = true)
        if (session.totalDurationMinutes > 0) {
            BoardFlowInfoPill(Icons.Default.Schedule, "${session.totalDurationMinutes} min", onArt = true)
        }
        if (session.location.isNotBlank()) {
            BoardFlowInfoPill(
                Icons.Default.LocationOn,
                session.location.trim(),
                onArt = true,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

/** The story of the night: chronicle line, moods and quotes on an amber-tinted card. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionHubMemoryCard(session: SessionHub, chronicleLines: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = BoardFlowShape.Card,
        // Translucent grey, as on the play details session card.
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Icon(
                Icons.Default.AutoStories,
                contentDescription = null,
                modifier = Modifier.padding(top = 2.dp).size(Dimens.Icon),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                chronicleLines.take(3).forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                if (session.moods.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        session.moods.forEach { mood -> BoardFlowMoodChip(label = mood) }
                    }
                }
                session.quotes.take(2).forEach { quote ->
                    Text(
                        "\u201C$quote\u201D",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionHubPlayCard(
    play: LoggedPlay,
    players: List<Player>,
    thumbnailUrl: String? = null,
    onClick: (() -> Unit)?
) {
    SessionGroup(onClick = onClick) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameCover(name = play.gameName, thumbnailUrl = thumbnailUrl, size = 44.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        play.gameName,
                        style = MaterialTheme.typography.titleMedium,
                        // Amber only when the play can be opened from here.
                        color = if (onClick != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                    )
                    val meta = listOfNotNull(
                        "${play.durationMinutes} min".takeIf { play.durationMinutes > 0 },
                        "Played ${play.quantity} times".takeIf { play.quantity > 1 }
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(
                            meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onClick != null) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            play.players.forEach { player ->
                val displayName = resolveSessionDisplayName(player.name, players)
                val score = player.score.trim().takeIf { it.isNotBlank() && it != "0" }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerAvatar(
                        displayName,
                        size = 24.dp,
                        color = player.color.takeIf { it.isNotBlank() }?.let(::resolvedPlayerColor)
                    )
                    Text(
                        displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (player.isWinner) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (player.isWinner) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = "Winner",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.IconSmall)
                        )
                    }
                    if (score != null) {
                        Text(
                            score,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (player.isWinner) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (player.isWinner) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun buildSessionTitle(session: SessionHub): String =
    session.title?.takeIf { it.isNotBlank() } ?: runCatching {
        val dow = LocalDate.parse(session.date).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        "${dow}'s session"
    }.getOrDefault("Session")

private fun buildSessionSubtitle(session: SessionHub): String {
    val parts = mutableListOf<String>()
    parts += formatSessionDate(session.date)
    if (session.location.isNotBlank()) parts += session.location
    return parts.joinToString(" · ")
}

private fun formatSessionDate(raw: String): String =
    runCatching {
        LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }.getOrDefault(raw)

private fun resolveSessionDisplayName(name: String, players: List<Player>): String {
    if (name.isBlank()) return name
    val lower = name.lowercase().trim()
    return players.firstOrNull { player ->
        (listOf(player.displayName) + player.aliases).any { it.lowercase().trim() == lower }
    }?.displayName ?: name
}
