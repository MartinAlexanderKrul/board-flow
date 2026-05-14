package cz.nicolsburg.boardflow.data

import android.util.Log
import cz.nicolsburg.boardflow.model.BggGame
import cz.nicolsburg.boardflow.model.ExtractedPlay
import cz.nicolsburg.boardflow.model.GameCandidate
import cz.nicolsburg.boardflow.model.GameItem
import cz.nicolsburg.boardflow.model.GameRecognitionHint

internal fun normalizeForRecognition(s: String): String =
    s.lowercase()
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

/**
 * Matches Gemini-detected game evidence against the local collection.
 *
 * Gemini output is treated as weak evidence, not ground truth. We score each
 * collection entry independently and return a ranked list so callers can decide
 * what confidence threshold to act on.
 *
 * Scoring signals:
 *  1. Title similarity  — up to 1.0; primary signal when Gemini detected a title.
 *  2. Category-name-in-game-name — up to 0.2 boost; catches trivially-named categories.
 *  3. Template category match — saved hint categories matched against detected categories.
 *     Requires ≥ 3 overlapping categories for a strong signal. Weight is inversely
 *     proportional to title strength so that a score sheet with no visible title can
 *     still be recognised from its category layout alone.
 *  4. Saved title match bonus — small boost (+0.15) when the detected title matches a
 *     title string stored in the hint from a prior confirmation.
 */
class GameRecognitionEngine {

    companion object {
        /** Minimum category overlap required for a strong template-based signal. */
        const val MIN_TEMPLATE_CATEGORY_OVERLAP = 3
        private const val TAG = "GameRecognition"
    }

    private data class ScoreResult(
        val score: Float,
        val reason: String,
        val primarySignal: String,
        val templateOverlap: Int
    )

    /**
     * Returns up to [maxResults] ranked candidates from [collection] that best
     * match the detection evidence in [extractedPlay]. Empty if there is no
     * evidence or no collection to search.
     */
    fun rankCandidates(
        extractedPlay: ExtractedPlay,
        collection: List<GameItem>,
        hints: List<GameRecognitionHint> = emptyList(),
        maxResults: Int = 5
    ): List<GameCandidate> {
        if (collection.isEmpty()) return emptyList()

        val title = extractedPlay.detectedGameTitle?.trim()
        val categories = extractedPlay.detectedScoringCategories

        if (title.isNullOrBlank() && categories.isEmpty()) return emptyList()

        val hintMap = hints.associateBy { it.gameObjectId }

        if (categories.isNotEmpty() && !title.isNullOrBlank()) {
            Log.d(TAG, "rankCandidates: title='$title' categories=${categories.size}:${categories.take(5).joinToString()}")
        } else if (!title.isNullOrBlank()) {
            Log.d(TAG, "rankCandidates: title='$title' (no categories)")
        } else {
            Log.d(TAG, "rankCandidates: no title, categories=${categories.size}:${categories.take(5).joinToString()} hints=${hintMap.size}")
        }

        return collection.mapNotNull { item ->
            val gameId = item.objectId.toIntOrNull() ?: return@mapNotNull null
            val bggGame = BggGame(
                id = gameId,
                name = item.name,
                yearPublished = item.yearPublished?.toString(),
                thumbnailUrl = item.thumbnailUrl
            )
            val hint = hintMap[item.objectId]
            scoreItem(item.name, title, categories, hint, item.numPlays ?: 0)?.let { result ->
                GameCandidate(
                    game = bggGame,
                    score = result.score,
                    matchReason = result.reason,
                    primarySignal = result.primarySignal,
                    templateOverlap = result.templateOverlap
                )
            }
        }
            .sortedByDescending { it.score }
            .take(maxResults)
    }

