package cz.nicolsburg.boardflow.data.scoresheet

import cz.nicolsburg.boardflow.model.CountRow
import cz.nicolsburg.boardflow.model.ExpeditionRow
import cz.nicolsburg.boardflow.model.MajorityRow
import cz.nicolsburg.boardflow.model.PointsRow
import cz.nicolsburg.boardflow.model.ScoreExample
import cz.nicolsburg.boardflow.model.ScoreGroup
import cz.nicolsburg.boardflow.model.ScoreModule
import cz.nicolsburg.boardflow.model.ScoreRow
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.ScoreTiebreaker
import cz.nicolsburg.boardflow.model.SetsRow
import cz.nicolsburg.boardflow.model.SheetOrigin
import cz.nicolsburg.boardflow.model.TableRow
import cz.nicolsburg.boardflow.model.TieRule
import cz.nicolsburg.boardflow.model.WinRule
import org.json.JSONArray
import org.json.JSONObject

/**
 * The portable JSON form of a [ScoreSheet], used for the user's own sheets (AI drafts, imports),
 * backups and sharing. Every row kind round-trips; defaults are left out when writing. Pure, so it
 * is unit-tested; problems with the content are [ScoreSheetValidator]'s job.
 */
object ScoreSheetJson {
    const val SCHEMA_VERSION = 1

