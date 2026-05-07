package cz.nicolsburg.boardflow.ui.history

import cz.nicolsburg.boardflow.data.SecurePreferences
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.Player
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class StatsTimeRange(val label: String, val subtitle: String = label) {
    ALL("All time"),
    THIS_YEAR("This year"),
    THIS_MONTH("This month"),
    LAST_30("Last 30 d", "Last 30 days")
}

fun List<LoggedPlay>.filterByTimeRange(range: StatsTimeRange): List<LoggedPlay> {
    if (range == StatsTimeRange.ALL) return this
    val now = LocalDate.now()
    val cutoff = when (range) {
        StatsTimeRange.THIS_YEAR -> LocalDate.of(now.year, 1, 1)
        StatsTimeRange.THIS_MONTH -> LocalDate.of(now.year, now.monthValue, 1)
        StatsTimeRange.LAST_30 -> now.minusDays(30)
        StatsTimeRange.ALL -> return this
    }
    return filter { play ->
        runCatching { LocalDate.parse(play.date) >= cutoff }.getOrDefault(false)
    }
}

data class GameHistoryStats(
    val plays: Int,
    val lastPlayedDate: String?,
    val avgDurationMinutes: Int?,
    val bestScore: Pair<String, Int>?,
    val mostWins: Pair<String, Int>?,
    val commonPlayers: List<Pair<String, Int>>
)

enum class InsightType(val priority: Int) {
    Milestone(1),
    RecentActivity(2),
    Rivalry(3),
    Session(4),
    Dormant(5)
}

data class ContextualInsight(
    val key: String,
    val text: String,
    val type: InsightType
)

private data class InsightCandidate(
    val key: String,
    val text: String,
    val type: InsightType
)

private fun resolveDisplayName(raw: String, roster: List<Player>): String {
    if (roster.isEmpty()) return raw.trim()
    val lower = raw.lowercase().trim()
    return roster.firstOrNull { player ->
        player.displayName.lowercase().trim() == lower ||
            player.aliases.any { alias -> alias.lowercase().trim() == lower }
    }?.displayName ?: raw.trim()
}

private fun computeGameHistoryStats(gamePlays: List<LoggedPlay>, roster: List<Player>): GameHistoryStats? {
    if (gamePlays.isEmpty()) return null

    val plays = gamePlays.sumOf { it.quantity.coerceAtLeast(1) }
    val lastDate = gamePlays.maxOfOrNull { it.date }?.let {
        runCatching {
            LocalDate.parse(it).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }.getOrDefault(it)
    }

    val withDuration = gamePlays.filter { it.durationMinutes > 0 }
    val avgDuration = if (withDuration.isEmpty()) null else withDuration.sumOf { it.durationMinutes } / withDuration.size

    var bestScore: Pair<String, Int>? = null
    gamePlays.forEach { play ->
        play.players.forEach { player ->
            val score = player.score.trim().toIntOrNull()
            if (score != null && player.name.isNotBlank()) {
                val displayName = resolveDisplayName(player.name, roster)
                if (bestScore == null || score > bestScore!!.second) {
                    bestScore = displayName to score
                }
            }
        }
    }

    val winCounts = mutableMapOf<String, Int>()
    gamePlays.forEach { play ->
        play.players.filter { it.isWinner && it.name.isNotBlank() }.forEach { player ->
            val name = resolveDisplayName(player.name, roster)
            winCounts[name] = (winCounts[name] ?: 0) + 1
        }
    }
    val mostWins = winCounts.maxByOrNull { it.value }?.let { it.key to it.value }

    val playerCounts = mutableMapOf<String, Int>()
    gamePlays.forEach { play ->
        play.players.filter { it.name.isNotBlank() }.forEach { player ->
            val name = resolveDisplayName(player.name, roster)
            playerCounts[name] = (playerCounts[name] ?: 0) + 1
        }
    }
    val commonPlayers = playerCounts.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(3)
        .map { it.key to it.value }

    return GameHistoryStats(plays, lastDate, avgDuration, bestScore, mostWins, commonPlayers)
}

fun List<LoggedPlay>.gameHistoryStats(gameName: String, roster: List<Player> = emptyList()): GameHistoryStats? {
    val gamePlays = filter { it.gameName.trim().equals(gameName.trim(), ignoreCase = true) }
    return computeGameHistoryStats(gamePlays, roster)
}

