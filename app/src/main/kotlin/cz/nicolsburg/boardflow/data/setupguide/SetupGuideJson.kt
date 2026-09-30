package cz.nicolsburg.boardflow.data.setupguide

import cz.nicolsburg.boardflow.model.AmountCase
import cz.nicolsburg.boardflow.model.AmountRule
import cz.nicolsburg.boardflow.model.GuideModule
import cz.nicolsburg.boardflow.model.GuideOrigin
import cz.nicolsburg.boardflow.model.GuideProvenance
import cz.nicolsburg.boardflow.model.GuideSection
import cz.nicolsburg.boardflow.model.GuideSectionKind
import cz.nicolsburg.boardflow.model.GuideStep
import cz.nicolsburg.boardflow.model.PlayerRange
import cz.nicolsburg.boardflow.model.SetupGuide
import cz.nicolsburg.boardflow.model.StepCondition
import org.json.JSONArray
import org.json.JSONObject

/**
 * org.json mapping for the portable setup-guide format, matching the style of `BackupSerializer`
 * and `SessionMemoryJson`. Parsing is lenient about optional fields (`opt*`) so older app versions
 * ignore fields added later; structural changes bump `schemaVersion` instead.
 */
object SetupGuideJson {

    /** Throws on malformed JSON or missing required fields; callers treat that as "no guide". */
    fun parse(json: String): SetupGuide = parse(JSONObject(json))

    fun parseOrNull(json: String): SetupGuide? = runCatching { parse(json) }.getOrNull()

    fun parse(obj: JSONObject): SetupGuide = SetupGuide(
        schemaVersion = obj.getInt("schemaVersion"),
        gameId = obj.getInt("gameId"),
        aliasGameIds = obj.optJSONArray("aliasGameIds").ints(),
        gameName = obj.getString("gameName"),
        version = obj.getInt("version"),
        provenance = obj.optJSONObject("provenance")?.let(::parseProvenance) ?: GuideProvenance(),
        players = obj.optJSONObject("players")?.let(::parseRange),
        modules = obj.optJSONArray("modules").objects().map(::parseModule),
        sections = obj.optJSONArray("sections").objects().map(::parseSection)
    )

    fun toJson(guide: SetupGuide): JSONObject = JSONObject().apply {
        put("schemaVersion", guide.schemaVersion)
        put("gameId", guide.gameId)
        if (guide.aliasGameIds.isNotEmpty()) put("aliasGameIds", JSONArray(guide.aliasGameIds))
        put("gameName", guide.gameName)
        put("version", guide.version)
        put("provenance", provenanceToJson(guide.provenance))
        guide.players?.let { put("players", rangeToJson(it)) }
        if (guide.modules.isNotEmpty()) put("modules", JSONArray(guide.modules.map(::moduleToJson)))
        put("sections", JSONArray(guide.sections.map(::sectionToJson)))
    }

    fun toJsonString(guide: SetupGuide): String = toJson(guide).toString(2)

    // --- Parsing ---

    private fun parseProvenance(obj: JSONObject) = GuideProvenance(
        origin = obj.optString("origin").let { name ->
            GuideOrigin.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: GuideOrigin.BOARDFLOW
        },
        author = obj.optStringOrNull("author"),
        sources = obj.optJSONArray("sources").strings(),
        basedOnVersion = obj.optIntOrNull("basedOnVersion"),
        aiModel = obj.optStringOrNull("aiModel"),
        reviewed = obj.optBoolean("reviewed", true)
    )

    private fun parseRange(obj: JSONObject) = PlayerRange(
        min = obj.getInt("min"),
        max = obj.getInt("max"),
        exclude = obj.optJSONArray("exclude").ints().toSet()
    )

    private fun parseModule(obj: JSONObject) = GuideModule(
        id = obj.getString("id"),
        name = obj.getString("name"),
        bggId = obj.optIntOrNull("bggId"),
        defaultEnabled = obj.optBoolean("defaultEnabled", false),
        players = obj.optJSONObject("players")?.let(::parseRange),
        extendsMaxPlayers = obj.optIntOrNull("extendsMaxPlayers"),
        forcedAtPlayers = obj.optJSONArray("forcedAtPlayers").ints().toSet(),
        requires = obj.optJSONArray("requires").strings(),
        excludes = obj.optJSONArray("excludes").strings(),
        note = obj.optStringOrNull("note"),
        group = obj.optStringOrNull("group")
    )

    private fun parseSection(obj: JSONObject) = GuideSection(
        id = obj.getString("id"),
        title = obj.getString("title"),
        kind = obj.optString("kind").let { name ->
            GuideSectionKind.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: GuideSectionKind.SETUP
        },
        condition = obj.optJSONObject("condition")?.let(::parseCondition),
        steps = obj.optJSONArray("steps").objects().map(::parseStep)
    )

    private fun parseStep(obj: JSONObject) = GuideStep(
        id = obj.getString("id"),
        text = obj.getString("text"),
        amounts = obj.optJSONObject("amounts")?.let { amounts ->
            amounts.keys().asSequence().associateWith { key -> parseAmount(amounts.get(key)) }
        }.orEmpty(),
        condition = obj.optJSONObject("condition")?.let(::parseCondition),
        note = obj.optStringOrNull("note"),
        ref = obj.optStringOrNull("ref")
    )

