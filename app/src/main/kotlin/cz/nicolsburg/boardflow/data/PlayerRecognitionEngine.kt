package cz.nicolsburg.boardflow.data

import android.util.Log
import cz.nicolsburg.boardflow.model.Player
import cz.nicolsburg.boardflow.model.PlayerRecognitionHint

data class PlayerMatch(
    val player: Player,
    val confidence: Float,
    val source: String
)

object PlayerRecognitionEngine {

    private const val TAG = "PlayerRecognition"

    /**
     * Resolve a scanned player name against the roster using hints, then aliases, then fuzzy.
     * Fuzzy matches are returned with low confidence so callers can decide whether to auto-apply.
     */
    fun resolve(
        scannedName: String,
        roster: List<Player>,
        hints: List<PlayerRecognitionHint>
    ): PlayerMatch? {
        if (scannedName.isBlank() || roster.isEmpty()) return null
        val normalized = normalizeForRecognition(scannedName)
        val lower = scannedName.lowercase().trim()

        // 1. Saved scan-player hints
        val relevantHints = hints
            .filter { it.scannedNameNormalized == normalized }
            .sortedByDescending { it.timesConfirmed }
        if (relevantHints.isNotEmpty()) {
            val best = relevantHints.first()
            val player = roster.firstOrNull { it.id == best.confirmedRosterPlayerId }
            if (player != null) {
                val second = relevantHints.getOrNull(1)
                val ambiguous = second != null &&
                    second.confirmedRosterPlayerId != best.confirmedRosterPlayerId &&
                    second.timesConfirmed * 2 >= best.timesConfirmed
                val confidence = if (ambiguous) 0.55f
                else minOf(0.95f, 0.70f + best.timesConfirmed * 0.05f)
                Log.d(TAG, "hint '$scannedName' -> '${player.displayName}' conf=$confidence ambiguous=$ambiguous")
                return PlayerMatch(player, confidence, "hint")
            }
        }

        // 2. Exact alias / display name
        val exactMatch = roster.firstOrNull { p ->
            (listOf(p.displayName) + p.aliases).any { it.lowercase().trim() == lower }
        }
        if (exactMatch != null) {
            Log.d(TAG, "alias '$scannedName' -> '${exactMatch.displayName}'")
            return PlayerMatch(exactMatch, 1.0f, "alias")
        }

        // 3. Fuzzy (Levenshtein) — returned but not auto-applied by callers
        val threshold = maxOf(2, lower.length / 3)
        data class FR(val player: Player, val dist: Int)
        val fuzzy = roster.mapNotNull { p ->
            val dist = (listOf(p.displayName) + p.aliases).minOf { levenshteinDistance(lower, it.lowercase().trim()) }
            if (dist <= threshold) FR(p, dist) else null
        }.sortedBy { it.dist }
        if (fuzzy.isNotEmpty()) {
            val (p, dist) = fuzzy.first()
            val confidence = 1f - dist.toFloat() / (lower.length + 1)
            Log.d(TAG, "fuzzy '$scannedName' -> '${p.displayName}' dist=$dist conf=$confidence")
            return PlayerMatch(p, confidence, "fuzzy")
        }

        Log.d(TAG, "no match '$scannedName'")
        return null
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length; val n = b.length
        val dp = Array(m + 1) { i -> IntArray(n + 1) { j -> if (i == 0) j else if (j == 0) i else 0 } }
        for (i in 1..m) for (j in 1..n) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
        return dp[m][n]
    }
}