fun List<LoggedPlay>.gameHistoryStats(gameId: Int, roster: List<Player> = emptyList()): GameHistoryStats? {
    val gamePlays = filter { it.gameId == gameId }
    return computeGameHistoryStats(gamePlays, roster)
}

fun List<LoggedPlay>.gameHealthSignal(gameId: Int): Pair<String, Boolean>? {
    val gamePlays = filter { it.gameId == gameId }
    if (gamePlays.isEmpty()) return "No plays logged yet" to false

    val totalPlays = gamePlays.sumOf { it.quantity.coerceAtLeast(1) }
    if (totalPlays == 1) return "Only played once" to false

    val now = LocalDate.now()
    val cutoff30 = now.minusDays(30)
    val recentCount = gamePlays.count {
        runCatching { LocalDate.parse(it.date) >= cutoff30 }.getOrDefault(false)
    }
    if (recentCount >= 2) return "Played ${recentCount}x in the last 30 days" to true

    val lastDate = gamePlays
        .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
        .maxOrNull() ?: return null

    val days = now.toEpochDay() - lastDate.toEpochDay()
    return when {
        days <= 0 -> "Played today" to true
        days == 1L -> "Last played yesterday" to true
        days <= 14 -> "Last played $days days ago" to true
        days <= 60 -> "Last played $days days ago" to false
        else -> "Last played ${days / 30} months ago" to false
    }
}

fun List<LoggedPlay>.gameInsight(gameId: Int, roster: List<Player> = emptyList()): String? {
    val gamePlays = filter { it.gameId == gameId }
    if (gamePlays.size < 3) return null

    val winCounts = mutableMapOf<String, Int>()
    gamePlays.forEach { play ->
        play.players.filter { it.isWinner && it.name.isNotBlank() }.forEach { player ->
            val name = resolveDisplayName(player.name, roster)
            winCounts[name] = (winCounts[name] ?: 0) + 1
        }
    }
    val topWinner = winCounts.maxByOrNull { it.value } ?: return null
    return if (topWinner.value >= 2) "${topWinner.key} leads with ${topWinner.value} wins" else null
}

