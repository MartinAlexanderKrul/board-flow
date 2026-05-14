# AGENTS.md

## Purpose

This repository contains the BoardFlow Android app.

Agents working here should preserve the current user-facing design language while improving correctness, maintainability, and architectural clarity. The app has already accumulated several interconnected flows, so the main job is usually to make the existing product work more coherently rather than to reinvent it.

## Product Snapshot

BoardFlow currently supports all of the following:

- local and online BGG play logging
- offline-first local play saving
- manual reposting of unposted local plays from History
- edit and delete play flows
- play quantity, incomplete flag, and nowInStats toggle
- AI score extraction from images with Gemini (with model fallback/cycling)
- saved player roster with aliases, optional BGG usernames, and Levenshtein fuzzy matching
- collection browsing across owned / wishlist / sleeves
- per-game sleeve exclusion (toggle individual games out of sleeve display)
- configurable sleeve manufacturer priority (Appearance settings)
- game detail drill-ins with history and player links
- expansion / sibling title detection and display in log flow
- record moment detection after logging (first win, new high score, win streak)
- history tabs for plays, stats, and players
- signature-based deduplication of local and BGG plays
- QR code play sharing and import
- Google Sheets / Drive sync
- CSV import/merge into a sheet
- spreadsheet creation / connection
- Drive folder and QR code creation
- backup export / import
- session continuation / play-again flows

When changing the app, keep those flows in mind. A fix in one area often has consequences for history, roster matching, sync, or backup behavior.

## Fast Start

If you are new to the repo, do not start with a whole-codebase read. Start here:

- `README.md`
  - current product behavior, runtime storage model, major screen flows
- `app/src/main/kotlin/cz/nicolsburg/boardflow/ui/app/AppShell.kt`
  - top-level navigation, scaffold, cross-screen routing
- `app/src/main/kotlin/cz/nicolsburg/boardflow/AppViewModel.kt`
  - search, log play, history, roster, local play persistence, outbox posting, import/export, record moments
- `app/src/main/kotlin/cz/nicolsburg/boardflow/SyncViewModel.kt`
  - Google account/sheet state, collection refresh, sleeve refresh, sync log, canonical collection loading
- `app/src/main/kotlin/cz/nicolsburg/boardflow/data/CanonicalCollectionStore.kt`
  - live Room-backed store
- `app/src/main/kotlin/cz/nicolsburg/boardflow/data/BggRepository.kt`
  - BGG search, collection, play history, log/edit/delete flows
- `app/src/main/kotlin/cz/nicolsburg/boardflow/data/GoogleApiClient.kt`
  - Sheets / Drive sync

Prefer targeted inspection of those files over broad exploration unless the issue clearly spans multiple layers.

## Current Architecture

- `MainActivity.kt`
  - thin Android entry point
  - lifecycle hooks
  - activity-result launchers
  - auth wiring
- `ui/app/AppShell.kt`
  - app scaffold
  - header
  - bottom nav (5 tabs: NewPlay, History, Collection, Players, Settings)
  - screen routing
  - cross-screen deep-link style callbacks between Collection, History, Players, and Log Play
  - consumes `pendingHistoryNavigation` requests from `AppViewModel`
- `auth/GoogleAuthManager.kt`
  - Google account selection / sign-in orchestration
- `core/di/AppContainer.kt`
  - lightweight manual DI container
- `core/navigation/AppRoutes.kt`
  - central route definitions
- `AppViewModel.kt`
  - game search and recent games
  - session continuation / play-again setup
  - AI extraction handoff into review; Gemini model cycling
  - editable log state integration (shared between log and edit flows)
  - roster and player alias management; fuzzy matching with Levenshtein distance
  - local play history and cached BGG history merge and deduplication (signature-based)
  - play post/edit/delete flows (local and BGG)
  - local outbox posting for unposted plays (per-play and bulk)
  - record moment detection (first win, new high score, win streak)
  - expansion / sibling title detection (`GameRelations`)
  - cross-tab navigation requests (`pendingHistoryNavigation`)
  - import/export and backup restore
  - app theme and sleeve manufacturer preference state (`appTheme`, `sleevePreferredManufacturer`)
