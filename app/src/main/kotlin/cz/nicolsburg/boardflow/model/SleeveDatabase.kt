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
            genericName     = "Standard CCG",
            recommendedSize = "63.5 × 88 mm",
            originalSizes   = listOf("63×88","63.5×88","63.5×88.9","63×88.5"),
            tlamaDiamond    = "Diamond Green Standard 63.5×88",
            paladin         = "Percival 63.5×89",
            ultraPro        = "Deck Protector Standard",
            sapphire        = "Dark Green 63.5×88",
            sleeveKings     = "Standard Card Game 63.5×88",
            arcaneTinmen    = "Standard Board Game 63.5×88"
        ),
        SleeveEntry(
            genericName     = "Loose-fit Standard",
            recommendedSize = "63.5 × 88 mm",
            originalSizes   = listOf("61×88","64×89","64×89.5","62.3×87.25"),
            tlamaDiamond    = "Diamond Green Standard 63.5×88",
            paladin         = "Percival 63.5×89",
            ultraPro        = "Deck Protector Standard",
            sapphire        = "Dark Green 63.5×88",
            sleeveKings     = "Standard Card Game 63.5×88",
            arcaneTinmen    = "Standard Board Game 63.5×88"
        ),
        SleeveEntry(
            genericName     = "Mini Euro",
            recommendedSize = "44 × 68 mm",
            originalSizes   = listOf("44×68","44×67","44×67.5"),
            tlamaDiamond    = "Diamond Azure European Mini 45×68",
            paladin         = "Athos Mini Euro",
            ultraPro        = "Mini European",
            sapphire        = "Mini Euro",
            sleeveKings     = "Mini Euro 44×68",
            arcaneTinmen    = "Small 44×68"
        ),
        SleeveEntry(
            genericName     = "Chimera Mini",
            recommendedSize = "45 × 68 mm",
            originalSizes   = listOf("45×67","43×67"),
            tlamaDiamond    = "Diamond Red Chimera Mini",
            paladin         = "Chimera Mini",
            ultraPro        = "Mini Euro",
            sapphire        = "Mini Euro",
            sleeveKings     = "Mini Chimera",
            arcaneTinmen    = "Mini Euro"
        ),
        SleeveEntry(
            genericName     = "Tiny Euro",
            recommendedSize = "44 × 63 mm",
            originalSizes   = listOf("44×63"),
            tlamaDiamond    = null,
            paladin         = "Tiny Euro",
            ultraPro        = "Tiny Euro",
            sapphire        = "Tiny Euro",
            sleeveKings     = "Mini Euro Short",
            arcaneTinmen    = "Tiny Euro"
        ),
        SleeveEntry(
            genericName     = "Standard American",
            recommendedSize = "59 × 92 mm",
            originalSizes   = listOf("59×91","59×92","60×92"),
            tlamaDiamond    = "Diamond Blue European Standard 59×92",
            paladin         = "Gawain",
            ultraPro        = "Standard American",
            sapphire        = "American 59×92",
            sleeveKings     = "Standard American",
            arcaneTinmen    = "Standard American"
        ),
        SleeveEntry(
            genericName     = "Chimera Standard",
            recommendedSize = "57.5 × 89 mm",
            originalSizes   = listOf("57×89","58×89","57×87","56×87","56.45×86.6","53×86"),
            tlamaDiamond    = "Diamond Orange Chimera Standard 57.5×89",
            paladin         = "Tristan",
            ultraPro        = "Small Standard",
            sapphire        = "Chimera",
            sleeveKings     = "Chimera",
            arcaneTinmen    = "Chimera"
        ),
        SleeveEntry(
            genericName     = "American Mini",
            recommendedSize = "41 × 63 mm",
            originalSizes   = listOf("41×63","41×63.5"),
            tlamaDiamond    = "Diamond Yellow American Mini 41×63",
            paladin         = "American Mini",
            ultraPro        = "Mini American",
            sapphire        = "Mini American",
            sleeveKings     = "Mini American",
            arcaneTinmen    = "Mini American"
        ),
        SleeveEntry(
            genericName     = "Medium Square",
            recommendedSize = "67 × 67 mm",
            originalSizes   = listOf("67×67","65×65"),
            tlamaDiamond    = null,
            paladin         = "Medium Square",
            ultraPro        = "Medium Square",
            sapphire        = "Medium Square",
            sleeveKings     = "Medium Square",
            arcaneTinmen    = "Medium Square"
        ),
        SleeveEntry(
            genericName     = "Large Square",
            recommendedSize = "70 × 70 mm",
            originalSizes   = listOf("70×70","72×72"),
            tlamaDiamond    = "Diamond Black Square 70×70",
            paladin         = "Square 70×70",
            ultraPro        = "Square",
            sapphire        = "Square",
            sleeveKings     = "Square",
            arcaneTinmen    = "Square"
        ),
        SleeveEntry(
            genericName     = "Small Square",
            recommendedSize = "63 × 63 mm",
            originalSizes   = listOf("63×63","62×62"),
            tlamaDiamond    = null,
            paladin         = "Square",
            ultraPro        = "Square",
            sapphire        = "Square",
            sleeveKings     = "Square",
            arcaneTinmen    = "Square"
        ),
        SleeveEntry(
            genericName     = "Wyrmspan Cave",
            recommendedSize = "57 × 57 mm",
            originalSizes   = listOf("57×57"),
            tlamaDiamond    = null,
            paladin         = "Square Mini",
            ultraPro        = "Square Mini",
            sapphire        = "Square Mini",
            sleeveKings     = "Square Mini",
            arcaneTinmen    = "Square Mini"
        ),
        SleeveEntry(
            genericName     = "Large Euro / 7 Wonders",
            recommendedSize = "65 × 100 mm",
            originalSizes   = listOf("65×100","67.5×100"),
            tlamaDiamond    = "Diamond Bronze 65×100",
            paladin         = "Merlin 65×100",
            ultraPro        = "Large European",
            sapphire        = "65×100",
            sleeveKings     = "Large Euro",
            arcaneTinmen    = "Large"
        ),
        SleeveEntry(
            genericName     = "Euro Large Narrow",
            recommendedSize = "55 × 100 mm",
            originalSizes   = listOf("55×100"),
            tlamaDiamond    = null,
            paladin         = "Lancelot",
            ultraPro        = "Euro Large",
            sapphire        = "Euro Large",
            sleeveKings     = "Euro Large",
            arcaneTinmen    = "Euro Large"
        ),
        SleeveEntry(
            genericName     = "Tall Narrow",
            recommendedSize = "61 × 112 mm",
            originalSizes   = listOf("61×112"),
            tlamaDiamond    = null,
            paladin         = "Tall Tarot",
            ultraPro        = "Tall Tarot",
            sapphire        = "Tall Tarot",
            sleeveKings     = "Tall Tarot",
            arcaneTinmen    = "Tall Tarot"
        ),
        SleeveEntry(
            genericName     = "Tarot Narrow",
            recommendedSize = "70 × 120 mm",
            originalSizes   = listOf("70×120","69.78×120.25"),
            tlamaDiamond    = "Diamond Pink Tarot 70×120",
            paladin         = "Guinevere",
            ultraPro        = "Tarot",
            sapphire        = "Tarot",
            sleeveKings     = "Tarot 70×120",
            arcaneTinmen    = "Tarot"
        ),
        SleeveEntry(
            genericName     = "War of the Ring",
            recommendedSize = "67 × 120 mm",
            originalSizes   = listOf("67×120"),
            tlamaDiamond    = null,
            paladin         = "War of the Ring",
            ultraPro        = "War of the Ring",
            sapphire        = "War of the Ring",
            sleeveKings     = "War of the Ring",
            arcaneTinmen    = "War of the Ring"
        ),
        SleeveEntry(
            genericName     = "Dixit / Large Tarot",
            recommendedSize = "80 × 120 mm",
            originalSizes   = listOf("79×120","80×120"),
            tlamaDiamond    = "Diamond Gold Dixit 80×120",
            paladin         = "Dixit",
            ultraPro        = "Dixit",
            sapphire        = "Dixit",
            sleeveKings     = "Dixit",
            arcaneTinmen    = "Dixit"
        ),
        SleeveEntry(
            genericName     = "Scythe",
            recommendedSize = "70 × 110 mm",
            originalSizes   = listOf("70×110"),
            tlamaDiamond    = "Diamond Lime Scythe 70×110",
            paladin         = "Scythe",
            ultraPro        = "Oversized",
            sapphire        = "70×110",
            sleeveKings     = "Scythe",
            arcaneTinmen    = "Scythe"
        ),
        SleeveEntry(
            genericName     = "Tiny Epic",
            recommendedSize = "88 × 126 mm",
            originalSizes   = listOf("88×126","87×127"),
            tlamaDiamond    = "Diamond Grey Tiny Epic 88×125",
            paladin         = "Tiny Epic",
            ultraPro        = "Oversized",
            sapphire        = "Oversized",
            sleeveKings     = "Tiny Epic Oversized",
            arcaneTinmen    = "Oversized"
        ),
        SleeveEntry(
            genericName     = "Oversized Portrait",
            recommendedSize = "102 × 146 mm",
            originalSizes   = listOf("102×146"),
            tlamaDiamond    = null,
            paladin         = "Mordred",
            ultraPro        = "Oversized Portrait",
            sapphire        = "Oversized Portrait",
            sleeveKings     = "Oversized Portrait",
            arcaneTinmen    = "Oversized Portrait"
        ),
        SleeveEntry(
            genericName     = "Oversized XXL",
            recommendedSize = "101 × 126 mm",
            originalSizes   = listOf("101×126","103×128"),
            tlamaDiamond    = null,
            paladin         = "Oversized",
            ultraPro        = "Oversized",
            sapphire        = "Oversized",
            sleeveKings     = "XXL Oversized",
            arcaneTinmen    = "Oversized"
        ),
        SleeveEntry(
            genericName     = "Giant Oversized",
            recommendedSize = "127 × 159 mm",
            originalSizes   = listOf("127×159"),
            tlamaDiamond    = null,
            paladin         = "Giant Oversized",
            ultraPro        = "Giant Oversized",
            sapphire        = "Giant Oversized",
            sleeveKings     = "Giant Oversized",
            arcaneTinmen    = "Giant Oversized"
        ),
        SleeveEntry(
            genericName     = "Photo / Tarot XL",
            recommendedSize = "100 × 152 mm",
            originalSizes   = listOf("100×152","101.6×152.4"),
            tlamaDiamond    = null,
            paladin         = "Morgana",
            ultraPro        = "Photo Large",
            sapphire        = "Photo Large",
            sleeveKings     = "Photo Large",
            arcaneTinmen    = "Photo Large"
        ),
        SleeveEntry(
            genericName     = "Square Large",
            recommendedSize = "102 × 102 mm",
            originalSizes   = listOf("102×102"),
            tlamaDiamond    = null,
            paladin         = "Square Large",
            ultraPro        = "Square Large",
            sapphire        = "Square Large",
            sleeveKings     = "Square Large",
            arcaneTinmen    = "Square Large"
        ),
        SleeveEntry(
            genericName     = "Medium Square Large",
            recommendedSize = "95 × 95 mm",
            originalSizes   = listOf("95×95"),
            tlamaDiamond    = null,
            paladin         = "Square Medium",
            ultraPro        = "Square Medium",
            sapphire        = "Square Medium",
            sleeveKings     = "Square Medium",
            arcaneTinmen    = "Square Medium"
        ),
        SleeveEntry(
            genericName     = "Large Custom",
            recommendedSize = "83 × 113 mm",
            originalSizes   = listOf("83×113"),
            tlamaDiamond    = null,
            paladin         = "Custom Large",
            ultraPro        = "Custom",
            sapphire        = "Custom Large",
            sleeveKings     = "Custom Large",
            arcaneTinmen    = "Custom Large"
        ),
        SleeveEntry(
            genericName     = "Landscape Tarot",
            recommendedSize = "102 × 76 mm",
            originalSizes   = listOf("102×76"),
            tlamaDiamond    = null,
            paladin         = "Tarot Landscape",
            ultraPro        = "Tarot Landscape",
            sapphire        = "Tarot Landscape",
            sleeveKings     = "Tarot Landscape",
            arcaneTinmen    = "Tarot Landscape"
        ),
        SleeveEntry(
            genericName     = "Portrait Tarot",
            recommendedSize = "76 × 102 mm",
            originalSizes   = listOf("76×102"),
            tlamaDiamond    = null,
            paladin         = "Tarot Portrait",
            ultraPro        = "Tarot Portrait",
            sapphire        = "Tarot Portrait",
            sleeveKings     = "Tarot Portrait",
            arcaneTinmen    = "Tarot Portrait"
        ),
        SleeveEntry(
            genericName     = "Large Square Premium",
            recommendedSize = "76 × 76 mm",
            originalSizes   = listOf("76×76"),
            tlamaDiamond    = null,
            paladin         = "Square Premium",
            ultraPro        = "Square Premium",
            sapphire        = "Square Premium",
            sleeveKings     = "Square Premium",
            arcaneTinmen    = "Square Premium"
        ),
        SleeveEntry(
            genericName     = "Small Custom",
            recommendedSize = "50 × 75 mm",
            originalSizes   = listOf("50×75","49×72.5"),
            tlamaDiamond    = null,
            paladin         = "Small Custom",
            ultraPro        = "Small Custom",
            sapphire        = "Small Custom",
            sleeveKings     = "Small Custom",
            arcaneTinmen    = "Small Custom"
        ),
        SleeveEntry(
            genericName     = "Tiny Custom",
            recommendedSize = "53 × 63 mm",
            originalSizes   = listOf("53×63"),
            tlamaDiamond    = null,
            paladin         = "Tiny Custom",
            ultraPro        = "Tiny Custom",
            sapphire        = "Tiny Custom",
            sleeveKings     = "Tiny Custom",
            arcaneTinmen    = "Tiny Custom"
        ),
        SleeveEntry(
            genericName     = "Narrow Tall",
            recommendedSize = "62 × 103.5 mm",
            originalSizes   = listOf("62×103.5"),
            tlamaDiamond    = null,
            paladin         = "Narrow Tall",
            ultraPro        = "Narrow Tall",
            sapphire        = "Narrow Tall",
            sleeveKings     = "Narrow Tall",
            arcaneTinmen    = "Narrow Tall"
        ),
        SleeveEntry(
            genericName     = "Small Tarot",
            recommendedSize = "62 × 79 mm",
            originalSizes   = listOf("62×79"),
            tlamaDiamond    = null,
            paladin         = "Small Tarot",
            ultraPro        = "Small Tarot",
            sapphire        = "Small Tarot",
            sleeveKings     = "Small Tarot",
            arcaneTinmen    = "Small Tarot"
        ),
        SleeveEntry(
            genericName     = "Small Landscape",
            recommendedSize = "58 × 75 mm",
            originalSizes   = listOf("58×75"),
            tlamaDiamond    = null,
            paladin         = "Small Landscape",
            ultraPro        = "Small Landscape",
            sapphire        = "Small Landscape",
            sleeveKings     = "Small Landscape",
            arcaneTinmen    = "Small Landscape"
        ),
        SleeveEntry(
            genericName     = "LOTR Duel",
            recommendedSize = "54 × 80 mm",
            originalSizes   = listOf("54×80"),
            tlamaDiamond    = null,
            paladin         = "LOTR Duel",
            ultraPro        = "LOTR Duel",
            sapphire        = "LOTR Duel",
            sleeveKings     = "LOTR Duel",
            arcaneTinmen    = "LOTR Duel"
        ),
        SleeveEntry(
            genericName     = "Large Board Cards",
            recommendedSize = "91 × 141 mm",
            originalSizes   = listOf("91×141","90×130"),
            tlamaDiamond    = null,
            paladin         = "Oversized Large",
            ultraPro        = "Oversized Large",
            sapphire        = "Oversized Large",
            sleeveKings     = "Oversized Large",
            arcaneTinmen    = "Oversized Large"
        ),
        SleeveEntry(
            genericName     = "Spirit Island Panels",
            recommendedSize = "230 × 152 mm",
            originalSizes   = listOf("230×152"),
            tlamaDiamond    = null,
            paladin         = null,
            ultraPro        = null,
            sapphire        = null,
            sleeveKings     = null,
            arcaneTinmen    = null
        ),
        SleeveEntry(
            genericName     = "Wonder Boards",
            recommendedSize = "110 × 250 mm",
            originalSizes   = listOf("110×250"),
            tlamaDiamond    = null,
            paladin         = null,
            ultraPro        = null,
            sapphire        = null,
            sleeveKings     = null,
            arcaneTinmen    = null
        ),
        SleeveEntry(
            genericName     = "Wingspan Goal Board",
            recommendedSize = "150 × 120 mm",
            originalSizes   = listOf("150×120"),
            tlamaDiamond    = null,
            paladin         = null,
            ultraPro        = null,
            sapphire        = null,
            sleeveKings     = null,
            arcaneTinmen    = null
        ),
        SleeveEntry(
            genericName     = "WotR Oversized",
            recommendedSize = "120 × 134 mm",
            originalSizes   = listOf("120×134"),
            tlamaDiamond    = null,
            paladin         = "WotR Oversized",
            ultraPro        = "WotR Oversized",
            sapphire        = "WotR Oversized",
            sleeveKings     = "WotR Oversized",
            arcaneTinmen    = "WotR Oversized"
        )
    )

    /**
     * Matches a size string (e.g. "63.5 x 88 mm" or "63×88") against the database.
     * Uses ±0.6 mm tolerance on each dimension so minor BGG rounding variants still match.
     */
    fun findBySize(size: String): SleeveEntry? {
        val query = parseDimensions(size) ?: return null
        fun match(q: Pair<Float, Float>) = entries.firstOrNull { entry ->
            matchesDim(entry.recommendedSize, q) ||
                entry.originalSizes.any { matchesDim(it, q) }
        }
        return match(query) ?: match(query.second to query.first)
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