    /** An amount is either a bare value (`5`, `"5, 6"`) or a rule object. */
    private fun parseAmount(value: Any): AmountRule = when (value) {
        is JSONObject -> AmountRule(
            default = value.opt("default")?.takeUnless { it == JSONObject.NULL }?.toString(),
            byPlayers = value.optJSONObject("byPlayers")?.let { map ->
                map.keys().asSequence().associate { key -> key.toInt() to map.get(key).toString() }
            }.orEmpty(),
            cases = value.optJSONArray("cases").objects().map { case ->
                AmountCase(condition = parseCondition(case.getJSONObject("when")), value = case.get("value").toString())
            }
        )
        else -> AmountRule(default = value.toString())
    }

    private fun parseCondition(obj: JSONObject) = StepCondition(
        players = obj.optJSONArray("players")?.ints()?.toSet(),
        minPlayers = obj.optIntOrNull("minPlayers"),
        maxPlayers = obj.optIntOrNull("maxPlayers"),
        modules = obj.optJSONArray("modules").strings(),
        notModules = obj.optJSONArray("notModules").strings()
    )

    // --- Serialization ---

    private fun provenanceToJson(p: GuideProvenance) = JSONObject().apply {
        put("origin", p.origin.name)
        p.author?.let { put("author", it) }
        if (p.sources.isNotEmpty()) put("sources", JSONArray(p.sources))
        p.basedOnVersion?.let { put("basedOnVersion", it) }
        p.aiModel?.let { put("aiModel", it) }
        if (!p.reviewed) put("reviewed", false)
    }

    private fun rangeToJson(r: PlayerRange) = JSONObject().put("min", r.min).put("max", r.max).apply {
        if (r.exclude.isNotEmpty()) put("exclude", JSONArray(r.exclude.sorted()))
    }

    private fun moduleToJson(m: GuideModule) = JSONObject().apply {
        put("id", m.id)
        put("name", m.name)
        m.bggId?.let { put("bggId", it) }
        if (m.defaultEnabled) put("defaultEnabled", true)
        m.players?.let { put("players", rangeToJson(it)) }
        m.extendsMaxPlayers?.let { put("extendsMaxPlayers", it) }
        if (m.forcedAtPlayers.isNotEmpty()) put("forcedAtPlayers", JSONArray(m.forcedAtPlayers.sorted()))
        if (m.requires.isNotEmpty()) put("requires", JSONArray(m.requires))
        if (m.excludes.isNotEmpty()) put("excludes", JSONArray(m.excludes))
        m.note?.let { put("note", it) }
        m.group?.let { put("group", it) }
    }

    private fun sectionToJson(s: GuideSection) = JSONObject().apply {
        put("id", s.id)
        put("title", s.title)
        put("kind", s.kind.name)
        s.condition?.let { put("condition", conditionToJson(it)) }
        put("steps", JSONArray(s.steps.map(::stepToJson)))
    }

    private fun stepToJson(s: GuideStep) = JSONObject().apply {
        put("id", s.id)
        put("text", s.text)
        if (s.amounts.isNotEmpty()) {
            put("amounts", JSONObject().also { amounts ->
                s.amounts.forEach { (key, rule) -> amounts.put(key, amountToJson(rule)) }
            })
        }
        s.condition?.let { put("condition", conditionToJson(it)) }
        s.note?.let { put("note", it) }
        s.ref?.let { put("ref", it) }
    }

    private fun amountToJson(rule: AmountRule): Any {
        if (rule.byPlayers.isEmpty() && rule.cases.isEmpty() && rule.default != null) return rule.default
        return JSONObject().apply {
            rule.default?.let { put("default", it) }
            if (rule.byPlayers.isNotEmpty()) {
                put("byPlayers", JSONObject().also { map ->
                    rule.byPlayers.toSortedMap().forEach { (count, value) -> map.put(count.toString(), value) }
                })
            }
            if (rule.cases.isNotEmpty()) {
                put("cases", JSONArray(rule.cases.map { case ->
                    JSONObject().put("when", conditionToJson(case.condition)).put("value", case.value)
                }))
            }
        }
    }

    private fun conditionToJson(c: StepCondition) = JSONObject().apply {
        c.players?.let { put("players", JSONArray(it.sorted())) }
        c.minPlayers?.let { put("minPlayers", it) }
        c.maxPlayers?.let { put("maxPlayers", it) }
        if (c.modules.isNotEmpty()) put("modules", JSONArray(c.modules))
        if (c.notModules.isNotEmpty()) put("notModules", JSONArray(c.notModules))
    }
}

// --- org.json helpers ---

internal fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

internal fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { getString(it) }

internal fun JSONArray?.ints(): List<Int> =
    if (this == null) emptyList() else (0 until length()).map { getInt(it) }

internal fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

internal fun JSONObject.optIntOrNull(key: String): Int? =
    if (isNull(key)) null else (opt(key) as? Number)?.toInt() ?: optString(key).toIntOrNull()