- `SyncViewModel.kt`
  - Google auth state
  - spreadsheet connection state
  - collection refresh (BGG + Sheets + sleeves, with play count backfill from history)
  - sleeve refresh and per-game exclusion state
  - CSV import/merge
  - Drive folder and QR code creation
  - full Sheets sync
  - sync log / progress state
  - silent startup collection load (gated: only runs if last sync was more than 4 hours ago via `securePrefs.lastSyncedAt`)
- `data/`
  - `BggApiClient.kt` -- low-level BGG HTTP/XML client and sleeve scraping
  - `BggRepository.kt` -- BGG feature layer (collection, search, play CRUD, history)
  - `GoogleApiClient.kt` -- Sheets / Drive API
  - `GeminiRepository.kt` -- AI extraction, model discovery, fallback cycling
  - `CanonicalCollectionStore.kt` -- Room-backed live source of truth
  - `BackupSerializer.kt` -- backup JSON import/export
  - `SecurePreferences.kt` -- encrypted preferences (credentials, settings, roster, session)
  - `QrGenerator.kt` -- QR code PNG generation and gallery save
  - `PlayShareSerializer.kt` -- play encode/decode for QR sharing
  - `BggImageCache.kt` -- thumbnail preload after sync
  - `BggCache.kt` -- file-based BGG collection cache
  - `BggPlaySync.kt` -- top-level BGG play cache refresh function
  - `CsvParser.kt` -- CSV row parsing for sync
- `model/`
  - `Models.kt` -- all core data classes (`GameItem`, `LoggedPlay`, `Player`, `SessionContext`, `RecordMoment`, ...)
  - `SleeveDatabase.kt` -- `SleeveManufacturer` enum, `SleeveEntry`, `SleeveDatabase` object
- `ui/`
  - screens and shared Compose UI helpers

## Source Of Truth

### Live Runtime Data

The live runtime source of truth is Room via `CanonicalCollectionStore`.

It stores:

- canonical merged collection snapshot (`GameItem` records)
- local logged plays
- cached BGG play history

### Preferences / Settings

`SecurePreferences` stores:

- BGG credentials (username, password)
- Gemini key, model endpoint, available models cache
- app theme (`app_theme`, enum name string)
- sleeve priority manufacturer (`sleeve_preferred_manufacturer`, `SleeveManufacturer` enum name)
- player roster (display names, aliases, BGG usernames)
- recent games (last 50)
- sync preferences (spreadsheet ID, sheet tab name, Google email)
- session context (active game, players, location, timestamp)
- sleeve exclusion list (game IDs)
- per-game insight key cache

Do not move live collection/history state back into large JSON blobs in preferences.

### Backup Format

`BackupSerializer` owns import/export JSON (format version 2).

Backups can contain:

- collection snapshot (full `GameItem` array under `collectionSnapshots.__canonical_collection__`)
- local logged plays
- cached BGG plays
- player roster
- recent games
- sleeve exclusions
- settings (theme, spreadsheet config, sleeve manufacturer preference)
- optionally sensitive data (BGG password, Gemini API key)

Import is selective: only keys present in the backup JSON are applied; missing keys do not overwrite existing values.

## Key Product Flows To Understand

### Search -> Log Play

- `NewPlayScreen` loads recent games and local collection-backed search first
- if no local collection result matches, search falls back to BGG XML search
- BGG search covers both base games and expansions (`type=boardgame,boardgameexpansion`)
- BGG search results are sorted alphabetically; the list has no hard result cap
- search is debounced (800ms after typing stops); local collection is checked first; if no local match, BGG is called with `exact=1` first, then `exact=0` as a fallback if exact returns nothing
- `isBggSearchActive` in `AppViewModel` prevents `loadCollection`, `updateFromCollection`, and `loadRecentGames` from overwriting `_searchResults` while a BGG search result set is displayed; the guard clears when the user selects a game or clears the query
- lists longer than 20 items show a draggable fast-scroll bar on the right edge (`NewPlayScreen.FastScrollBar`): amber pill thumb, animated opacity (idle 20% / scrolling 65% / dragging 80%), floating letter bubble that leads the thumb position, and haptic feedback (`HapticFeedbackType.TextHandleMove`) per letter section change
- selected games move into `LogPlayScreen`
- session context may prefill players/location
- AI extraction may prefill players/scores
- expansion / sibling titles detected from name patterns and shown alongside base game

