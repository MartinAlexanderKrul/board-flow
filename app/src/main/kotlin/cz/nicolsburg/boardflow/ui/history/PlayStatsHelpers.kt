package cz.nicolsburg.boardflow.ui.history

import cz.nicolsburg.boardflow.data.SecurePreferences
import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.Player
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

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
    val lower = raw.lowercase().trim()
    return roster.firstOrNull { player ->
        player.displayName.lowercase().trim() == lower ||
            player.aliases.any { alias -> alias.lowercase().trim() == lower }
    }?.displayName ?: "Unknown"
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

// ── Shared duration formatter ─────────────────────────────────────────────────

fun formatDuration(minutes: Int): String = when {
    minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60}h"
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes}m"
}

// ── Range-specific observations ───────────────────────────────────────────────

fun List<LoggedPlay>.buildRangeObservations(
    range: StatsTimeRange,
    allPlays: List<LoggedPlay>
): List<SmartObservation> {
    if (isEmpty()) return emptyList()
    val now = LocalDate.now()
    val totalPlays = sumOf { it.quantity.coerceAtLeast(1) }
    val uniqueGames = map { it.gameName }.toSet().size
    val withDuration = filter { it.durationMinutes > 0 }
    val avgSession = if (withDuration.isNotEmpty()) withDuration.sumOf { it.durationMinutes } / withDuration.size else 0
    val result = mutableListOf<SmartObservation>()

    when (range) {
        StatsTimeRange.THIS_YEAR -> {
            val monthsElapsed = now.monthValue.coerceAtLeast(1)
            val projected = totalPlays * 12 / monthsElapsed

            // Pace projection
            if (monthsElapsed >= 2) {
                result += SmartObservation(
                    "$totalPlays plays so far this year. On pace for $projected by December."
                )
            }

            // vs last year
            val lastYearCount = allPlays.filter {
                runCatching { LocalDate.parse(it.date).year == now.year - 1 }.getOrDefault(false)
            }.sumOf { it.quantity.coerceAtLeast(1) }
            if (lastYearCount > 0 && monthsElapsed >= 3) {
                val paceVsLastYear = projected - lastYearCount
                when {
                    paceVsLastYear > 5 -> result += SmartObservation(
                        "Ahead of last year's pace by $paceVsLastYear plays. A stronger year."
                    )
                    paceVsLastYear < -5 -> result += SmartObservation(
                        "Behind last year's pace by ${-paceVsLastYear} plays. Still time to catch up."
                    )
                    else -> result += SmartObservation(
                        "Almost exactly on last year's pace. Consistent as ever."
                    )
                }
            }

            // Most active month this year
            val byMonth = groupBy { runCatching { LocalDate.parse(it.date).monthValue }.getOrNull() }
                .filterKeys { it != null }
                .mapValues { (_, g) -> g.sumOf { it.quantity.coerceAtLeast(1) } }
            val peakMonth = byMonth.maxByOrNull { it.value }
            if (peakMonth != null && byMonth.size >= 2) {
                val monthName = java.time.Month.of(peakMonth.key!!).getDisplayName(TextStyle.FULL, Locale.getDefault())
                    .replaceFirstChar { it.uppercase() }
                result += SmartObservation(
                    "$monthName was your busiest month — ${peakMonth.value} ${if (peakMonth.value == 1) "play" else "plays"}."
                )
            }

            // Unique games
            if (uniqueGames >= 5) {
                result += SmartObservation(
                    "$uniqueGames different games played this year. " +
                        if (uniqueGames > totalPlays / 2) "Lots of variety." else "With some clear favorites emerging."
                )
            }

            // Average session this year
            if (avgSession > 0) {
                result += SmartObservation(
                    "Average session this year: ${formatDuration(avgSession)}."
                )
            }
        }

        StatsTimeRange.THIS_MONTH -> {
            val monthName = now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                .replaceFirstChar { it.uppercase() }

            // vs last month
            val lastMonthStart = now.withDayOfMonth(1).minusMonths(1)
            val lastMonthEnd = now.withDayOfMonth(1).minusDays(1)
            val lastMonthCount = allPlays.filter {
                runCatching {
                    val d = LocalDate.parse(it.date)
                    !d.isBefore(lastMonthStart) && !d.isAfter(lastMonthEnd)
                }.getOrDefault(false)
            }.sumOf { it.quantity.coerceAtLeast(1) }

            when {
                lastMonthCount == 0 -> result += SmartObservation(
                    "$totalPlays ${if (totalPlays == 1) "play" else "plays"} in $monthName so far."
                )
                totalPlays > lastMonthCount -> result += SmartObservation(
                    "$totalPlays plays in $monthName so far — more than all of last month ($lastMonthCount)."
                )
                totalPlays == lastMonthCount -> result += SmartObservation(
                    "$totalPlays plays so far — exactly matching last month. Right on track."
                )
                else -> result += SmartObservation(
                    "$totalPlays plays in $monthName so far. Last month was $lastMonthCount."
                )
            }

            // Most played game this month
            val topGame = groupBy { it.gameName }
                .mapValues { (_, g) -> g.sumOf { it.quantity.coerceAtLeast(1) } }
                .maxByOrNull { it.value }
            if (topGame != null && topGame.value >= 2) {
                result += SmartObservation(
                    "${topGame.key} is leading the month with ${topGame.value} plays."
                )
            }

            // Active days this month
            val activeDays = mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet().size
            val daysInMonth = now.dayOfMonth
            if (activeDays >= 3) {
                result += SmartObservation(
                    "Games on $activeDays of the $daysInMonth days so far this month."
                )
            }

            // Average session
            if (avgSession > 0 && withDuration.size >= 2) {
                result += SmartObservation(
                    "Average session in $monthName: ${formatDuration(avgSession)}."
                )
            }
        }

        StatsTimeRange.LAST_30 -> {
            // vs previous 30 days
            val cutoff30 = now.minusDays(30)
            val cutoff60 = now.minusDays(60)
            val prev30Count = allPlays.filter {
                runCatching {
                    val d = LocalDate.parse(it.date)
                    !d.isBefore(cutoff60) && d.isBefore(cutoff30)
                }.getOrDefault(false)
            }.sumOf { it.quantity.coerceAtLeast(1) }

            when {
                prev30Count == 0 -> result += SmartObservation(
                    "$totalPlays plays in the last 30 days."
                )
                totalPlays > prev30Count * 1.3 -> result += SmartObservation(
                    "$totalPlays plays in 30 days — up from $prev30Count the month before. Momentum building."
                )
                totalPlays < prev30Count * 0.7 -> result += SmartObservation(
                    "$totalPlays plays in the last 30 days, down from $prev30Count. Quieter stretch."
                )
                else -> result += SmartObservation(
                    "$totalPlays plays in the last 30 days — roughly the same as the month before ($prev30Count)."
                )
            }

            // Weekly pace
            val playsPerWeek = totalPlays.toFloat() / 4.3f
            if (playsPerWeek >= 1f) {
                result += SmartObservation(
                    "${String.format("%.1f", playsPerWeek)} plays per week on average over the last month."
                )
            }

            // Unique games
            if (uniqueGames >= 3) {
                result += SmartObservation(
                    "$uniqueGames different games across $totalPlays plays in 30 days."
                )
            }

            // Average session
            if (avgSession > 0 && withDuration.size >= 3) {
                result += SmartObservation(
                    "Sessions averaged ${formatDuration(avgSession)} over the last 30 days."
                )
            }
        }

        StatsTimeRange.ALL -> Unit
    }

    return result.distinctBy { it.text }
}

