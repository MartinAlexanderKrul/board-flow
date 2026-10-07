package cz.nicolsburg.boardflow.data.setupguide

import cz.nicolsburg.boardflow.model.GuideOrigin
import cz.nicolsburg.boardflow.model.SetupGuide
import org.json.JSONObject

/**
 * The prompt for drafting a Quick Setup guide from a rulebook PDF, and the checks on what comes
 * back. Pure, so it is unit-tested; the HTTP side is [GeminiGuideDraftGenerator].
 */
object GuideDraftPrompt {

    fun build(gameId: Int, gameName: String, problems: List<String> = emptyList()): String = buildString {
        appendLine("The attached PDF is the rulebook of the board game \"$gameName\" (BoardGameGeek id $gameId).")
        appendLine("Write a setup cheat sheet for it as one JSON object in the format below: box -> table -> first turn.")
        appendLine("It is for people who already know how to play, so it is not a tutorial or a rules summary.")
        appendLine()
        appendLine(FORMAT)
        appendLine()
        appendLine(RULES)
        appendLine()
        appendLine("Use gameId $gameId and gameName \"$gameName\". Answer with the JSON object only.")
        if (problems.isNotEmpty()) {
            appendLine()
            appendLine("Your previous answer was rejected for these problems. Fix all of them:")
            problems.take(20).forEach { appendLine("- $it") }
        }
    }

    /**
     * Turns the model's answer into a guide for [gameId]: strips code fences, forces the ids,
     * version and AI provenance, and returns the guide with the validator's problems (empty when
     * the guide is usable). Throws when the answer is not a guide at all.
     */
    fun parse(text: String, gameId: Int, gameName: String, model: String?, sourceName: String?): Pair<SetupGuide, List<String>> {
        val body = text.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = body.indexOf('{')
        val end = body.lastIndexOf('}')
        require(start >= 0 && end > start) { "The answer has no JSON object" }
        val parsed = SetupGuideJson.parse(JSONObject(body.substring(start, end + 1)))
        val guide = parsed.copy(
            schemaVersion = SetupGuide.CURRENT_SCHEMA_VERSION,
            gameId = gameId,
            // Ids the model "knows" are guesses; a wrong one would open this guide for another game.
            aliasGameIds = emptyList(),
            modules = parsed.modules.map { it.copy(bggId = null) },
            gameName = gameName,
            version = 1,
            provenance = parsed.provenance.copy(
                origin = GuideOrigin.AI_DRAFT,
                author = null,
                sources = listOfNotNull(sourceName?.takeIf { it.isNotBlank() }),
                basedOnVersion = null,
                aiModel = model,
                reviewed = false
            )
        )
        return guide to SetupGuideValidator.validate(guide)
    }

    private val FORMAT = """
Format (JSON, comments are explanations only and must not appear in the answer):
{
  "schemaVersion": 2,
  "gameId": 0,
  "gameName": "",
  "version": 1,
  "players": { "min": 1, "max": 4 },          // add "exclude": [5] for counts in the range the game does not support
  "modules": [                                 // one per expansion or optional variant the rulebook covers; [] if none
    {
      "id": "kebab-case-id",                   // unique in the guide
      "name": "Expansion name",
      "defaultEnabled": false,
      "players": { "min": 1, "max": 2 },       // optional: only offered at these counts
      "extendsMaxPlayers": 6,                  // optional: unlocks higher player counts
      "forcedAtPlayers": [1],                  // optional: always on at these counts, e.g. a solo mode
      "group": "Mode",                         // optional: modules sharing a group are one choice and exactly one of them is always on
      "requires": [], "excludes": [],
      "note": "optional one line shown while the module is on"
    }
  ],
  "sections": [
    {
      "id": "board", "title": "Board",
      "kind": "SETUP",                         // SETUP (checklist), REMINDERS (easy to forget), START, or CHARACTERS
      "condition": { "modules": ["x"] },       // optional, hides the whole section
      "steps": [
        {
          "id": "forest",                       // kebab-case, unique in the whole guide
          "text": "Place {n} Forest cards",     // {n} is filled from amounts
          "amounts": { "n": { "default": 4, "byPlayers": { "2": 3 },
                              "cases": [ { "when": { "players": [2], "modules": ["x"] }, "value": 4 } ] } },
          "condition": { "minPlayers": 2, "maxPlayers": 4, "players": [2, 3], "modules": ["x"], "notModules": ["y"] },
          "note": "optional one line that is easy to miss",
          "ref": "p.5",                         // rulebook page or section the step comes from
          "details": ["..."]                    // only in CHARACTERS sections, see below
        }
      ]
    }
  ]
}
A step is shown only when all parts of its condition hold; there is no OR, so write two steps instead.
To replace a base step when a module is on, give it "notModules": ["x"] and put the replacement next to it with "modules": ["x"].
An amount is a plain value ("n": 8) or a rule object: the first matching case wins, then byPlayers, then default. Values may be text.
""".trim()

    private val RULES = """
Rules:
- Use only what the rulebook says. Never add rules, components or quantities that are not in it.
- Paraphrase; do not copy sentences from the rulebook.
- Cover the whole setup in the order it is done, then the first turn. Leave out strategy and the full rules.
- Do not list the box contents; every SETUP step is something to do.
- Spell out every quantity the rulebook gives (cards, tokens, ships and buildings per system, cards to deal). Never send the reader to a card or the rulebook for a number the rulebook states.
- Put every amount you define into its step text as {name}; an amount the text does not show is lost.
- Take setup numbers from the setup section. The same action later in the game often uses other numbers (cards dealt at setup vs drawn on a turn); do not mix them up, and do not use numbers from other editions of the game.
- Each step is one short line with its quantity in it; use amounts with byPlayers for anything that depends on the player count.
- Put things people often forget in one REMINDERS section titled "Easy to forget" (rules that are easy to miss during play, at most 6), and keep the START section titled "Start playing" to 1-3 steps.
- When the game has characters with their own abilities (heroes, factions, roles), add one CHARACTERS section titled after them (e.g. "Heroes", "Factions") listing every one: "text" is the character's name, "details" is a list of short lines, one per ability, starting with the ability's name ("Backstab: wins any combat that would be a tie"). Characters from an expansion get that module as their condition. Do not repeat these abilities in REMINDERS.
- Give every step a "ref" with the rulebook page or section.
- Use modules only for expansions or variants this PDF actually describes.
- Only use "group" for alternatives where exactly one must be chosen (e.g. Competitive / Co-op). Optional scenarios or variants are separate modules without a group.
- Write in English, even when the rulebook is in another language.
""".trim()
}