    fun write(sheet: ScoreSheet): JSONObject = JSONObject().apply {
        put("schemaVersion", SCHEMA_VERSION)
        put("id", sheet.id)
        put("gameName", sheet.gameName)
        put("gameIds", JSONArray(sheet.gameIds.toList()))
        put("maxPlayers", sheet.maxPlayers)
        if (sheet.winRule != WinRule.HIGHEST) put("winRule", sheet.winRule.name)
        put("provenance", JSONObject().apply {
            put("origin", sheet.origin.name)
            put("reviewed", sheet.reviewed)
            sheet.aiModel?.let { put("aiModel", it) }
            if (sheet.sources.isNotEmpty()) put("sources", JSONArray(sheet.sources))
        })
        if (sheet.modules.isNotEmpty()) put("modules", JSONArray().also { arr ->
            sheet.modules.forEach { m ->
                arr.put(JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    if (m.bggIds.isNotEmpty()) put("bggIds", JSONArray(m.bggIds.toList()))
                    m.group?.let { put("group", it) }
                    if (m.defaultEnabled) put("defaultEnabled", true)
                })
            }
        })
        put("rows", JSONArray().also { arr -> sheet.rows.forEach { arr.put(writeRow(it)) } })
        if (sheet.tiebreakers.isNotEmpty()) put("tiebreakers", JSONArray().also { arr ->
            sheet.tiebreakers.forEach { t ->
                arr.put(JSONObject().apply {
                    put("id", t.id)
                    put("label", t.label)
                    if (!t.preferHighest) put("preferHighest", false)
                    t.fromKey?.let { put("fromKey", it) }
                })
            }
        })
        if (sheet.examples.isNotEmpty()) put("examples", JSONArray().also { arr ->
            sheet.examples.forEach { e ->
                arr.put(JSONObject().apply {
                    put("players", JSONArray().also { players ->
                        e.players.forEach { values -> players.put(JSONObject(values.mapValues { it.value as Any })) }
                    })
                    put("totals", JSONArray(e.totals))
                    if (e.modules.isNotEmpty()) put("modules", JSONArray(e.modules.toList()))
                    e.note?.let { put("note", it) }
                })
            }
        })
    }

    /** Reads a sheet; throws on JSON that is not a sheet at all (missing id, rows or a row kind). */
    fun parse(json: JSONObject): ScoreSheet {
        val provenance = json.optJSONObject("provenance")
        return ScoreSheet(
            id = json.getString("id"),
            gameName = json.getString("gameName"),
            gameIds = json.optJSONArray("gameIds").ints().toSet(),
            maxPlayers = json.optInt("maxPlayers", 6),
            modules = json.optJSONArray("modules").objects().map { m ->
                ScoreModule(
                    id = m.getString("id"),
                    name = m.optString("name", m.getString("id")),
                    bggIds = m.optJSONArray("bggIds").ints().toSet(),
                    group = m.optStringOrNull("group"),
                    defaultEnabled = m.optBoolean("defaultEnabled", false)
                )
            },
            rows = json.getJSONArray("rows").objects().map(::parseRow),
            tiebreakers = json.optJSONArray("tiebreakers").objects().map { t ->
                ScoreTiebreaker(
                    id = t.getString("id"),
                    label = t.optString("label", t.getString("id")),
                    preferHighest = t.optBoolean("preferHighest", true),
                    fromKey = t.optStringOrNull("fromKey")
                )
            },
            winRule = json.optStringOrNull("winRule")?.let { runCatching { WinRule.valueOf(it.uppercase()) }.getOrNull() }
                ?: WinRule.HIGHEST,
            origin = provenance?.optStringOrNull("origin")?.let { runCatching { SheetOrigin.valueOf(it) }.getOrNull() }
                ?: SheetOrigin.USER,
            reviewed = provenance?.optBoolean("reviewed", true) ?: true,
            aiModel = provenance?.optStringOrNull("aiModel"),
            sources = provenance?.optJSONArray("sources").strings(),
            examples = json.optJSONArray("examples").objects().map { e ->
                ScoreExample(
                    players = e.getJSONArray("players").objects().map { p -> p.keys().asSequence().associateWith { p.getInt(it) } },
                    totals = e.getJSONArray("totals").ints(),
                    modules = e.optJSONArray("modules").strings().toSet(),
                    note = e.optStringOrNull("note")
                )
            }
        )
    }

    fun parse(text: String): ScoreSheet = parse(JSONObject(text))

    private fun writeRow(row: ScoreRow): JSONObject = JSONObject().apply {
        put("id", row.id)
        put("label", row.label)
        row.hint?.let { put("hint", it) }
        row.module?.let { put("module", it) }
        when (row) {
            is PointsRow -> {
                put("kind", "points")
                if (row.negative) put("negative", true)
                if (row.subtract) put("subtract", true)
            }
            is CountRow -> {
                put("kind", "count")
                if (row.pointsEach != 1) put("pointsEach", row.pointsEach)
                if (row.per != 1) put("per", row.per)
                if (row.squared) put("squared", true)
                if (row.roundUp) put("roundUp", true)
            }
            is MajorityRow -> {
                put("kind", "majority")
                put("groups", writeGroups(row.groups))
                put("awards", JSONArray(row.awards))
                if (row.ties != TieRule.SPLIT) put("ties", row.ties.name)
                if (row.pointsEach != 0) put("pointsEach", row.pointsEach)
                if (row.inputPrefix != row.id) put("inputPrefix", row.inputPrefix)
                if (row.awardsByPlayers.isNotEmpty()) put("awardsByPlayers", JSONObject().also { o ->
                    row.awardsByPlayers.forEach { (n, awards) -> o.put(n.toString(), JSONArray(awards)) }
                })
                if (row.beatsEach != 0) put("beatsEach", row.beatsEach)
                if (row.fewestPenalty != 0) {
                    put("fewestPenalty", row.fewestPenalty)
                    put("fewestFromPlayers", row.fewestFromPlayers)
                }
            }
            is TableRow -> {
                put("kind", "table")
                put("steps", JSONArray().also { arr -> row.steps.forEach { (count, points) -> arr.put(JSONArray(listOf(count, points))) } })
                if (row.below != 0) put("below", row.below)
            }
            is SetsRow -> {
                put("kind", "sets")
                put("groups", writeGroups(row.groups))
                put("setBonus", row.setBonus)
            }
            is ExpeditionRow -> {
                put("kind", "expedition")
                put("groups", writeGroups(row.groups))
                if (row.cost != 20) put("cost", row.cost)
                if (row.bonus != 20) put("bonus", row.bonus)
                if (row.bonusCards != 8) put("bonusCards", row.bonusCards)
            }
        }
    }

    private fun parseRow(o: JSONObject): ScoreRow {
        val id = o.getString("id")
        val label = o.optString("label", id)
        val hint = o.optStringOrNull("hint")
        val module = o.optStringOrNull("module")
        return when (val kind = o.optString("kind", "points").lowercase()) {
            "points" -> PointsRow(id, label, hint, module, o.optBoolean("negative", false), o.optBoolean("subtract", false))
            "count" -> CountRow(
                id, label, o.optInt("pointsEach", 1), hint, module,
                per = o.optInt("per", 1).coerceAtLeast(1),
                squared = o.optBoolean("squared", false),
                roundUp = o.optBoolean("roundUp", false)
            )
            "majority" -> MajorityRow(
                id = id, label = label, groups = parseGroups(o), awards = o.optJSONArray("awards").ints(),
                hint = hint, module = module,
                ties = o.optStringOrNull("ties")?.let { runCatching { TieRule.valueOf(it.uppercase()) }.getOrNull() } ?: TieRule.SPLIT,
                pointsEach = o.optInt("pointsEach", 0),
                inputPrefix = o.optStringOrNull("inputPrefix") ?: id,
                awardsByPlayers = o.optJSONObject("awardsByPlayers")?.let { byPlayers ->
                    byPlayers.keys().asSequence().mapNotNull { k -> k.toIntOrNull()?.let { it to byPlayers.optJSONArray(k).ints() } }.toMap()
                } ?: emptyMap(),
                beatsEach = o.optInt("beatsEach", 0),
                fewestPenalty = o.optInt("fewestPenalty", 0),
                fewestFromPlayers = o.optInt("fewestFromPlayers", 3)
            )
            "table" -> TableRow(
                id, label,
                steps = o.getJSONArray("steps").let { arr ->
                    (0 until arr.length()).map { i -> arr.getJSONArray(i).let { it.getInt(0) to it.getInt(1) } }
                },
                below = o.optInt("below", 0), hint = hint, module = module
            )
            "sets" -> SetsRow(id, label, parseGroups(o), o.optInt("setBonus", 0), hint, module)
            "expedition" -> ExpeditionRow(
                id, label, parseGroups(o), o.optInt("cost", 20), o.optInt("bonus", 20), o.optInt("bonusCards", 8), hint, module
            )
            else -> throw IllegalArgumentException("Unknown row kind '$kind' in row $id")
        }
    }

    private fun writeGroups(groups: List<ScoreGroup>) = JSONArray().also { arr ->
        groups.forEach { arr.put(JSONObject().put("id", it.id).put("label", it.label)) }
    }

    private fun parseGroups(o: JSONObject): List<ScoreGroup> =
        o.optJSONArray("groups").objects().map { ScoreGroup(it.getString("id"), it.optString("label", it.getString("id"))) }

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
    private fun JSONArray?.ints(): List<Int> = if (this == null) emptyList() else (0 until length()).map { getInt(it) }
    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).map { getString(it) }
    private fun JSONObject.optStringOrNull(key: String): String? = if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
}
