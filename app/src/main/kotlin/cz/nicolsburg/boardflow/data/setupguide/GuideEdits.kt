package cz.nicolsburg.boardflow.data.setupguide

import cz.nicolsburg.boardflow.model.AmountCase
import cz.nicolsburg.boardflow.model.AmountRule
import cz.nicolsburg.boardflow.model.GuideModule
import cz.nicolsburg.boardflow.model.GuideSection
import cz.nicolsburg.boardflow.model.GuideSectionKind
import cz.nicolsburg.boardflow.model.GuideStep
import cz.nicolsburg.boardflow.model.SetupGuide
import cz.nicolsburg.boardflow.model.StepCondition

/**
 * The edits the in-app guide editor can make, including per-player quantities and when a step
 * or section shows. Pure functions on [SetupGuide], so the editor's
 * ViewModel stays thin and the rules are unit-tested. Existing step ids never change (ticks and
 * conditions refer to them); new steps and sections get ids that are unique in the guide.
 */
object GuideEdits {

    fun setSectionTitle(guide: SetupGuide, sectionId: String, title: String): SetupGuide =
        guide.mapSection(sectionId) { it.copy(title = title) }

    fun setStepText(guide: SetupGuide, sectionId: String, stepId: String, text: String): SetupGuide =
        guide.mapStep(sectionId, stepId) { it.copy(text = text) }

    fun setStepNote(guide: SetupGuide, sectionId: String, stepId: String, note: String): SetupGuide =
        guide.mapStep(sectionId, stepId) { it.copy(note = note.ifBlank { null }) }

    /** Adds an empty step at the end of a section; returns the new guide and the step's id. */
    fun addStep(guide: SetupGuide, sectionId: String): Pair<SetupGuide, String> {
        val id = uniqueId(guide.allStepIds(), "my-step")
        return guide.mapSection(sectionId) { it.copy(steps = it.steps + GuideStep(id = id, text = "")) } to id
    }

    fun deleteStep(guide: SetupGuide, sectionId: String, stepId: String): SetupGuide =
        guide.mapSection(sectionId) { section -> section.copy(steps = section.steps.filterNot { it.id == stepId }) }

    /** Moves a step one place up ([delta] = -1) or down (+1) within its section. */
    fun moveStep(guide: SetupGuide, sectionId: String, stepId: String, delta: Int): SetupGuide =
        guide.mapSection(sectionId) { section ->
            val from = section.steps.indexOfFirst { it.id == stepId }
            val to = from + delta
            if (from < 0 || to !in section.steps.indices) return@mapSection section
            val steps = section.steps.toMutableList()
            steps.add(to, steps.removeAt(from))
            section.copy(steps = steps)
        }

    /** Adds a checklist section after the last setup section; returns the new guide and its id. */
    fun addSection(guide: SetupGuide, title: String = "My steps"): Pair<SetupGuide, String> {
        val id = uniqueId(guide.sections.map { it.id }.toSet(), "my-section")
        val section = GuideSection(id = id, title = title, kind = GuideSectionKind.SETUP)
        val insertAt = guide.sections.indexOfLast { it.kind == GuideSectionKind.SETUP } + 1
        val sections = guide.sections.toMutableList().apply { add(insertAt, section) }
        return guide.copy(sections = sections) to id
    }

    fun deleteSection(guide: SetupGuide, sectionId: String): SetupGuide =
        guide.copy(sections = guide.sections.filterNot { it.id == sectionId })

    /** Drops steps left empty and sections left with no steps, so a half-done edit still saves. */
    fun tidy(guide: SetupGuide): SetupGuide = guide.copy(
        sections = guide.sections
            .map { section -> section.copy(steps = section.steps.filter { it.text.isNotBlank() }) }
            .filter { it.steps.isNotEmpty() }
    )

    // --- Quantities ---