// ── Smart observations ────────────────────────────────────────────────────────

data class SmartObservation(val text: String)

fun List<LoggedPlay>.buildSmartObservations(): List<SmartObservation> {
    if (size < 5) return emptyList()
    val result = mutableListOf<SmartObservation>()
    val now = LocalDate.now()
    val totalPlays = sumOf { it.quantity.coerceAtLeast(1) }
    val withDuration = filter { it.durationMinutes > 0 }
    val totalMinutes = withDuration.sumOf { it.durationMinutes }
    val uniqueGames = map { it.gameName }.toSet().size

    // Total time as days
    if (totalMinutes >= 180) {
        val hours = totalMinutes / 60
        val days = totalMinutes / (60 * 24)
        if (days >= 2) {
            result += SmartObservation(
                "You've spent ${hours}h at the table. That's $days full days living inside cardboard worlds."
            )
        } else {
            result += SmartObservation("You've logged ${hours}h of plays. The stack keeps calling.")
        }
    }

    // Dominant game by time share
    if (withDuration.size >= 5 && totalMinutes > 0) {
        val gameMinutes = withDuration.groupBy { it.gameName }.mapValues { (_, g) -> g.sumOf { it.durationMinutes } }
        val top = gameMinutes.maxByOrNull { it.value }
        if (top != null) {
            val pct = top.value * 100 / totalMinutes
            if (pct >= 20) {
                result += SmartObservation(
                    "${top.key} accounts for ${pct}% of your table time. Some games just become a home."
                )
            }
        }
    }

    // Weekend vs weekday session length
    val weekdayDur = filter { p ->
        p.durationMinutes > 0 &&
            runCatching { LocalDate.parse(p.date).dayOfWeek.value in 1..5 }.getOrDefault(false)
    }
    val weekendDur = filter { p ->
        p.durationMinutes > 0 &&
            runCatching { LocalDate.parse(p.date).dayOfWeek.value in 6..7 }.getOrDefault(false)
    }
    if (weekdayDur.size >= 4 && weekendDur.size >= 4) {
        val wdAvg = weekdayDur.sumOf { it.durationMinutes } / weekdayDur.size
        val weAvg = weekendDur.sumOf { it.durationMinutes } / weekendDur.size
        if (weAvg > wdAvg * 1.4) {
            result += SmartObservation(
                "Weekend sessions average ${formatDuration(weAvg)}. Weeknights: ${formatDuration(wdAvg)}. " +
                    "Two different people sit down at this table."
            )
        }
    }

    // Dormant beloved game
    val dormant = groupBy { it.gameName }
        .mapNotNull { (name, plays) ->
            val count = plays.sumOf { it.quantity.coerceAtLeast(1) }
            val last = plays.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.maxOrNull()
            val days = last?.let { now.toEpochDay() - it.toEpochDay() }
            if (count >= 4 && days != null && days > 60) Triple(name, days, count) else null
        }
        .maxByOrNull { it.third }
    if (dormant != null) {
        val months = (dormant.second / 30).toInt()
        result += SmartObservation(
            "${dormant.first} hasn't hit the table in ${months} month${if (months > 1) "s" else ""}. " +
                "You've played it ${dormant.third} times. Maybe it's waiting."
        )
    }

    // New games discovered this year
    val thisYear = now.year
    val newThisYear = map { it.gameName }.toSet().count { name ->
        val first = filter { it.gameName == name }
            .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.minOrNull()
        first?.year == thisYear
    }
    when {
        newThisYear >= 5 -> result += SmartObservation(
            "$newThisYear games played for the first time this year. The search for a new favorite never ends."
        )
        newThisYear in 2..4 -> result += SmartObservation(
            "$newThisYear new games tried so far this year. Still exploring."
        )
    }

    // Dominant season
    val bySeason = IntArray(4)
    forEach { play ->
        runCatching {
            val m = LocalDate.parse(play.date).monthValue
            val s = when (m) { in 3..5 -> 0; in 6..8 -> 1; in 9..11 -> 2; else -> 3 }
            bySeason[s] += play.quantity.coerceAtLeast(1)
        }
    }
    val seasonTotal = bySeason.sum()
    if (seasonTotal > 0) {
        val peakIdx = bySeason.indices.maxByOrNull { bySeason[it] }!!
        val pct = bySeason[peakIdx] * 100 / seasonTotal
        if (pct >= 38) {
            val names = listOf("spring", "summer", "fall", "winter")
            result += SmartObservation(
                "${pct}% of your plays happen in ${names[peakIdx]}. The season brings you to the table."
            )
        }
    }

    // Play pace vs last year
    val lastYear = thisYear - 1
    val thisYearCount = filter { runCatching { LocalDate.parse(it.date).year == thisYear }.getOrDefault(false) }
        .sumOf { it.quantity.coerceAtLeast(1) }
    val lastYearCount = filter { runCatching { LocalDate.parse(it.date).year == lastYear }.getOrDefault(false) }
        .sumOf { it.quantity.coerceAtLeast(1) }
    val monthsIn = now.monthValue
    if (lastYearCount >= 8 && thisYearCount > 0 && monthsIn >= 2) {
        val projected = thisYearCount * 12 / monthsIn
        when {
            projected > lastYearCount * 1.25 ->
                result += SmartObservation(
                    "On pace for $projected plays this year. $lastYear was $lastYearCount. The table is busy."
                )
            projected < lastYearCount * 0.65 && monthsIn >= 4 ->
                result += SmartObservation(
                    "Quieter than $lastYear so far — $thisYearCount plays in $monthsIn months. Life has its seasons."
                )
        }
    }

    // Consistency vs burstiness
    val monthCounts = groupBy { p ->
        runCatching { LocalDate.parse(p.date).let { it.year * 100 + it.monthValue } }.getOrNull()
    }.filterKeys { it != null }.mapValues { (_, g) -> g.sumOf { it.quantity.coerceAtLeast(1) } }
    if (monthCounts.size >= 6) {
        val vals = monthCounts.values.toList()
        val avg = vals.average()
        val stdDev = sqrt(vals.map { (it - avg) * (it - avg) }.average())
        val cv = if (avg > 0) stdDev / avg else 0.0
        when {
            cv < 0.35 && avg >= 2 -> result += SmartObservation(
                "${avg.toInt()} plays a month, give or take. Consistency is its own kind of mastery."
            )
            cv > 1.2 -> result += SmartObservation(
                "You game in bursts — quiet months, then a flurry. The table calls when it calls."
            )
        }
    }

    // Depth vs breadth
    if (totalPlays >= 20 && uniqueGames >= 3) {
        val depth = totalPlays.toDouble() / uniqueGames
        when {
            depth > 5.0 -> result += SmartObservation(
                "$uniqueGames games, $totalPlays plays. ${String.format("%.1f", depth)} plays each on average. You go deep."
            )
            depth < 1.5 -> result += SmartObservation(
                "$uniqueGames different games across $totalPlays plays. You keep it wide and fresh."
            )
        }
    }

    return result.distinctBy { it.text }
}