### Log Play -> Local / BGG

- if online with credentials, the app can post to BGG
- if offline or posting is unavailable, the play can still be saved locally
- extra related games may post separately; failures there can leave local follow-up plays
- local unposted plays are intentionally user-controlled from History rather than silently auto-posted on startup
- after posting, `AppViewModel` detects record moments (first win, new high score, win streak) by comparing against play history snapshot

### Play Deduplication (Merge Logic)

- `AppViewModel.historyPlays` is the merged, deduplicated derived flow
- two dedup strategies run in sequence:
  - **signature key**: full play hash (game, date, players with scores/colors, location, comments)
  - **history correlation key**: lighter hash (game, date, players, colors) used when score info may differ
- local plays that match a fetched BGG play are marked as posted; truly orphaned local plays that were deleted on BGG are pruned

### History

- History merges local logged plays with cached BGG plays
- local-only plays can be deleted locally
- BGG plays can be deleted remotely
- edit flow updates local state and remote BGG when needed
- History includes:
  - `Plays`
  - `Stats`
  - `Players`
- the Plays tab also acts as the outbox surface for unposted local plays

### Collection

- collection data is loaded through sync and propagated from the canonical Room snapshot
- tabs include:
  - `Owned`
  - `Wishlist`
  - `Sleeves`
- game detail dialog is a major cross-link hub into History and Players
- sleeve display respects per-game exclusion toggles

### Sync

- Sync screen is the user-facing operational hub for:
  - BGG readiness
  - Google readiness
  - sheet connection
  - refresh collection (BGG + Sheets + sleeves; backfills play counts from cached history)
  - refresh sleeve sizes
  - sync to Sheets
  - CSV import/merge
  - create Drive folders / QR codes (with optional gallery save)
  - create/connect spreadsheet
  - review sync log

### Settings

- manages BGG credentials
- manages Google sheet connection access points
- manages Gemini configuration (key, model endpoint, model discovery)
- manages theme (Light, Dark)
- manages sleeve manufacturer priority (`SleeveManufacturer`; persisted in `SecurePreferences`, exposed via `AppViewModel.sleevePreferredManufacturer`; used in `GameDetailDialog` via `SleeveEntry.preferredFor()`)
- manages import/export
- can clear cached collection

### QR Play Sharing

- `PlayShareSerializer` encodes a `LoggedPlay` for QR
- `QrGenerator` produces a QR PNG and can save to the device gallery
- `QrPlayImportScreen` lets users scan/paste a QR play and confirm import
- import lands in `AppViewModel.pendingImportedPlay`; user confirms before saving locally

### Record Moments

- `AppViewModel.captureHistorySnapshot()` snapshots play history before posting
- `AppViewModel.detectRecord()` compares the new play against the snapshot:
  - **FirstWin**: first-ever win for this game by this player
  - **NewHighScore**: score exceeds previous best for this player in this game
  - **WinStreak**: player has won 2 or more consecutive plays of this game
- result surfaced in `LogPlayScreen` after a successful post

## What Usually Matters

- preserve the current visual hierarchy unless the user explicitly asks for a redesign
- keep merge logic source-aware:
  - BGG owns identity, stats, BGG links, ownership flags, BGG play count, and remote history
  - Google Sheets owns spreadsheet-specific/manual sheet values and links
  - sleeve refresh owns sleeves only
- full sync should update the canonical merged snapshot once at the end
- local/offline history should not mutate canonical collection state
- local unposted plays should remain visible and user-controlled
- BGG XML search outside the loaded collection requires the XML API token and should fail quietly to an empty result state if missing/rejected
- player matching should stay explicit unless a match is truly exact
- sleeve exclusions are per-game and stored in `SecurePreferences`; respect them in both display and export

## Expectations For Changes

- keep the existing visual layout and design language unless explicitly asked to redesign
- prefer small structural fixes over broad rewrites
- keep `MainActivity` thin
- keep navigation concerns in `ui/app` or `core/navigation`
- keep business logic out of composables
- prefer `StateFlow` and unidirectional data flow
- avoid introducing new overlapping sources of truth
- do not reintroduce deprecated Google sign-in APIs
- if a task is localized, stay within that feature area unless the bug clearly crosses layers

## BGG Flow Notes