    private fun scoreItem(
        gameName: String,
        detectedTitle: String?,
        scoringCategories: List<String>,
        hint: GameRecognitionHint?,
        loggedPlayCount: Int
    ): ScoreResult? {
        var score = 0f
        val reasons = mutableListOf<String>()
        var titleSim = 0f
        var catBoostApplied = 0f
        var templateOverlap = 0
        var templateSignal = 0f
        var primarySignal = "none"

        // --- 1. Title similarity ---
        if (!detectedTitle.isNullOrBlank()) {
            titleSim = titleSimilarity(detectedTitle, gameName)
            if (titleSim >= 0.35f) {
                score += titleSim
                reasons += "title ${(titleSim * 100).toInt()}%"
                primarySignal = "title"
            }
        }

        // --- 2. Category-name-in-game-name (low-weight legacy signal) ---
        if (scoringCategories.isNotEmpty()) {
            val normalGameName = normalizeForRecognition(gameName)
            val hitCount = scoringCategories.count { cat ->
                val normalCat = normalizeForRecognition(cat)
                normalGameName.contains(normalCat) || normalCat.contains(normalGameName)
            }
            if (hitCount > 0) {
                catBoostApplied = (hitCount.toFloat() / scoringCategories.size) * 0.2f
                score += catBoostApplied
                reasons += "$hitCount category hint(s)"
            }
        }

        // --- 3. Template category matching (confirmed hint categories as scan template) ---
        // User-confirmed hints are trusted regardless of play count.
        if (hint != null && scoringCategories.isNotEmpty() && hint.normalizedCategories.isNotEmpty()) {
            val normDetectedCats = scoringCategories.map { normalizeForRecognition(it) }
            templateOverlap = normDetectedCats.count { hint.normalizedCategories.contains(it) }
            val templateSize = maxOf(normDetectedCats.size, hint.normalizedCategories.size)

            if (templateOverlap >= MIN_TEMPLATE_CATEGORY_OVERLAP) {
                // Strong match: weight scales inversely with title strength so category-only
                // sheets can still produce a high-confidence candidate.
                val templateWeight = when {
                    titleSim >= 0.70f -> 0.30f  // title strong, categories confirm
                    titleSim >= 0.35f -> 0.50f  // moderate title, categories add significant weight
                    else              -> 0.75f  // title absent/weak, categories are primary driver
                }
                val overlapRatio = templateOverlap.toFloat() / templateSize
                templateSignal = overlapRatio * templateWeight
                score += templateSignal
                if (titleSim < 0.35f) primarySignal = "category-template"
                reasons += "$templateOverlap/$templateSize template cats"
            } else if (templateOverlap > 0) {
                // Partial overlap: small contribution only, not strong enough to drive recognition
                val templateSize2 = maxOf(normDetectedCats.size, hint.normalizedCategories.size)
                templateSignal = (templateOverlap.toFloat() / templateSize2) * 0.10f
                score += templateSignal
                reasons += "$templateOverlap partial template cat(s)"
            }

            // --- 4. Saved title match bonus ---
            if (!detectedTitle.isNullOrBlank()) {
                val normTitle = normalizeForRecognition(detectedTitle)
                if (hint.normalizedTitles.any { it == normTitle }) {
                    score += 0.15f
                    reasons += "saved title"
                }
            }
        }

        if (score <= 0f) return null

        val finalScore = score.coerceIn(0f, 1f)
        Log.d(TAG, buildString {
            append("[$gameName] ")
            append("title=${(titleSim * 100).toInt()}% ")
            append("catBoost=${(catBoostApplied * 100).toInt()}% ")
            append("templateOverlap=$templateOverlap(need>=$MIN_TEMPLATE_CATEGORY_OVERLAP) ")
            append("templateSignal=${(templateSignal * 100).toInt()}% ")
            append("primarySignal=$primarySignal ")
            append("hint=${hint?.normalizedCategories?.size?.let { "saved($it cats, ${hint.timesConfirmed}x confirmed)" } ?: "none"} ")
            append("plays=$loggedPlayCount ")
            append("finalScore=${(finalScore * 100).toInt()}%")
            if (reasons.isNotEmpty()) append(" [${reasons.joinToString()}]")
        })

        return ScoreResult(
            score = finalScore,
            reason = reasons.joinToString(", "),
            primarySignal = primarySignal,
            templateOverlap = templateOverlap
        )
    }

    private fun titleSimilarity(a: String, b: String): Float {
        val na = normalizeForRecognition(a)
        val nb = normalizeForRecognition(b)
        if (na == nb) return 1f
        if (na.isBlank() || nb.isBlank()) return 0f

        val containment = when {
            nb.contains(na) -> na.length.toFloat() / nb.length
            na.contains(nb) -> nb.length.toFloat() / na.length
            else -> 0f
        }

        val dist = levenshtein(na, nb)
        val levSim = 1f - dist.toFloat() / maxOf(na.length, nb.length)

        return maxOf(levSim, containment)
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                else minOf(dp[i - 1][j - 1], dp[i - 1][j], dp[i][j - 1]) + 1
            }
        }
        return dp[a.length][b.length]
    }
}
