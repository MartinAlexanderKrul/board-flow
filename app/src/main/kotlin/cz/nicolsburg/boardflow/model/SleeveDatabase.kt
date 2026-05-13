package cz.nicolsburg.boardflow.model

import kotlin.math.abs

data class SleeveEntry(
    val genericName: String,
    val recommendedSize: String,
    val originalSizes: List<String>,
    val tlamaDiamond: String?,
    val paladin: String?,
    val ultraPro: String?,
    val sapphire: String?,
    val sleeveKings: String?,
    val arcaneTinmen: String?
) {
    /** All available (brand, productName) pairs in priority order. */
    val manufacturerOptions: List<Pair<String, String>> = buildList {
        tlamaDiamond?.let { add("TLAMA Diamond" to it) }
        paladin?.let       { add("Paladin" to it) }
        ultraPro?.let      { add("Ultra Pro" to it) }
        sapphire?.let      { add("Sapphire" to it) }
        sleeveKings?.let   { add("Sleeve Kings" to it) }
        arcaneTinmen?.let  { add("Arcane Tinmen" to it) }
    }

    /** Best available option by priority (TLAMA Diamond → Paladin → Ultra Pro → Sapphire → Sleeve Kings → Arcane Tinmen). */
    val preferred: Pair<String, String>? get() = manufacturerOptions.firstOrNull()
}

object SleeveDatabase {