private fun buildInsightCandidates(
    gamePlays: List<LoggedPlay>,
    roster: List<Player>,
    currentPlayerName: String?
): List<InsightCandidate> {
    if (gamePlays.isEmpty()) return emptyList()

    val candidates = mutableListOf<InsightCandidate>()
    val now = LocalDate.now()
    val totalPlays = gamePlays.sumOf { it.quantity.coerceAtLeast(1) }
    val lastDate = gamePlays.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.maxOrNull()
    val daysSinceLast = lastDate?.let { now.toEpochDay() - it.toEpochDay() }

    when (totalPlays) {
        1 -> candidates += InsightCandidate("milestone_first_play", "First play logged", InsightType.Milestone)
        5, 10, 25, 50, 100 -> candidates += InsightCandidate(
            key = "milestone_$totalPlays",
            text = "$totalPlays plays logged",
            type = InsightType.Milestone
        )
    }

    val last7 = now.minusDays(7)
    val last30 = now.minusDays(30)
    val recent7 = gamePlays.sumOf { play ->
        if (runCatching { LocalDate.parse(play.date) >= last7 }.getOrDefault(false)) play.quantity.coerceAtLeast(1) else 0
    }
    val recent30 = gamePlays.sumOf { play ->
        if (runCatching { LocalDate.parse(play.date) >= last30 }.getOrDefault(false)) play.quantity.coerceAtLeast(1) else 0
    }
    when {
        daysSinceLast == 0L ->
            candidates += InsightCandidate("recent_today", "Played today", InsightType.RecentActivity)
        recent7 >= 2 ->
            candidates += InsightCandidate("recent_week_$recent7", "Played ${recent7}x this week", InsightType.RecentActivity)
        recent30 >= 2 ->
            candidates += InsightCandidate("recent_30_$recent30", "Played ${recent30}x in the last 30 days", InsightType.RecentActivity)
        daysSinceLast == 1L ->
            candidates += InsightCandidate("recent_yesterday", "Last played yesterday", InsightType.RecentActivity)
        daysSinceLast != null && daysSinceLast in 2..14 ->
            candidates += InsightCandidate("recent_${daysSinceLast}d", "Last played $daysSinceLast days ago", InsightType.RecentActivity)
    }

    if (totalPlays >= 3) {
        val winCounts = mutableMapOf<String, Int>()
        gamePlays.forEach { play ->
            play.players.filter { it.isWinner && it.name.isNotBlank() }.forEach { player ->
                val name = resolveDisplayName(player.name, roster)
                winCounts[name] = (winCounts[name] ?: 0) + 1
            }
        }
        val sorted = winCounts.entries.sortedByDescending { it.value }
        val top = sorted.firstOrNull()
        val second = sorted.getOrNull(1)
        val totalWins = sorted.sumOf { it.value }
        val hasClearLeader = top != null &&
            top.value >= 2 &&
            (second == null || top.value > second.value) &&
            (totalWins == 0 || top.value * 2 >= totalWins + 1)
        if (hasClearLeader) {
            val isCurrentPlayerLeader = currentPlayerName != null &&
                top.key.lowercase().trim() == currentPlayerName.lowercase().trim()
            val text = if (isCurrentPlayerLeader) {
                "You lead with ${top.value} wins"
            } else {
                "${top.key} leads with ${top.value} wins"
            }
            candidates += InsightCandidate("rivalry_${top.key}_${top.value}", text, InsightType.Rivalry)
        }
    }

    val withDuration = gamePlays.filter { it.durationMinutes > 0 }
    if (withDuration.isNotEmpty()) {
        fun formatDuration(minutes: Int) = when {
            minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60}h"
            minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
            else -> "${minutes}m"
        }

        val longestDuration = withDuration.maxOf { it.durationMinutes }
        if (withDuration.size >= 3) {
            val averageDuration = withDuration.sumOf { it.durationMinutes } / withDuration.size
            candidates += InsightCandidate(
                key = "session_avg_$averageDuration",
                text = "Average session length is ${formatDuration(averageDuration)}",
                type = InsightType.Session
            )
        }
        candidates += InsightCandidate(
            key = "session_longest_$longestDuration",
            text = "Longest session lasted ${formatDuration(longestDuration)}",
            type = InsightType.Session
        )
    }

    if (candidates.none { it.type == InsightType.RecentActivity }) {
        when {
            totalPlays == 1 ->
                candidates += InsightCandidate("dormant_once", "Only played once so far", InsightType.Dormant)
            daysSinceLast != null && daysSinceLast > 14 ->
                candidates += InsightCandidate("dormant_$daysSinceLast", "Last played $daysSinceLast days ago", InsightType.Dormant)
        }
    }

    return candidates.sortedWith(
        compareBy<InsightCandidate> { it.type.priority }
            .thenBy { it.key }
    )
}

fun List<LoggedPlay>.gameContextualInsight(
    gameId: Int,
    roster: List<Player> = emptyList(),
    currentPlayerName: String? = null,
    prefs: SecurePreferences? = null
): ContextualInsight? {
    val candidates = buildInsightCandidates(
        gamePlays = filter { it.gameId == gameId },
        roster = roster,
        currentPlayerName = currentPlayerName
    )
    if (candidates.isEmpty()) return null

    val topCandidate = candidates.first()
    val lastShownKey = prefs?.getLastGameInsightKey(gameId)
    val neighboringCandidate = candidates.firstOrNull { candidate ->
        candidate.key != topCandidate.key && candidate.type.priority <= topCandidate.type.priority + 1
    }
    val chosen = if (topCandidate.key == lastShownKey && neighboringCandidate != null) {
        neighboringCandidate
    } else {
        topCandidate
    }

    prefs?.setLastGameInsightKey(gameId, chosen.key)
    return ContextualInsight(
        key = chosen.key,
        text = chosen.text,
        type = chosen.type
    )
}

data class RivalryStat(
    val opponentName: String,
    val playsTogetherCount: Int,
    val myWins: Int,
    val theirWins: Int
)

