# BoardFlow Quick Setup guides

Setup cheat sheets for games you already know how to play: **box -> table -> first turn**.
They are not rulebooks or tutorials.

This folder does two jobs:

- **Bundled.** `app/build.gradle.kts` copies `*.json` from here into the APK as
  `assets/setup-guides/`, so these guides work offline from the moment the app is installed.
- **Remote catalog.** The app reads
  `https://raw.githubusercontent.com/MartinAlexanderKrul/board-flow/master/setup-guides/index.json`
  (at most once a day) and downloads every guide whose `version` is newer than its local copy,
  so guides published after a release also work offline once the app has been online.
  Merging a change to `master` updates every install without an app release.

To add or fix a guide, open a pull request. Every guide must pass
`SetupGuideValidator`, which runs over this folder in `BundledSetupGuidesTest`:

```sh
./gradlew.bat :app:testDebugUnitTest
```

## Files

- `index.json` lists every guide: `gameId`, `gameName`, `gameIds` (the base game, its aliases and
  every module `bggId`; any of these ids opens the guide), `version`, `schemaVersion` and `path`.
- `<bggId>.json` holds one guide, named after the base game's BoardGameGeek id.

**When you change a guide, bump its `version` in both the guide file and `index.json`.** The app
only downloads a guide when the index version is higher than the copy it already has.

## Guide format (schemaVersion 1)

```jsonc
{
  "schemaVersion": 1,
  "gameId": 199792,                 // BGG id of the base game
  "aliasGameIds": [],               // other editions that share this setup (optional)
  "gameName": "Everdell",
  "version": 1,                     // content revision; bump on every change
  "provenance": { "origin": "BOARDFLOW", "author": "...", "sources": ["Everdell rulebook"] },
  "players": { "min": 1, "max": 4 },   // optional "exclude": [5] for unsupported counts in the range
  "modules": [
    {
      "id": "bellfaire",            // kebab-case, unique in the guide
      "name": "Bellfaire",
      "bggId": 289057,              // opening this BGG id preselects the module
      "defaultEnabled": false,
      "players": { "min": 1, "max": 2 },   // only offered at these counts (optional)
      "extendsMaxPlayers": 6,       // unlocks higher player counts (optional)
      "forcedAtPlayers": [1],       // always on at these counts, e.g. solo mode (optional)
      "group": "Mode",              // modules sharing a group are one choice, e.g. Competitive / Co-op (optional)
      "requires": [], "excludes": [],
      "note": "Shown while the module is on"
    }
  ],
  "sections": [
    {
      "id": "board", "title": "Board",
      "kind": "SETUP",              // SETUP (checklist) | REMINDERS ("Easy to forget") | START
      "condition": { "modules": ["pearlbrook"] },   // optional, hides the whole section
      "steps": [
        {
          "id": "forest",            // stable id: keeps ticks when the configuration changes
          "text": "Place {n} Forest cards",
          "amounts": { "n": { "default": 4, "byPlayers": { "2": 3 },
                              "cases": [ { "when": { "players": [2], "modules": ["nightweave"] }, "value": 4 } ] } },
          "condition": { "minPlayers": 2, "notModules": ["pearlbrook"] },
          "note": "One line that is easy to miss",
          "ref": "Everdell p.5"      // rulebook page, for reviewers
        }
      ]
    }
  ]
}
```

### How conditions work

- A step is shown only when **all** parts of its `condition` hold: `players` (a list of counts),
  `minPlayers`, `maxPlayers`, `modules` (all must be on) and `notModules` (none may be on).
- There is no OR. To say "A or B", write two steps with different ids.
- To replace a base step when an expansion is on, give the base step `notModules: ["x"]` and put
  the replacement step at the same position with `modules: ["x"]`.

### How amounts work

- An amount is either a plain value (`"n": 8`) or a rule object.
- For a rule object, the first matching `cases` entry wins, then `byPlayers`, then `default`.
- Values can be text, e.g. `"5, 6, 7"` for per-seat hand sizes.

## Writing rules

- Cover **every published gameplay expansion**, not only the ones you own: one module per
  expansion, with its BGG id. Leave out promo cards, component upgrades and fan packs, and say in
  the pull request which expansions were left out and why (not released yet, no public rulebook,
  sealed campaign).

- Only include what gets the game onto the table and through the first turn. Leave out strategy
  and full rules.
- Paraphrase. Never paste rulebook text.
- Keep each step to one short line with the quantity in it.
- Put anything people forget under `REMINDERS`, and keep `START` to 1-3 steps.
- Add a `ref` to every step so reviewers can check it against the rulebook.
