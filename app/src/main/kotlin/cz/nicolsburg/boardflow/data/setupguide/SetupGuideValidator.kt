package cz.nicolsburg.boardflow.data.setupguide

import cz.nicolsburg.boardflow.model.GuideSectionKind
import cz.nicolsburg.boardflow.model.SetupGuide
import cz.nicolsburg.boardflow.model.StepCondition

/**
 * Structural checks every guide must pass before it is stored or shown, whatever its source
 * (bundled, catalog, user, or a future AI draft).
 */
object SetupGuideValidator {

    private val idPattern = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

    fun validate(guide: SetupGuide): List<String> = buildList {
        if (guide.schemaVersion < 1 || guide.schemaVersion > SetupGuide.CURRENT_SCHEMA_VERSION) {
            add("Unsupported schemaVersion ${guide.schemaVersion}")
        }
        if (guide.gameId <= 0) add("gameId must be a positive BGG id")
        if (guide.gameName.isBlank()) add("gameName is blank")
        if (guide.version < 1) add("version must be >= 1")
        guide.players?.let { if (it.min < 1 || it.max < it.min) add("Invalid players range ${it.min}-${it.max}") }

        val moduleIds = guide.modules.map { it.id }
        duplicates(moduleIds).forEach { add("Duplicate module id '$it'") }
        guide.modules.forEach { module ->
            if (!idPattern.matches(module.id)) add("Module id '${module.id}' must be kebab-case")
            (module.requires + module.excludes).filter { it !in moduleIds }
                .forEach { add("Module '${module.id}' references unknown module '$it'") }
        }

        val sectionIds = guide.sections.map { it.id }
        duplicates(sectionIds).forEach { add("Duplicate section id '$it'") }
        val stepIds = guide.sections.flatMap { s -> s.steps.map { it.id } }
        duplicates(stepIds).forEach { add("Duplicate step id '$it'") }
        if (guide.sections.none { it.kind == GuideSectionKind.SETUP }) add("Guide has no SETUP section")

        fun checkCondition(where: String, condition: StepCondition?) {
            condition ?: return
            (condition.modules + condition.notModules).filter { it !in moduleIds }
                .forEach { add("$where references unknown module '$it'") }
        }

        guide.sections.forEach { section ->
            if (!idPattern.matches(section.id)) add("Section id '${section.id}' must be kebab-case")
            if (section.steps.isEmpty()) add("Section '${section.id}' has no steps")
            checkCondition("Section '${section.id}'", section.condition)
            section.steps.forEach { step ->
                val where = "Step '${step.id}'"
                if (!idPattern.matches(step.id)) add("$where id must be kebab-case")
                if (step.text.isBlank()) add("$where has blank text")
                checkCondition(where, step.condition)
                SetupGuideResolver.placeholders(step.text).filter { it !in step.amounts }
                    .forEach { add("$where uses {$it} without an amount") }
                step.amounts.forEach { (key, rule) ->
                    if (rule.default == null && rule.byPlayers.isEmpty() && rule.cases.isEmpty()) {
                        add("$where amount '$key' has no values")
                    }
                    rule.cases.forEach { checkCondition("$where amount '$key'", it.condition) }
                }
            }
        }
    }

    fun isValid(guide: SetupGuide): Boolean = validate(guide).isEmpty()

    private fun duplicates(ids: List<String>): Set<String> =
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
}