    val entries: List<SleeveEntry> = listOf(
        SleeveEntry(
            genericName    = "Standard CCG",
            recommendedSize = "63.5 × 88 mm",
            originalSizes  = listOf("63×88","63.5×88","63.5×88.9","63×88.5","64×89","64×89.5"),
            tlamaDiamond   = "Diamond Green Standard 63.5×88",
            paladin        = "Paladin Percival 63.5×89",
            ultraPro       = "Ultra Pro Standard Deck Protector",
            sapphire       = "Sapphire Dark Green 63.5×88",
            sleeveKings    = "Sleeve Kings Standard Card Game 63.5×88",
            arcaneTinmen   = "Arcane Tinmen Standard 63.5×88"
        ),
        SleeveEntry(
            genericName    = "Mini Euro",
            recommendedSize = "44 × 68 mm",
            originalSizes  = listOf("44×68","44×67"),
            tlamaDiamond   = "Diamond Azure European Mini 45×68",
            paladin        = "Paladin Athos Mini Euro",
            ultraPro       = "Ultra Pro Mini European",
            sapphire       = "Sapphire Mini Euro 44×68",
            sleeveKings    = "Sleeve Kings Mini Euro 44×68",
            arcaneTinmen   = "Arcane Tinmen Small 44×68"
        ),
        SleeveEntry(
            genericName    = "Chimera Mini",
            recommendedSize = "45 × 68 mm",
            originalSizes  = listOf("45×67"),
            tlamaDiamond   = "Diamond Red Chimera Mini",
            paladin        = "Paladin Chimera Mini",
            ultraPro       = "Ultra Pro Mini Euro",
            sapphire       = "Sapphire Mini Euro",
            sleeveKings    = "Sleeve Kings Mini Euro",
            arcaneTinmen   = "Arcane Tinmen Mini Euro"
        ),
        SleeveEntry(
            genericName    = "American Standard",
            recommendedSize = "59 × 92 mm",
            originalSizes  = listOf("59×91","59×92","60×92"),
            tlamaDiamond   = "Diamond Blue European Standard 59×92",
            paladin        = "Paladin Gawain",
            ultraPro       = "Ultra Pro Standard American",
            sapphire       = "Sapphire American 59×92",
            sleeveKings    = "Sleeve Kings Standard American 59×92",
            arcaneTinmen   = "Arcane Tinmen Standard American"
        ),
        SleeveEntry(
            genericName    = "Chimera Standard",
            recommendedSize = "57.5 × 89 mm",
            originalSizes  = listOf("57×89","57×87"),
            tlamaDiamond   = "Diamond Orange Chimera Standard 57.5×89",
            paladin        = "Paladin Tristan",
            ultraPro       = "Ultra Pro Small Standard",
            sapphire       = "Sapphire Chimera",
            sleeveKings    = "Sleeve Kings Chimera 57.5×89",
            arcaneTinmen   = "Arcane Tinmen Chimera"
        ),
        SleeveEntry(
            genericName    = "American Mini",
            recommendedSize = "41 × 63 mm",
            originalSizes  = listOf("41×63","41×63.5"),
            tlamaDiamond   = "Diamond Yellow American Mini 41×63",
            paladin        = "Paladin American Mini",
            ultraPro       = "Ultra Pro Mini American",
            sapphire       = "Sapphire Mini American",
            sleeveKings    = "Sleeve Kings Mini American 41×63",
            arcaneTinmen   = "Arcane Tinmen Mini American"
        ),
        SleeveEntry(
            genericName    = "Square Small",
            recommendedSize = "70 × 70 mm",
            originalSizes  = listOf("70×70"),
            tlamaDiamond   = "Diamond Black Square 70×70",
            paladin        = "Paladin Square 70×70",
            ultraPro       = "Ultra Pro Square",
            sapphire       = "Sapphire Square",
            sleeveKings    = "Sleeve Kings Square",
            arcaneTinmen   = "Arcane Tinmen Square"
        ),
        SleeveEntry(
            genericName    = "Large Euro",
            recommendedSize = "65 × 100 mm",
            originalSizes  = listOf("65×100"),
            tlamaDiamond   = "Diamond Bronze 65×100",
            paladin        = "Paladin 65×100",
            ultraPro       = "Ultra Pro Large European",
            sapphire       = "Sapphire 65×100",
            sleeveKings    = "Sleeve Kings Large Euro",
            arcaneTinmen   = "Arcane Tinmen Large"
        ),
        SleeveEntry(
            genericName    = "Euro Large Narrow",
            recommendedSize = "55 × 100 mm",
            originalSizes  = listOf("55×100"),
            tlamaDiamond   = null,
            paladin        = "Paladin Euro Large",
            ultraPro       = "Ultra Pro Euro Large",
            sapphire       = "Sapphire Euro Large",
            sleeveKings    = "Sleeve Kings Euro Large",
            arcaneTinmen   = "Arcane Tinmen Euro Large"
        ),
        SleeveEntry(
            genericName    = "Scythe",
            recommendedSize = "70 × 110 mm",
            originalSizes  = listOf("70×110"),
            tlamaDiamond   = "Diamond Lime Scythe 70×110",
            paladin        = "Paladin Scythe",
            ultraPro       = "Ultra Pro Oversized",
            sapphire       = "Sapphire 70×110",
            sleeveKings    = "Sleeve Kings Scythe",
            arcaneTinmen   = "Arcane Tinmen Scythe"
        ),
        SleeveEntry(
            genericName    = "Tarot Narrow",
            recommendedSize = "70 × 120 mm",
            originalSizes  = listOf("70×120","69.78×120.25"),
            tlamaDiamond   = "Diamond Pink Tarot 70×120",
            paladin        = "Paladin Tarot",
            ultraPro       = "Ultra Pro Tarot",
            sapphire       = "Sapphire Tarot",
            sleeveKings    = "Sleeve Kings Tarot 70×120",
            arcaneTinmen   = "Arcane Tinmen Tarot"
        ),
        SleeveEntry(
            genericName    = "Large Tarot",
            recommendedSize = "80 × 120 mm",
            originalSizes  = listOf("79×120","80×120"),
            tlamaDiamond   = "Diamond Gold Dixit 80×120",
            paladin        = "Paladin Dixit",
            ultraPro       = "Ultra Pro Dixit",
            sapphire       = "Sapphire Dixit",
            sleeveKings    = "Sleeve Kings Dixit 80×120",
            arcaneTinmen   = "Arcane Tinmen Dixit"
        ),
        SleeveEntry(
            genericName    = "Tiny Epic",
            recommendedSize = "88 × 125 mm",
            originalSizes  = listOf("88×125","88×126"),
            tlamaDiamond   = "Diamond Grey Tiny Epic 88×125",
            paladin        = "Paladin Tiny Epic",
            ultraPro       = "Ultra Pro Oversized",
            sapphire       = "Sapphire Oversized",
            sleeveKings    = "Sleeve Kings Oversized",
            arcaneTinmen   = "Arcane Tinmen Oversized"
        ),
        SleeveEntry(
            genericName    = "Oversized XXL",
            recommendedSize = "101 × 126 mm",
            originalSizes  = listOf("101×126"),
            tlamaDiamond   = null,
            paladin        = "Paladin Oversized",
            ultraPro       = "Ultra Pro Oversized",
            sapphire       = "Sapphire Oversized",
            sleeveKings    = "Sleeve Kings XXL Oversized",
            arcaneTinmen   = "Arcane Tinmen Oversized"
        ),
        SleeveEntry(
            genericName    = "Square Large",
            recommendedSize = "102 × 102 mm",
            originalSizes  = listOf("102×102"),
            tlamaDiamond   = null,
            paladin        = "Paladin Square Large",
            ultraPro       = "Ultra Pro Square Large",
            sapphire       = "Sapphire Square Large",
            sleeveKings    = "Sleeve Kings Square Large",
            arcaneTinmen   = "Arcane Tinmen Square Large"
        ),
        SleeveEntry(
            genericName    = "Large Custom",
            recommendedSize = "83 × 113 mm",
            originalSizes  = listOf("83×113"),
            tlamaDiamond   = null,
            paladin        = "Paladin Custom Large",
            ultraPro       = "Ultra Pro Custom",
            sapphire       = "Sapphire Custom Large",
            sleeveKings    = "Sleeve Kings Custom Large",
            arcaneTinmen   = "Arcane Tinmen Custom"
        ),
        SleeveEntry(
            genericName    = "Square",
            recommendedSize = "63 × 63 mm",
            originalSizes  = listOf("63×63"),
            tlamaDiamond   = null,
            paladin        = "Paladin Square",
            ultraPro       = "Ultra Pro Square",
            sapphire       = "Sapphire Square",
            sleeveKings    = "Sleeve Kings Square",
            arcaneTinmen   = "Arcane Tinmen Square"
        )
    )

    /**
     * Matches a size string (e.g. "63.5 x 88 mm" or "63×88") against the database.
     * Uses ±0.6 mm tolerance on each dimension so minor BGG rounding variants still match.
     */
    fun findBySize(size: String): SleeveEntry? {
        val query = parseDimensions(size) ?: return null
        return entries.firstOrNull { entry ->
            matchesDim(entry.recommendedSize, query) ||
                entry.originalSizes.any { matchesDim(it, query) }
        }
    }

    private fun matchesDim(sizeStr: String, query: Pair<Float, Float>): Boolean {
        val d = parseDimensions(sizeStr) ?: return false
        return abs(d.first - query.first) < 0.6f && abs(d.second - query.second) < 0.6f
    }

    private val DIMENSION_RE = Regex("""(\d+\.?\d*)\s*[×xX]\s*(\d+\.?\d*)""")

    private fun parseDimensions(size: String): Pair<Float, Float>? {
        val m = DIMENSION_RE.find(size) ?: return null
        val w = m.groupValues[1].toFloatOrNull() ?: return null
        val h = m.groupValues[2].toFloatOrNull() ?: return null
        return w to h
    }
}