// ── Contextual narrative header ───────────────────────────────────────────────

fun List<LoggedPlay>.buildContextualHeader(range: StatsTimeRange, currentStreak: Int = 0): String {
    if (isEmpty()) return "Start your story"
    val now = LocalDate.now()
    val monthName = now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.uppercase() }
    return when {
        currentStreak >= 7 -> "On a $currentStreak-day streak"
        currentStreak >= 3 -> "You're on a run"
        range == StatsTimeRange.THIS_MONTH -> "Your $monthName so far"
        range == StatsTimeRange.THIS_YEAR -> "Your ${now.year} in games"
        range == StatsTimeRange.LAST_30 -> "The last 30 days"
        else -> {
            val recent = any { p ->
                runCatching { LocalDate.parse(p.date) >= now.minusDays(3) }.getOrDefault(false)
            }
            val totalPlays = sumOf { it.quantity.coerceAtLeast(1) }
            val years = mapNotNull { runCatching { LocalDate.parse(it.date).year }.getOrNull() }.toSet().size
            when {
                recent -> "Back at the table"
                totalPlays < 5 -> "Building your story"
                years >= 3 -> "$years years of gaming"
                else -> "Your gaming story"
            }
        }
    }
}

// ── Gamer archetype ───────────────────────────────────────────────────────────

