package cz.nicolsburg.boardflow.data.setupguide

import cz.nicolsburg.boardflow.model.AmountRule
import cz.nicolsburg.boardflow.model.GuideModule
import cz.nicolsburg.boardflow.model.PlayerRange
import cz.nicolsburg.boardflow.model.ResolvedSection
import cz.nicolsburg.boardflow.model.ResolvedSetup
import cz.nicolsburg.boardflow.model.ResolvedStep
import cz.nicolsburg.boardflow.model.ResolvedTextPart
import cz.nicolsburg.boardflow.model.SetupGuide

/**
 * Pure functions that turn a guide plus a table configuration into the checklist to show.
 * No Android dependencies, so it is covered by plain JVM unit tests.
 */
object SetupGuideResolver {

    private val placeholder = Regex("""\{([A-Za-z0-9_]+)\}""")
    private const val FALLBACK_MIN = 1
    private const val FALLBACK_MAX = 4

    /** Largest player count offered as a chip, so 1-99 party games stay readable. */
    const val MAX_PLAYER_CHIPS = 8

    fun baseRange(guide: SetupGuide, fallback: PlayerRange? = null): PlayerRange =
        guide.players ?: fallback ?: PlayerRange(FALLBACK_MIN, FALLBACK_MAX)

    /** Player counts selectable given the modules the user has switched on. */
    fun selectablePlayerCounts(
        guide: SetupGuide,
        selectedModules: Set<String>,
        fallback: PlayerRange? = null
    ): List<Int> {
        val base = baseRange(guide, fallback)
        val extended = guide.modules
            .filter { it.id in selectedModules }
            .mapNotNull { it.extendsMaxPlayers }
            .maxOrNull()
        val max = maxOf(base.max, extended ?: base.max).coerceAtMost(base.min + MAX_PLAYER_CHIPS - 1)
        return (base.min..max).filter { it !in base.exclude }
    }

    /** Player counts offered in the picker, including counts only reachable through a module. */
    fun allPlayerCounts(guide: SetupGuide, fallback: PlayerRange? = null): List<Int> =
        selectablePlayerCounts(guide, guide.modules.map { it.id }.toSet(), fallback)

    /** The selectable count closest to [preferred] (ties go to the lower count). */
    fun nearestPlayerCount(counts: List<Int>, preferred: Int): Int =
        counts.minWithOrNull(compareBy<Int> { kotlin.math.abs(it - preferred) }.thenBy { it }) ?: preferred

    fun isModuleAvailable(module: GuideModule, playerCount: Int): Boolean =
        module.players == null || playerCount in module.players.min..module.players.max

    /**
     * The modules actually in play: the user's selection, plus forced and required modules,
     * minus modules unavailable at this player count or excluded by a forced module.
     */
    fun effectiveModules(guide: SetupGuide, playerCount: Int, selected: Set<String>): Set<String> {
        val byId = guide.modules.associateBy { it.id }
        val forced = guide.modules.filter { playerCount in it.forcedAtPlayers }.map { it.id }.toSet()
        val result = linkedSetOf<String>()
        fun add(id: String) {
            val module = byId[id] ?: return
            if (!isModuleAvailable(module, playerCount) || !result.add(id)) return
            module.requires.forEach(::add)
        }
        forced.forEach(::add)
        selected.forEach(::add)
        // Single-choice groups: keep one member (forced first, then guide order); if none is on,
        // turn on the default (or first) member available at this player count.
        guide.modules.mapNotNull { it.group }.distinct().forEach { group ->
            val members = guide.modules.filter { it.group == group && isModuleAvailable(it, playerCount) }
            val chosen = members.filter { it.id in result }.sortedBy { if (it.id in forced) 0 else 1 }
            if (chosen.size > 1) chosen.drop(1).forEach { result.remove(it.id) }
            if (chosen.isEmpty()) {
                (members.firstOrNull { it.defaultEnabled } ?: members.firstOrNull())?.let { add(it.id) }
            }
        }
        // Exclusions: forced modules win; otherwise the earlier-listed module wins.
        val ordered = guide.modules.map { it.id }.filter { it in result }
            .sortedBy { if (it in forced) 0 else 1 }
        val kept = linkedSetOf<String>()
        for (id in ordered) {
            val module = byId.getValue(id)
            val clashes = kept.any { other -> id in byId.getValue(other).excludes || other in module.excludes }
            if (!clashes) kept.add(id)
        }
        // A module whose requirement was dropped by an exclusion is dropped too.
        return kept.filterTo(linkedSetOf()) { id -> byId.getValue(id).requires.all { it in kept } }
    }