- both authenticated and unauthenticated collection flows exist
- play posting/edit/delete is authenticated; uses cookie-based session persistence in `BggApiClient`
- retry and error handling exist in the repository layer
- BGG XML search outside the local collection requires `BGG_XML_API_TOKEN`
- if BGG XML token-based search is unavailable, fail quietly to empty results instead of noisy user-facing errors where possible
- `BggRepository.searchGames` accepts an `exact: Boolean` parameter that maps to `exact=1` / `exact=0` in the BGG XML API URL; `AppViewModel.searchGames` cascades exact=1 then exact=0 automatically
- search covers `type=boardgame,boardgameexpansion` so expansions appear alongside base games in external search results

## History / Roster Notes

- saved roster players are distinct from arbitrary logged names
- history rows should show all logged players, even if names are similar
- general stats may treat unsaved names differently from roster views
- roster-oriented views should stay roster-based
- editable player rows should use stable UI identity and survive configuration changes where practical
- fuzzy player matching uses Levenshtein distance (threshold ~1/3 of input length); matches shown as suggestions, not auto-applied

## Sleeve Notes

- `SleeveManufacturer` enum lives in `SleeveDatabase.kt` (7 values: AUTO, TLAMA_DIAMOND, PALADIN, ULTRA_PRO, SAPPHIRE, SLEEVE_KINGS, ARCANE_TINMEN)
- `SleeveEntry.preferredFor(manufacturer)` returns the (brand, productName) pair for the chosen brand, falling back to the best available if that brand does not carry this size
- the user-selected manufacturer is read from `SecurePreferences.sleevePreferredManufacturer`, exposed as `AppViewModel.sleevePreferredManufacturer: StateFlow<SleeveManufacturer>`
- `GameDetailDialog` reads it at composition time and passes it through `SleevesBlock` -> `SleevesSection`
- per-game exclusions are stored in `SecurePreferences` as a `Set<String>` of game objectIds; managed via `SyncViewModel.toggleSleeveGameExclusion` / `excludeAllSleeveGames` / `includeAllSleeveGames`

## UI Conventions

- preserve the current screen hierarchy and tab layout
- prefer extracting small reusable helpers when a screen starts carrying duplicated framework glue
- avoid unsafe `!!` access in composables when nullable state can be handled cleanly
- keep screen parameters minimal and explicit
- use saveable state for meaningful in-progress screen state
- keep user-facing strings and docs in UTF-8, but prefer plain ASCII punctuation when practical
- be careful with PowerShell bulk replacements; they can introduce mojibake like `Ã‚Â·`, `Ã¢â‚¬Â¦`, or `ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦`
- if encoding corruption appears in working changes, fix it before continuing

### Modal Pattern Example

```kotlin
@Composable
fun SpreadsheetConnectModal(
    currentSheetName: String?,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit,
    onCreateNew: (() -> Unit)? = null
) { ... }
```

## Build / Verification

Before finishing substantial changes, run:

```sh
./gradlew.bat :app:compileDebugKotlin
```

Also use this when startup behavior, resources, or packaging may be affected:

```sh
./gradlew.bat :app:assembleDebug
```

If you only changed docs or a very small behavior fix, compile is usually enough.

## Important Runtime Note

Google sign-in and Google Sheets / Drive access depend on external Firebase / Google Cloud OAuth configuration. A successful compile does not guarantee runtime sign-in success if SHA fingerprints or OAuth client setup are wrong.

## Dependency Notes

Current notable choices:

- Java 17 / Kotlin JVM target 17
- Compose + Material 3
- Navigation Compose
- Credential Manager + Google Identity
- OkHttp
- CameraX
- Coil
- Room

Avoid adding Retrofit / Moshi back unless there is a clear need; they were removed as unused.

## Refactor Guidance

If continuing modernization, the most valuable current targets are:

- split `AppViewModel` and `SyncViewModel` into smaller feature-oriented state holders
- move models out of the catch-all `Models.kt` into more focused files
- replace heuristic history reconciliation with stronger explicit sync identity
- further reduce duplicated collection state / bridging between view models
- add clearer UI state models for mixed loading/data/error screens
- improve Google sign-in diagnostics with device-tested logging if runtime issues continue

When in doubt, inspect the targeted feature files first and avoid a whole-repo read unless the bug really spans sync, storage, and UI together.
