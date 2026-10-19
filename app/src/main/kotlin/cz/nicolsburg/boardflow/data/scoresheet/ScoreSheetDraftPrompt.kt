package cz.nicolsburg.boardflow.data.scoresheet

import cz.nicolsburg.boardflow.data.setupguide.DraftSpec
import cz.nicolsburg.boardflow.model.ScoreSheet
import cz.nicolsburg.boardflow.model.SheetOrigin
import org.json.JSONObject

/**
 * The prompt for drafting an end-of-game score sheet from a rulebook PDF, and the checks on what
 * comes back. Pure, so it is unit-tested; the HTTP side is `GeminiGuideDraftGenerator`.
 *
 * The model must add worked examples, and [ScoreSheetValidator] recomputes their totals: a sheet
 * whose rows do not give the totals the model itself worked out from the rules goes back to it
 * with the difference, which catches most misread scoring rules.
 */
object ScoreSheetDraftPrompt {

    fun spec(gameId: Int, gameName: String, sourceName: String?) = object : DraftSpec<ScoreSheet> {
        override val noun = "score sheet"
        override fun prompt(problems: List<String>): String = build(gameId, gameName, problems)
        override fun parse(answer: String, model: String): Pair<ScoreSheet, List<String>> =
            ScoreSheetDraftPrompt.parse(answer, gameId, gameName, model, sourceName)
        override fun refusal(answer: String): String? = ScoreSheetDraftPrompt.refusal(answer)
    }

    fun build(gameId: Int, gameName: String, problems: List<String> = emptyList()): String = buildString {
        appendLine("The attached PDF is the rulebook of the board game \"$gameName\" (BoardGameGeek id $gameId).")
        appendLine("Write the game's end-of-game score sheet as one JSON object in the format below: one row per thing that scores at the end, in the order the rulebook counts them.")
        appendLine("Players type a number per row and the app adds them up, so every row must say exactly how its number turns into points.")
        appendLine()
        appendLine(FORMAT)
        appendLine()
        appendLine(RULES)
        appendLine()
        appendLine("Use gameName \"$gameName\". Answer with the JSON object only.")
        if (problems.isNotEmpty()) {
            appendLine()
            appendLine("Your previous answer was rejected for these problems. Fix all of them:")
            problems.take(20).forEach { appendLine("- $it") }
        }
    }

    /** The model's reason for drafting nothing ({"noScoreSheet": "..."}), or null for a sheet. */
    fun refusal(text: String): String? {
        val json = runCatching { JSONObject(jsonBody(text)) }.getOrNull() ?: return null
        if (!json.has(NO_SHEET) || json.has("rows")) return null
        val reason = json.optString(NO_SHEET).trim().takeIf { it.isNotBlank() && it != "true" }
        return "This rulebook has no end-of-game scoring to put on a sheet" + (reason?.let { ": $it" } ?: ".")
    }

    /**
     * Turns the model's answer into the user's sheet for [gameId]: strips code fences, forces the
     * id, game and AI provenance, and returns it with the validator's problems (empty when usable).
     * Throws when the answer is not a sheet at all.
     */
    fun parse(text: String, gameId: Int, gameName: String, model: String?, sourceName: String?): Pair<ScoreSheet, List<String>> {
        val parsed = ScoreSheetJson.parse(JSONObject(jsonBody(text)))
        val sheet = parsed.copy(
            id = ScoreSheetRepository.userSheetId(gameId),
            gameName = gameName,
            // Ids the model "knows" are guesses; a wrong one would open this sheet for another game.
            gameIds = setOf(gameId),
            modules = parsed.modules.map { it.copy(bggIds = emptySet()) },
            maxPlayers = parsed.maxPlayers.coerceIn(1, MAX_PLAYERS),
            origin = SheetOrigin.AI_DRAFT,
            reviewed = false,
            aiModel = model,
            sources = listOfNotNull(sourceName?.takeIf { it.isNotBlank() })
        )
        val problems = ScoreSheetValidator.validate(sheet).toMutableList()
        if (sheet.rows.isEmpty()) problems += "The sheet has no rows"
        if (sheet.examples.isEmpty()) problems += "Add at least one worked example with the totals the rules give"
        return sheet to problems
    }

    private fun jsonBody(text: String): String {
        val body = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = body.indexOf('{')
        val end = body.lastIndexOf('}')
        require(start >= 0 && end > start) { "The answer has no JSON object" }
        return body.substring(start, end + 1)
    }

    private const val NO_SHEET = "noScoreSheet"
    private const val MAX_PLAYERS = 12