fun List<LoggedPlay>.rivalriesForPlayer(player: Player, limit: Int = 4): List<RivalryStat> {
    val playerNames = (listOf(player.displayName) + player.aliases).map { it.lowercase().trim() }
    val myPlays = filter { play -> play.players.any { it.name.lowercase().trim() in playerNames } }

    val opponentMap = mutableMapOf<String, Triple<Int, Int, Int>>()
    myPlays.forEach { play ->
        val iWon = play.players.any { it.name.lowercase().trim() in playerNames && it.isWinner }
        play.players
            .filter { playerResult -> playerResult.name.isNotBlank() && playerResult.name.lowercase().trim() !in playerNames }
            .forEach { opponent ->
                val key = opponent.name.trim()
                val (together, myWins, theirWins) = opponentMap[key] ?: Triple(0, 0, 0)
                opponentMap[key] = Triple(
                    together + 1,
                    myWins + if (iWon) 1 else 0,
                    theirWins + if (opponent.isWinner) 1 else 0
                )
            }
    }

    return opponentMap.entries
        .sortedByDescending { it.value.first }
        .take(limit)
        .map { (name, stats) -> RivalryStat(name, stats.first, stats.second, stats.third) }
}

fun List<LoggedPlay>.playerCurrentWinStreak(playerNames: List<String>): Int {
    val lowerNames = playerNames.map { it.lowercase().trim() }
    val myPlays = filter { play ->
        play.players.any { it.name.lowercase().trim() in lowerNames }
    }.sortedByDescending { it.date }
    var streak = 0
    for (play in myPlays) {
        val won = play.players.any { it.name.lowercase().trim() in lowerNames && it.isWinner }
        if (won) streak++ else break
    }
    return streak
}

fun List<LoggedPlay>.hotPlayerStreak(roster: List<Player>): Pair<String, Int>? =
    roster.map { player ->
        val names = (listOf(player.displayName) + player.aliases).map { it.lowercase().trim() }
        player.displayName to playerCurrentWinStreak(names)
    }.filter { it.second >= 2 }.maxByOrNull { it.second }

fun List<LoggedPlay>.mostPlayedThisMonth(): Pair<String, Int>? {
    val now = LocalDate.now()
    val thisMonth = now.year * 100 + now.monthValue
    return filter { play ->
        runCatching {
            LocalDate.parse(play.date).let { it.year * 100 + it.monthValue } == thisMonth
        }.getOrDefault(false)
    }.groupBy { it.gameName }
        .mapValues { (_, plays) -> plays.sumOf { it.quantity.coerceAtLeast(1) } }
        .maxByOrNull { it.value }
        ?.let { it.key to it.value }
}

fun List<LoggedPlay>.playInsights(play: LoggedPlay): List<String> {
    val gamePlays = filter {
        it.gameName.trim().equals(play.gameName.trim(), ignoreCase = true)
    }
    val result = mutableListOf<String>()

    if (gamePlays.size == 1 && gamePlays[0].id == play.id) {
        result.add("First time playing ${play.gameName}!")
        return result
    }

    play.players.filter { it.isWinner }.forEach { winner ->
        if (result.size >= 2) return result
        val name = winner.name.trim().ifBlank { return@forEach }
        val hadPreviousWin = gamePlays.any { loggedPlay ->
            loggedPlay.id != play.id &&
                loggedPlay.players.any { it.name.trim().equals(name, ignoreCase = true) && it.isWinner }
        }
        if (!hadPreviousWin) result.add("First win for $name in ${play.gameName}!")
    }

    if (result.size < 2) {
        play.players.forEach { player ->
            if (result.size >= 2) return result
            val name = player.name.trim().ifBlank { return@forEach }
            val score = player.score.trim().toIntOrNull() ?: return@forEach
            val previousBest = gamePlays
                .filter { it.id != play.id }
                .flatMap { it.players }
                .filter { it.name.trim().equals(name, ignoreCase = true) }
                .mapNotNull { it.score.trim().toIntOrNull() }
                .maxOrNull()
            if (previousBest == null || score > previousBest) {
                result.add("New high score for $name: $score pts!")
            }
        }
    }

    if (result.size < 2 && play.durationMinutes > 0) {
        val previousMax = gamePlays.filter { it.id != play.id && it.durationMinutes > 0 }
            .maxOfOrNull { it.durationMinutes }
        if (previousMax == null || play.durationMinutes > previousMax) {
            val label = if (play.durationMinutes >= 60) {
                "${play.durationMinutes / 60}h ${play.durationMinutes % 60}m"
            } else {
                "${play.durationMinutes}m"
            }
            result.add("Longest session of ${play.gameName} ($label)!")
        }
    }

    return result
}
