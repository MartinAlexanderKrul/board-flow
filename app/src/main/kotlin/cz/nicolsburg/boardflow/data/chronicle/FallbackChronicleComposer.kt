package cz.nicolsburg.boardflow.data.chronicle

import java.util.Locale
import kotlin.math.abs

class FallbackChronicleComposer {

    fun compose(request: ChronicleRequest, sourceKey: String): String {
        val quote = sanitizeQuote(request.quote)
        val mood = request.moods.firstOrNull { it.isNotBlank() }.orEmpty()
        val tone = moodDescriptor(mood)
        val colorHint = request.playerColors.firstOrNull { it.isNotBlank() }?.lowercase(Locale.getDefault()).orEmpty()

        val candidates = buildList {
            if (quote.isNotBlank() && tone.isNotBlank()) {
                add("\"$quote\" lingered over a table that never quite settled.")
                add("\"$quote\" became the line that held the whole evening together.")
                add("A $tone current ran through the table, and \"$quote\" caught it perfectly.")
            }
            if (quote.isNotBlank()) {
                add("\"$quote\" became the line the table kept.")
                add("The table kept coming back to \"$quote\".")
                add("\"$quote\" was what the night left behind.")
            }
            if (tone.isNotBlank()) {
                add("The table held a $tone feeling that stayed long after it ended.")
                add("Something $tone lingered at the table by the end of the night.")
                add("The night left behind a distinctly $tone glow.")
            }
            if (colorHint.isNotBlank()) {
                add("Even the $colorHint around the table felt part of the mood.")
            }
            add("The table left behind a memory worth keeping.")
        }

        val choice = abs(sourceKey.hashCode()) % candidates.size
        return truncateChronicle(candidates[choice])
    }

    private fun sanitizeQuote(raw: String): String =
        raw.trim()
            .trim('"', '\'', '“', '”')
            .replace(Regex("\\s+"), " ")
            .take(54)
            .trim()
            .trimEnd('.', '!', '?', ',', ';', ':')

    private fun truncateChronicle(raw: String): String {
        val clean = raw.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= MAX_CHRONICLE_LENGTH) return clean
        val slice = clean.take(MAX_CHRONICLE_LENGTH - 3)
        val shortened = slice.substringBeforeLast(' ').ifBlank { slice }
        return shortened.trimEnd('.', ',', ';', ':') + "..."
    }

    private fun moodDescriptor(mood: String): String {
        return when (mood.lowercase(Locale.getDefault()).trim()) {
            "chaotic" -> "restless"
            "chill" -> "easy"
            "intense" -> "charged"
            "cozy" -> "close"
            "competitive" -> "sharp"
            "legendary" -> "electric"
            else -> "felt"
        }
    }

    companion object {
        private const val MAX_CHRONICLE_LENGTH = 110
    }
}