    /** [ids] plus every module that requires one of them, directly or through another module. */
    fun withDependents(guide: SetupGuide, ids: Set<String>): Set<String> {
        val result = ids.toMutableSet()
        do {
            val added = guide.modules.filter { it.id !in result && it.requires.any(result::contains) }.map { it.id }
            result += added
        } while (added.isNotEmpty())
        return result
    }

    /** Modules the user may not toggle at this player count (forced on, or unavailable). */
    fun lockedModules(guide: SetupGuide, playerCount: Int, enabled: Set<String> = emptySet()): Set<String> {
        val byId = guide.modules.associateBy { it.id }
        // A module that needs a mode (a group member) that is not chosen, directly or through
        // another module: Arcs' Leaders & Lore in a campaign. Turning it on would switch the mode.
        fun needsOtherMode(id: String, seen: Set<String> = emptySet()): Boolean {
            val module = byId[id] ?: return false
            return module.requires.any { req ->
                req !in seen && ((byId[req]?.group != null && req !in enabled) || needsOtherMode(req, seen + id))
            }
        }
        return guide.modules.filter {
            playerCount in it.forcedAtPlayers || !isModuleAvailable(it, playerCount) ||
                (enabled.isNotEmpty() && needsOtherMode(it.id))
        }.map { it.id }.toSet()
    }

    fun resolve(guide: SetupGuide, playerCount: Int, selectedModules: Set<String>): ResolvedSetup {
        val modules = effectiveModules(guide, playerCount, selectedModules)
        val sections = guide.sections.mapNotNull { section ->
            if (section.condition?.matches(playerCount, modules) == false) return@mapNotNull null
            val steps = section.steps
                .filter { it.condition?.matches(playerCount, modules) != false }
                .map { step ->
                    ResolvedStep(
                        id = step.id,
                        parts = renderText(step.text, step.amounts, playerCount, modules),
                        note = step.note,
                        details = step.details.map(String::trim).filter(String::isNotEmpty)
                    )
                }
            if (steps.isEmpty()) null
            else ResolvedSection(id = section.id, title = section.title, kind = section.kind, steps = steps)
        }
        return ResolvedSetup(playerCount = playerCount, enabledModules = modules, sections = sections)
    }

    fun resolveAmount(rule: AmountRule, playerCount: Int, modules: Set<String>): String? =
        rule.cases.firstOrNull { it.condition.matches(playerCount, modules) }?.value
            ?: rule.byPlayers[playerCount]
            ?: rule.default

    internal fun renderText(
        text: String,
        amounts: Map<String, AmountRule>,
        playerCount: Int,
        modules: Set<String>
    ): List<ResolvedTextPart> {
        val parts = mutableListOf<ResolvedTextPart>()
        var cursor = 0
        for (match in placeholder.findAll(text)) {
            if (match.range.first > cursor) parts += ResolvedTextPart(text.substring(cursor, match.range.first), false)
            val value = amounts[match.groupValues[1]]?.let { resolveAmount(it, playerCount, modules) }
            // An unresolvable placeholder is a guide bug; show it verbatim rather than hide the step.
            parts += ResolvedTextPart(value ?: match.value, isAmount = value != null)
            cursor = match.range.last + 1
        }
        if (cursor < text.length) parts += ResolvedTextPart(text.substring(cursor), false)
        return parts
    }

    fun placeholders(text: String): Set<String> =
        placeholder.findAll(text).map { it.groupValues[1] }.toSet()
}
