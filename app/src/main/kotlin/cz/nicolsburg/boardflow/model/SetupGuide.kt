package cz.nicolsburg.boardflow.model

/**
 * A Quick Setup cheat sheet for one base game: box -> table -> first turn.
 *
 * Guides are portable JSON documents (see `setup-guides/README.md`). The same document shape is
 * bundled in the APK, served by the remote catalog, stored in Room and (later) produced by users
 * or AI drafts. Steps are one ordered list filtered by [StepCondition]s - there is no patching.
 */
data class SetupGuide(
    val schemaVersion: Int,
    val gameId: Int,
    val aliasGameIds: List<Int> = emptyList(),
    val gameName: String,
    val version: Int,
    val provenance: GuideProvenance = GuideProvenance(),
    val players: PlayerRange? = null,
    val modules: List<GuideModule> = emptyList(),
    val sections: List<GuideSection> = emptyList()
) {
    /** Every BGG id that should open this guide: the base game, its aliases and module ids. */
    val allGameIds: Set<Int>
        get() = buildSet {
            add(gameId)
            addAll(aliasGameIds)
            modules.mapNotNullTo(this) { it.bggId }
        }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

/** [exclude] lists counts inside the range the game doesn't support (e.g. Uprising: 1-4 or 6). */
data class PlayerRange(val min: Int, val max: Int, val exclude: Set<Int> = emptySet())

enum class GuideOrigin { BOARDFLOW, COMMUNITY, USER, AI_DRAFT }

data class GuideProvenance(
    val origin: GuideOrigin = GuideOrigin.BOARDFLOW,
    val author: String? = null,
    val sources: List<String> = emptyList(),
    val basedOnVersion: Int? = null,
    val aiModel: String? = null,
    val reviewed: Boolean = true
)

data class GuideModule(
    val id: String,
    val name: String,
    val bggId: Int? = null,
    val defaultEnabled: Boolean = false,
    /** Player counts this module can be used at; null means the guide's full range. */
    val players: PlayerRange? = null,
    /** Raises the selectable player count when enabled (e.g. a 5-6 player expansion). */
    val extendsMaxPlayers: Int? = null,
    /** Player counts at which this module is always on (e.g. co-op mode for solo). */
    val forcedAtPlayers: Set<Int> = emptySet(),
    val requires: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
    val note: String? = null,
    /**
     * Modules sharing a group are a single choice (e.g. "Mode": Competitive / Co-op): exactly one
     * available member is always on. The group name is shown as the picker label.
     */
    val group: String? = null
)

enum class GuideSectionKind { SETUP, REMINDERS, START }

data class GuideSection(
    val id: String,
    val title: String,
    val kind: GuideSectionKind = GuideSectionKind.SETUP,
    val condition: StepCondition? = null,
    val steps: List<GuideStep> = emptyList()
)

data class GuideStep(
    val id: String,
    /** Short instruction; `{name}` placeholders are filled from [amounts]. */
    val text: String,
    val amounts: Map<String, AmountRule> = emptyMap(),
    val condition: StepCondition? = null,
    val note: String? = null,
    /** Optional rulebook reference (e.g. "Pearlbrook p.3") for checking a guide against its source. */
    val ref: String? = null
)

/**
 * Resolves a placeholder value. The first matching [cases] entry wins, then [byPlayers], then [default].
 * Values are strings so they can hold "5, 6, 7" style per-seat lists as well as plain numbers.
 */
data class AmountRule(
    val default: String? = null,
    val byPlayers: Map<Int, String> = emptyMap(),
    val cases: List<AmountCase> = emptyList()
)

data class AmountCase(val condition: StepCondition, val value: String)

/** All present constraints must hold (AND). Express OR as two steps. */
data class StepCondition(
    val players: Set<Int>? = null,
    val minPlayers: Int? = null,
    val maxPlayers: Int? = null,
    val modules: List<String> = emptyList(),
    val notModules: List<String> = emptyList()
) {
    fun matches(playerCount: Int, enabledModules: Set<String>): Boolean {
        if (players != null && playerCount !in players) return false
        if (minPlayers != null && playerCount < minPlayers) return false
        if (maxPlayers != null && playerCount > maxPlayers) return false
        if (!enabledModules.containsAll(modules)) return false
        if (notModules.any { it in enabledModules }) return false
        return true
    }
}

/** Where the guide being shown came from; USER always wins over upstream copies. */
enum class SetupGuideSource { BUNDLED, CATALOG, USER }

data class LoadedSetupGuide(
    val guide: SetupGuide,
    val source: SetupGuideSource
)

// --- Resolved (rendered) form ---

data class ResolvedSetup(
    val playerCount: Int,
    val enabledModules: Set<String>,
    val sections: List<ResolvedSection>
) {
    val checkableStepIds: List<String>
        get() = sections.filter { it.kind == GuideSectionKind.SETUP }.flatMap { s -> s.steps.map { it.id } }
}

data class ResolvedSection(
    val id: String,
    val title: String,
    val kind: GuideSectionKind,
    val steps: List<ResolvedStep>
)

data class ResolvedStep(
    val id: String,
    /** Text split into runs so quantities can be emphasised. */
    val parts: List<ResolvedTextPart>,
    val note: String?
) {
    val plainText: String get() = parts.joinToString("") { it.text }
}

data class ResolvedTextPart(val text: String, val isAmount: Boolean)