enum class GamerArchetype(val title: String, val tagline: String) {
    DEDICANT("The Dedicant", "One game, understood deeply"),
    LOYALIST("The Loyalist", "Deep commitment to a handful of favorites"),
    MARATHONER("The Marathoner", "Long sessions. Full commitment."),
    SOCIALITE("The Socialite", "The more players, the better"),
    EXPLORER("The Explorer", "Always chasing the next new game"),
    CURATOR("The Curator", "Wide collection, deliberate exploration")
}

fun List<LoggedPlay>.computeGamerArchetype(): GamerArchetype? {
    val total = sumOf { it.quantity.coerceAtLeast(1) }
    if (total < 10) return null
    val uniqueGames = map { it.gameName }.toSet().size
    val gameCounts = groupBy { it.gameName }.mapValues { (_, g) -> g.sumOf { it.quantity.coerceAtLeast(1) } }
    val top1Share = (gameCounts.values.maxOrNull() ?: 0).toDouble() / total
    val top3Share = gameCounts.values.sortedDescending().take(3).sum().toDouble() / total
    val withDuration = filter { it.durationMinutes > 0 }
    val avgSession = if (withDuration.isNotEmpty()) withDuration.sumOf { it.durationMinutes }.toDouble() / withDuration.size else 0.0
    val avgPlayers = if (isNotEmpty()) sumOf { it.players.count { p -> p.name.isNotBlank() } }.toDouble() / size else 0.0
    val diversityRatio = if (total > 0) uniqueGames.toDouble() / total else 0.0
    return when {
        top1Share > 0.5 && total >= 15 -> GamerArchetype.DEDICANT
        top3Share > 0.70 -> GamerArchetype.LOYALIST
        avgSession > 150 -> GamerArchetype.MARATHONER
        avgPlayers > 4.0 -> GamerArchetype.SOCIALITE
        diversityRatio > 0.72 && uniqueGames >= 10 -> GamerArchetype.EXPLORER
        uniqueGames >= 20 && diversityRatio in 0.35..0.72 -> GamerArchetype.CURATOR
        else -> GamerArchetype.LOYALIST
    }
}