    /**
     * Sets one value of a step's quantity [name]: for [players] players, or the value for every
     * other count when [players] is null. A blank value removes it; a quantity left with no
     * values is removed (the validator then asks for one while the text still uses it).
     */
    fun setAmountValue(guide: SetupGuide, sectionId: String, stepId: String, name: String, players: Int?, value: String): SetupGuide =
        guide.mapStep(sectionId, stepId) { step ->
            val rule = step.amounts[name] ?: AmountRule()
            val v = value.trim().ifBlank { null }
            val updated = if (players == null) {
                rule.copy(default = v)
            } else {
                rule.copy(byPlayers = if (v == null) rule.byPlayers - players else rule.byPlayers + (players to v))
            }
            step.withAmount(name, updated)
        }

    /** Changes the value of one special case (e.g. "with Pearlbrook at 2 players"). */
    fun setCaseValue(guide: SetupGuide, sectionId: String, stepId: String, name: String, caseIndex: Int, value: String): SetupGuide =
        guide.mapStep(sectionId, stepId) { step ->
            val rule = step.amounts[name] ?: return@mapStep step
            if (caseIndex !in rule.cases.indices) return@mapStep step
            step.withAmount(name, rule.copy(cases = rule.cases.mapIndexed { i, c -> if (i == caseIndex) c.copy(value = value) else c }))
        }

    fun deleteCase(guide: SetupGuide, sectionId: String, stepId: String, name: String, caseIndex: Int): SetupGuide =
        guide.mapStep(sectionId, stepId) { step ->
            val rule = step.amounts[name] ?: return@mapStep step
            step.withAmount(name, rule.copy(cases = rule.cases.filterIndexed { i, _ -> i != caseIndex }))
        }

    /** Quantity names the editor shows for a step: the ones its text uses, then any others it still has. */
    fun quantityNames(step: GuideStep): List<String> =
        (SetupGuideResolver.placeholders(step.text) + step.amounts.keys).distinct()

    fun describeCase(case: AmountCase, modules: List<GuideModule>): String =
        describeCondition(case.condition, modules) ?: "Always"

    // --- When a step or section shows ---

    enum class ModuleRule { ANY, WITH, WITHOUT }

    /** The player counts (out of [allCounts]) at which [condition] lets a step show. */
    fun shownAtCounts(condition: StepCondition?, allCounts: List<Int>): Set<Int> =
        allCounts.filter { n ->
            condition == null || (
                (condition.players == null || n in condition.players) &&
                    (condition.minPlayers == null || n >= condition.minPlayers) &&
                    (condition.maxPlayers == null || n <= condition.maxPlayers)
                )
        }.toSet()

    fun moduleRule(condition: StepCondition?, moduleId: String): ModuleRule = when {
        condition == null -> ModuleRule.ANY
        moduleId in condition.modules -> ModuleRule.WITH
        moduleId in condition.notModules -> ModuleRule.WITHOUT
        else -> ModuleRule.ANY
    }

    /**
     * Shows the step (or, with [stepId] null, the whole section) only at [counts] players.
     * Stored as compactly as the guides are written: nothing for every count, "N+" or "up to N"
     * for a run at either end, otherwise the list. An empty selection is ignored.
     */
    fun setShownAtCounts(guide: SetupGuide, sectionId: String, stepId: String?, counts: Set<Int>, allCounts: List<Int>): SetupGuide {
        if (counts.isEmpty()) return guide
        val sorted = allCounts.sorted()
        val chosen = sorted.filter { it in counts }
        fun isRun(list: List<Int>) = list == sorted.subList(sorted.indexOf(list.first()), sorted.indexOf(list.first()) + list.size)
        return guide.mapCondition(sectionId, stepId) { c ->
            val base = (c ?: StepCondition()).copy(players = null, minPlayers = null, maxPlayers = null)
            when {
                chosen.size == sorted.size -> base
                isRun(chosen) && chosen.last() == sorted.last() -> base.copy(minPlayers = chosen.first())
                isRun(chosen) && chosen.first() == sorted.first() -> base.copy(maxPlayers = chosen.last())
                else -> base.copy(players = chosen.toSet())
            }
        }
    }

