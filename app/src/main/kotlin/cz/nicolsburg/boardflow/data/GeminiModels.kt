package cz.nicolsburg.boardflow.data

/**
 * Decides which Gemini models to call and in what order.
 *
 * Google retires versioned model names every few months, so nothing here names one. The order
 * comes from the "-latest" aliases Google keeps pointing at the current models, plus whatever the
 * key's model list reports, ranked by family and version.
 */
object GeminiModels {

    /** Stored model preference meaning "let the app choose". */
    const val AUTO = ""

    // Always tried, even before the model list was ever fetched.
    private val ALIASES = listOf("gemini-flash-latest", "gemini-flash-lite-latest")

    // Name parts of models that cannot read a score sheet and answer in JSON.
    private val SPECIALISED = setOf(
        "tts", "image", "embedding", "transcribe", "robotics", "computer",
        "customtools", "omni", "live", "audio", "native"
    )

    private val VERSION = Regex("""^gemini-(\d+)(?:\.(\d+))?-""")

    fun isUsable(model: String): Boolean =
        model.startsWith("gemini-") && model.split('-').none { it in SPECIALISED }

    /** Stable before preview, Flash before Flash-Lite before Pro, "-latest" alias first, then newest version. */
    fun rank(models: List<String>): List<String> =
        models.distinct().sortedWith(
            compareBy<String>({ isPreview(it) }, { family(it) }, { !it.endsWith("-latest") }, { -version(it) }, { it })
        )

    /**
     * Models to try, in order. A pinned model goes first and the automatic order follows it as
     * fallback, so a pin that Google has retired still ends in a working scan.
     */
    fun candidates(pinned: String, available: List<String>): List<String> {
        val auto = rank((ALIASES + available).filter(::isUsable))
        val pin = pinned.trim()
        return if (pin.isEmpty()) auto else listOf(pin) + (auto - pin)
    }

    /** The model after [current] in [candidates]; the first one when [current] is not in the list. */
    fun next(current: String, candidates: List<String>, excluded: Set<String> = emptySet()): String? =
        candidates.drop(candidates.indexOf(current) + 1).firstOrNull { it != current && it !in excluded }

    private fun isPreview(model: String): Boolean {
        val parts = model.split('-')
        return "preview" in parts || "exp" in parts
    }

    private fun family(model: String): Int {
        val parts = model.split('-')
        return when {
            "pro" in parts -> 2
            "lite" in parts -> 1
            "flash" in parts -> 0
            else -> 3
        }
    }

    private fun version(model: String): Int {
        val match = VERSION.find(model) ?: return 0
        return match.groupValues[1].toInt() * 100 + (match.groupValues[2].toIntOrNull() ?: 0)
    }
}
