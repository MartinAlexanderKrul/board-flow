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

    // Name parts of models that fail the scan or chronicle request. Checked against every model
    // a free-tier key lists (scripts/gemini_model_probe.py): these either reject image input or
    // JSON output, or - the Pro family - have no free-tier quota at all.
    private val SPECIALISED = setOf(
        "tts", "image", "embedding", "transcribe", "robotics", "computer",
        "customtools", "omni", "live", "audio", "native", "pro"
    )

    private val VERSION = Regex("""^gemini-(\d+)(?:\.(\d+))?-""")

    fun isUsable(model: String): Boolean =
        model.startsWith("gemini-") && model.split('-').none { it in SPECIALISED }

    /**
     * Stable before preview, Flash before Flash-Lite, "-latest" alias first, then newest version.
     * [preferLite] puts Flash-Lite first: it does not reason before answering, which makes it
     * several times faster and is plenty for a one-line chronicle.
     */
    fun rank(models: List<String>, preferLite: Boolean = false): List<String> =
        models.distinct().sortedWith(
            compareBy<String>(
                { isPreview(it) },
                { family(it).let { f -> if (preferLite && f <= 1) 1 - f else f } },
                { !it.endsWith("-latest") },
                { -version(it) },
                { it }
            )
        )

    /**
     * Models to try, in order. A pinned model goes first and the automatic order follows it as
     * fallback, so a pin that Google has retired still ends in a working scan.
     */
    fun candidates(pinned: String, available: List<String>, preferLite: Boolean = false): List<String> {
        val auto = rank((ALIASES + available).filter(::isUsable), preferLite)
        val pin = pinned.trim()
        return if (pin.isEmpty()) auto else listOf(pin) + (auto - pin)
    }

    /** True when a 400 body says the model itself cannot take the request (no image input, no JSON mode). */
    fun isModelRejection(errorBody: String): Boolean =
        errorBody.contains("is not enabled for") || errorBody.contains("is not supported by the model")

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
            "lite" in parts -> 1
            "flash" in parts -> 0
            else -> 2
        }
    }

    private fun version(model: String): Int {
        val match = VERSION.find(model) ?: return 0
        return match.groupValues[1].toInt() * 100 + (match.groupValues[2].toIntOrNull() ?: 0)
    }
}