    fun setModuleRule(guide: SetupGuide, sectionId: String, stepId: String?, moduleId: String, rule: ModuleRule): SetupGuide =
        guide.mapCondition(sectionId, stepId) { c ->
            val base = c ?: StepCondition()
            base.copy(
                modules = (base.modules - moduleId) + listOfNotNull(moduleId.takeIf { rule == ModuleRule.WITH }),
                notModules = (base.notModules - moduleId) + listOfNotNull(moduleId.takeIf { rule == ModuleRule.WITHOUT })
            )
        }

    /** One line for a condition, e.g. "only 2/3 players, with Pearlbrook", or null for "always". */
    fun describeShownWhen(condition: StepCondition?, modules: List<GuideModule>): String? =
        condition?.let { describeCondition(it, modules) }

    /**
     * A read-only line describing what the editor does not edit: player-count amounts and
     * conditions, e.g. "{n}: 2p 3, 3p 4 - only 2-3 players - with Rise of Ix".
     */
    fun describeRules(step: GuideStep, sectionCondition: StepCondition?, modules: List<GuideModule>): String? {
        val parts = buildList {
            step.amounts.forEach { (name, rule) -> describeAmount(name, rule)?.let(::add) }
            listOfNotNull(sectionCondition, step.condition).forEach { describeCondition(it, modules)?.let(::add) }
        }
        return parts.joinToString(" - ").ifBlank { null }
    }

    private fun describeAmount(name: String, rule: AmountRule): String? {
        val byPlayers = rule.byPlayers.toSortedMap().entries.joinToString(", ") { (p, v) -> "${p}p $v" }
        val value = listOfNotNull(byPlayers.ifBlank { null }, rule.default?.let { if (byPlayers.isBlank()) it else "else $it" })
            .joinToString(", ")
        val cases = if (rule.cases.isNotEmpty()) " (+${rule.cases.size} special cases)" else ""
        return if (value.isBlank() && cases.isBlank()) null else "{$name}: $value$cases"
    }

    private fun describeCondition(condition: StepCondition, modules: List<GuideModule>): String? {
        fun name(id: String) = modules.firstOrNull { it.id == id }?.name ?: id
        val parts = buildList {
            condition.players?.let { add("only ${it.sorted().joinToString("/")} players") }
            condition.minPlayers?.let { add("$it+ players") }
            condition.maxPlayers?.let { add("up to $it players") }
            condition.modules.forEach { add("with ${name(it)}") }
            condition.notModules.forEach { add("without ${name(it)}") }
        }
        return parts.joinToString(", ").ifBlank { null }
    }

    private fun GuideStep.withAmount(name: String, rule: AmountRule): GuideStep {
        val empty = rule.default == null && rule.byPlayers.isEmpty() && rule.cases.isEmpty()
        return copy(amounts = if (empty) amounts - name else amounts + (name to rule))
    }

    /** Applies [transform] to a step's condition, or the section's when [stepId] is null; an empty result is dropped. */
    private fun SetupGuide.mapCondition(sectionId: String, stepId: String?, transform: (StepCondition?) -> StepCondition): SetupGuide {
        fun tidy(c: StepCondition): StepCondition? = c.takeUnless {
            it.players == null && it.minPlayers == null && it.maxPlayers == null && it.modules.isEmpty() && it.notModules.isEmpty()
        }
        return if (stepId == null) {
            mapSection(sectionId) { it.copy(condition = tidy(transform(it.condition))) }
        } else {
            mapStep(sectionId, stepId) { it.copy(condition = tidy(transform(it.condition))) }
        }
    }

    private fun SetupGuide.allStepIds(): Set<String> = sections.flatMap { s -> s.steps.map { it.id } }.toSet()

    private fun uniqueId(taken: Set<String>, prefix: String): String {
        var n = 1
        while ("$prefix-$n" in taken) n++
        return "$prefix-$n"
    }

    private inline fun SetupGuide.mapSection(sectionId: String, transform: (GuideSection) -> GuideSection): SetupGuide =
        copy(sections = sections.map { if (it.id == sectionId) transform(it) else it })

    private inline fun SetupGuide.mapStep(sectionId: String, stepId: String, crossinline transform: (GuideStep) -> GuideStep): SetupGuide =
        mapSection(sectionId) { section -> section.copy(steps = section.steps.map { if (it.id == stepId) transform(it) else it }) }
}