// ── 52-week play heatmap ──────────────────────────────────────────────────────

data class HeatmapDay(val date: LocalDate, val count: Int) // count = -1 for future
data class HeatmapWeek(val days: List<HeatmapDay>)
data class HeatmapData(
    val weeks: List<HeatmapWeek>,
    val maxCount: Int,
    val monthLabels: List<Pair<Int, String>> // weekIndex → abbreviated month name
)

fun List<LoggedPlay>.buildHeatmapData(weekCount: Int = 52): HeatmapData {
    val now = LocalDate.now()
    val countsByDate = groupBy { p -> runCatching { LocalDate.parse(p.date) }.getOrNull() }
        .filterKeys { it != null }
        .mapValues { (_, g) -> g.sumOf { it.quantity.coerceAtLeast(1) } }
        .mapKeys { it.key!! }

    val startMonday = now.minusWeeks(weekCount.toLong()).let { d ->
        d.minusDays((d.dayOfWeek.value - 1).toLong())
    }

    val allWeeks = mutableListOf<HeatmapWeek>()
    val monthLabels = mutableListOf<Pair<Int, String>>()
    var lastMonth = -1
    var weekStart = startMonday

    while (!weekStart.isAfter(now)) {
        val days = (0..6).map { d ->
            val day = weekStart.plusDays(d.toLong())
            HeatmapDay(day, if (!day.isAfter(now)) (countsByDate[day] ?: 0) else -1)
        }
        val weekIdx = allWeeks.size
        if (weekStart.monthValue != lastMonth) {
            monthLabels += weekIdx to weekStart.month
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                .replaceFirstChar { it.uppercase() }
            lastMonth = weekStart.monthValue
        }
        allWeeks += HeatmapWeek(days)
        weekStart = weekStart.plusWeeks(1)
    }

    val maxCount = allWeeks.flatMap { it.days }.filter { it.count >= 0 }.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    return HeatmapData(allWeeks, maxCount, monthLabels)
}

