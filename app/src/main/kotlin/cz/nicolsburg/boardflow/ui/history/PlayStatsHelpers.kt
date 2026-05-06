package cz.nicolsburg.boardflow.ui.history

import cz.nicolsburg.boardflow.model.LoggedPlay
import cz.nicolsburg.boardflow.model.Player
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class StatsTimeRange(val label: String) {
    ALL("All time"),
    THIS_YEAR("This year"),
    THIS_MONTH("This month"),
    LAST_30("Last 30 d")
}

fun List<LoggedPlay>.filterByTimeRange(range: StatsTimeRange): List<LoggedPlay> {
    if (range == StatsTimeRange.ALL) return this
    val now = LocalDate.now()
    val cutoff = when (range) {
        StatsTimeRange.THIS_YEAR  -> LocalDate.of(now.year, 1, 1)
        StatsTimeRange.THIS_MONTH -> LocalDate.of(now.year, now.monthValue, 1)
        StatsTimeRange.LAST_30    -> now.minusDays(30)
        StatsTimeRange.ALL        -> return this
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
    val commonPlayers: List<String>
)

fun List<LoggedPlay>.gameHistoryStats(gameName: String): GameHistoryStats? {
    val gamePlays = filter { it.gameName.trim().equals(gameName.trim(), ignoreCase = true) }
        .filter { it.nowInStats }
    if (gamePlays.isEmpty()) return null

    val plays = gamePlays.sumOf { it.quantity.coerceAtLeast(1) }
    val lastDate = gamePlays.maxOfOrNull { it.date }?.let {
        runCatching {
            LocalDate.parse(it).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }.getOrDefault(it)
    }

    val withDuration = gamePlays.filter { it.durationMinutes > 0 }
    val avgDuration = if (withDuration.isEmpty()) null
        else withDuration.sumOf { it.durationMinutes } / withDuration.size

    var bestScore: Pair<String, Int>? = null
    gamePlays.forEach { play ->
        play.players.forEach { pr ->
            val score = pr.score.trim().toIntOrNull()
            if (score != null && pr.name.isNotBlank()) {
                if (bestScore == null || score > bestScore!!.second) {
                    bestScore = pr.name.trim() to score
                }
            }
        }
    }

    val winCounts = mutableMapOf<String, Int>()
    gamePlays.forEach { play ->
        play.players.filter { it.isWinner && it.name.isNotBlank() }.forEach { pr ->
            val name = pr.name.trim()
            winCounts[name] = (winCounts[name] ?: 0) + 1
        }
    }
    val mostWins = winCounts.maxByOrNull { it.value }?.let { it.key to it.value }

    val playerCounts = mutableMapOf<String, Int>()
    gamePlays.forEach { play ->
        play.players.filter { it.name.isNotBlank() }.forEach { pr ->
            val name = pr.name.trim()
            playerCounts[name] = (playerCounts[name] ?: 0) + 1
        }
    }
    val commonPlayers = playerCounts.entries.sortedByDescending { it.value }.take(4).map { it.key }

    return GameHistoryStats(plays, lastDate, avgDuration, bestScore, mostWins, commonPlayers)
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
            .filter { pr -> pr.name.isNotBlank() && pr.name.lowercase().trim() !in playerNames }
            .forEach { opp ->
                val key = opp.name.trim()
                val (together, myW, theirW) = opponentMap[key] ?: Triple(0, 0, 0)
                opponentMap[key] = Triple(
                    together + 1,
                    myW + if (iWon) 1 else 0,
                    theirW + if (opp.isWinner) 1 else 0
                )
            }
    }

    return opponentMap.entries
        .sortedByDescending { it.value.first }
        .take(limit)
        .map { (name, t) -> RivalryStat(name, t.first, t.second, t.third) }
}

/** Current consecutive-win streak for a player across all games, ordered by date desc. */
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

/** Name + streak for the player currently on the hottest win streak (≥2), or null. */
fun List<LoggedPlay>.hotPlayerStreak(roster: List<Player>): Pair<String, Int>? =
    roster.map { player ->
        val names = (listOf(player.displayName) + player.aliases).map { it.lowercase().trim() }
        player.displayName to playerCurrentWinStreak(names)
    }.filter { it.second >= 2 }.maxByOrNull { it.second }

/** The most-played game this calendar month and its play count, or null if no plays this month. */
fun List<LoggedPlay>.mostPlayedThisMonth(): Pair<String, Int>? {
    val now = LocalDate.now()
    val thisMonth = now.year * 100 + now.monthValue
    return filter { play ->
        runCatching {
            LocalDate.parse(play.date).let { it.year * 100 + it.monthValue } == thisMonth
        }.getOrDefault(false)
    }.groupBy { it.gameName }
        .mapValues { (_, g) -> g.sumOf { it.quantity.coerceAtLeast(1) } }
        .maxByOrNull { it.value }
        ?.let { it.key to it.value }
}

fun List<LoggedPlay>.playInsights(play: LoggedPlay): List<String> {
    val gamePlays = filter {
        it.gameName.trim().equals(play.gameName.trim(), ignoreCase = true) && it.nowInStats
    }
    val result = mutableListOf<String>()

    if (gamePlays.size == 1 && gamePlays[0].id == play.id) {
        result.add("First time playing ${play.gameName}!")
        return result
    }

    play.players.filter { it.isWinner }.forEach { winner ->
        if (result.size >= 2) return result
        val name = winner.name.trim().ifBlank { return@forEach }
        val hadPrevWin = gamePlays.any { p ->
            p.id != play.id &&
            p.players.any { it.name.trim().equals(name, ignoreCase = true) && it.isWinner }
        }
        if (!hadPrevWin) result.add("First win for $name in ${play.gameName}!")
    }

    if (result.size < 2) {
        play.players.forEach { pr ->
            if (result.size >= 2) return result
            val name = pr.name.trim().ifBlank { return@forEach }
            val score = pr.score.trim().toIntOrNull() ?: return@forEach
            val prevBest = gamePlays
                .filter { it.id != play.id }
                .flatMap { it.players }
                .filter { it.name.trim().equals(name, ignoreCase = true) }
                .mapNotNull { it.score.trim().toIntOrNull() }
                .maxOrNull()
            if (prevBest == null || score > prevBest) {
                result.add("New high score for $name: $score pts!")
            }
        }
    }

    if (result.size < 2 && play.durationMinutes > 0) {
        val prevMax = gamePlays.filter { it.id != play.id && it.durationMinutes > 0 }
            .maxOfOrNull { it.durationMinutes }
        if (prevMax == null || play.durationMinutes > prevMax) {
            val label = if (play.durationMinutes >= 60)
                "${play.durationMinutes / 60}h ${play.durationMinutes % 60}m"
            else "${play.durationMinutes}m"
            result.add("Longest session of ${play.gameName} ($label)!")
        }
    }

    return result
}