    private val FORMAT = """
Format (JSON, comments are explanations only and must not appear in the answer):
{
  "id": "draft",
  "gameName": "",
  "maxPlayers": 5,                          // the most players the rulebook allows, expansions included
  "winRule": "HIGHEST",                     // or "LOWEST" when the lowest total wins
  "modules": [                              // one per expansion or variant that changes scoring; [] if none
    { "id": "kebab-id", "name": "Expansion name", "group": "optional: one of a group is always on", "defaultEnabled": false }
  ],
  "rows": [
    // Every row: "id" (kebab-case, unique), "label" (short, e.g. "Eggs"), "hint" (one line saying what to count
    // and what it is worth, e.g. "1 point per egg on your birds"), optional "module" (shown only with that module on).
    { "kind": "points", "id": "birds", "label": "Birds", "hint": "Points printed on your birds" },
       // points typed as they are; "negative": true lets them go below 0; "subtract": true takes them off the total (a penalty typed as a positive number)
    { "kind": "count", "id": "eggs", "label": "Eggs", "hint": "1 point per egg", "pointsEach": 1 },
       // a counted thing: "pointsEach" points for every "per" of them (default 1), rounded down ("roundUp": true to round up);
       // "squared": true scores count x count x pointsEach. A penalty per item uses a negative pointsEach.
    { "kind": "majority", "id": "goals", "label": "Most forests", "hint": "Most 5, second 2",
      "groups": [ { "id": "forest", "label": "Forest" } ], "awards": [5, 2], "ties": "SPLIT" },
       // players type a count per group; in each group the most gets awards[0], the second awards[1] and so on (a count of 0 earns nothing).
       // "ties": "SPLIT" (tied players share the places' awards, rounded down) or "ALL" (each tied player gets the full award).
       // Optional: "awardsByPlayers": {"2": [3]} when awards differ by player count; "pointsEach": points per counted item on top;
       // "beatsEach": points for each other player with strictly fewer; "fewestPenalty": -6 taken from the fewest, from "fewestFromPlayers": 3 players.
    { "kind": "table", "id": "fields", "label": "Fields", "hint": "1-2: 1, 3: 2, 4: 3, 5+: 4", "steps": [[1, 1], [3, 2], [4, 3], [5, 4]], "below": -1 },
       // a count looked up in a printed chart: the points of the highest step whose count is at or below it; "below" for counts under the first step
    { "kind": "sets", "id": "science", "label": "Science", "hint": "Each symbol squared, +7 per set of three",
      "groups": [ { "id": "gear", "label": "Gear" }, { "id": "tablet", "label": "Tablet" } ], "setBonus": 7 }
       // each group's count squared, plus setBonus for every complete set of one of each group
  ],
  "tiebreakers": [                          // in order, from the rulebook; [] if it gives none
    { "id": "food", "label": "Most leftover food", "preferHighest": true, "fromKey": "optional: a row id whose typed value decides it" }
  ],
  "examples": [                             // 2 or more: values for 2-3 made-up players and the totals the rulebook's rules give
    { "players": [ { "birds": 30, "eggs": 8, "goals.forest": 3 }, { "birds": 25, "eggs": 10, "goals.forest": 3 } ],
      "totals": [41, 38], "modules": [], "note": "optional: what the example checks" }
       // keys are row ids; a majority or sets row is typed per group as "rowId.groupId"
  ]
}
""".trim()

    private val RULES = """
Rules:
- Only scoring done when the game ends belongs on the sheet. A score track marked during play is one "points" row ("Score track"), followed by the rows the end of the game adds.
- If the game has no end-of-game scoring to add up (scored only during play, a cooperative game without points, or the winner is whoever reaches a goal first), answer {"noScoreSheet": "one line saying why"} instead.
- Use only what the rulebook says. Never invent scoring rules, point values or tiebreakers.
- Prefer "count", "table", "majority" and "sets" rows that do the arithmetic over "points" rows that make players work it out; use "points" for values printed on cards or tiles that players add up.
- Each "hint" is one short line players can follow without the rulebook; paraphrase, do not copy sentences.
- Penalties: a "count" row with a negative pointsEach, or a "points" row with "subtract": true. Never ask players to type a negative number.
- Use modules only for expansions or variants this PDF describes that add or change scoring; their rows get "module".
- Work out each example's totals from the rulebook's rules by hand, not from your rows; the app recomputes them from the rows and rejects the sheet when they differ.
- Write in English, even when the rulebook is in another language.
""".trim()
}