// ── On This Day ───────────────────────────────────────────────────────────────

data class OnThisDayEntry(val yearsAgo: Int, val year: Int, val plays: List<LoggedPlay>)

fun List<LoggedPlay>.buildOnThisDay(): List<OnThisDayEntry> {
    val today = LocalDate.now()
    return (1..3).mapNotNull { yearsAgo ->
        val target = runCatching { LocalDate.of(today.year - yearsAgo, today.month, today.dayOfMonth) }.getOrNull()
            ?: return@mapNotNull null
        val plays = filter { runCatching { LocalDate.parse(it.date) == target }.getOrDefault(false) }
        if (plays.isNotEmpty()) OnThisDayEntry(yearsAgo, today.year - yearsAgo, plays) else null
    }
}

// ── Rivalry pairs at the table ────────────────────────────────────────────────

data class RivalryPair(
    val playerA: String,
    val playerB: String,
    val playsCount: Int,
    val aWins: Int,
    val bWins: Int,
    val mostPlayedGame: String?,
    val narrativeLine: String
)

fun List<LoggedPlay>.buildTopRivalryPairs(limit: Int = 3): List<RivalryPair> {
    val pairPlays = mutableMapOf<String, Triple<Int, Int, Int>>()
    val pairGames = mutableMapOf<String, MutableMap<String, Int>>()

    forEach { play ->
        val active = play.players.filter { it.name.isNotBlank() }
        if (active.size < 2) return@forEach
        for (i in active.indices) {
            for (j in i + 1 until active.size) {
                val a = active[i]; val b = active[j]
                val sorted = listOf(a.name.trim(), b.name.trim()).sorted()
                val key = sorted.joinToString("|")
                val aIsFirst = a.name.trim() == sorted[0]
                val (t, aw, bw) = pairPlays[key] ?: Triple(0, 0, 0)
                pairPlays[key] = Triple(
                    t + 1,
                    aw + if (if (aIsFirst) a.isWinner else b.isWinner) 1 else 0,
                    bw + if (if (aIsFirst) b.isWinner else a.isWinner) 1 else 0
                )
                pairGames.getOrPut(key) { mutableMapOf() }
                    .also { it[play.gameName] = (it[play.gameName] ?: 0) + 1 }
            }
        }
    }

    return pairPlays.entries
        .filter { it.value.first >= 3 }
        .sortedByDescending { it.value.first }
        .take(limit)
        .map { (key, data) ->
            val names = key.split("|")
            val (together, aWins, bWins) = data
            val tied = aWins == bWins
            val leader = if (aWins >= bWins) names[0] else names[1]
            val lead = abs(aWins - bWins)
            val scoreStr = if (aWins >= bWins) "$aWins–$bWins" else "$bWins–$aWins"
            val mostGame = pairGames[key]?.maxByOrNull { it.value }?.key
            val narrative = when {
                together < 5 -> "${names[0]} vs ${names[1]} — $together games played so far"
                tied -> "${names[0]} and ${names[1]} are deadlocked — $aWins each after $together games"
                lead == 1 -> "$leader leads by a single game. $together games in."
                else -> "$leader leads $scoreStr after $together games together"
            }
            RivalryPair(names[0], names[1], together, aWins, bWins, mostGame, narrative)
        }
}
