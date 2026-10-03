package cz.nicolsburg.boardflow.data.setupguide

import cz.nicolsburg.boardflow.model.AmountRule
import cz.nicolsburg.boardflow.model.GuideModule
import cz.nicolsburg.boardflow.model.GuideSection
import cz.nicolsburg.boardflow.model.GuideSectionKind
import cz.nicolsburg.boardflow.model.GuideStep
import cz.nicolsburg.boardflow.model.SetupGuide
import cz.nicolsburg.boardflow.model.StepCondition

/**
 * The edits the in-app guide editor can make. Pure functions on [SetupGuide], so the editor's
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
