# AGENTS.md

Guidance for AI agents and contributors changing the BoardFlow code. For a product overview, build setup and the documentation index, see [`README.md`](README.md); for every screen and shared component, see [`docs/UI_SURFACES.md`](docs/UI_SURFACES.md).

## Purpose

This repository contains the BoardFlow Android app (version 6, released on GitHub; Google Play release material in `play-store/`).

Agents working here should preserve the current user-facing design language while improving correctness, maintainability, and architectural clarity. The app has already accumulated several interconnected flows, so the main job is usually to make the existing product work more coherently rather than to reinvent it.

## Product Snapshot

BoardFlow currently supports all of the following:

- local and online BGG play logging
- offline-first local play saving
- unposted local plays post automatically when the device is online (`BggPlayPostWorker`), or by hand from History
- edit and delete play flows
- play quantity, incomplete flag, and nowInStats toggle
- AI score extraction from images with Gemini (with model fallback/cycling), preceded by a local non-blocking scan quality warning for obviously dark, blurry, low-resolution, or too-far images; malformed responses automatically trigger a silent background retry — if it succeeds while the user is still on `LogPlayScreen`, a non-blocking banner offers to apply the cleaner result
- the model name used for a scan is stored on `ExtractedPlay.modelUsed` and shown below the "Raw AI response" label in the AI output card
- AI game recognition from scan: auto-identify the game using saved recognition templates (title similarity + category fingerprint matching, two-gate autoswitch: TITLE_GATE >= 0.90 or TEMPLATE_CATEGORY_GATE >= 0.75 with >= 3 category matches)
- home-screen widgets: `SessionWidget` (last played session), `DailyInsightWidget` (rotating stat insights), and `StatsWidget` (plays this month + active challenge progress); all three include a camera button that cold-starts the app into Quick Scan via `ACTION_QUICK_SCAN`
- saved player roster with aliases, optional BGG usernames, and Levenshtein fuzzy matching
- collection browsing on a single My Shelf tab with owned / wishlist / played filter dimensions, plus sleeves
- per-game sleeve exclusion (toggle individual games out of sleeve display)
- configurable sleeve manufacturer priority (Settings > Preferences)
- game detail drill-ins with history and player links
- expansion / sibling title detection and display in log flow; `RelatedGamesBanner` is a single scrolling row of chips under the play details
- record moment detection after logging (first win, new high score, win streak)
- session memory: per-play mood chips (multi-select, preset + custom) and quote capture from `PlayDetailsDialog`
- chronicle generation: AI-generated single atmospheric sentence per session using Gemini, with deterministic offline fallback; stored in `play_memories` Room table; persists independently of BGG sync; togglable via Settings > Preferences
- Journal tabs for plays, challenges, stats (a Plays / Collection switch) and players
- player avatar colours (`Player.color`)
- one dark amber design system, with motion between screens and tabs (`NavTransitions`, `BoardFlowTabContent`)
- signature-based deduplication of local and BGG plays
- QR code play sharing and import
- Google Sheets / Drive sync
- CSV import/merge into a sheet
- spreadsheet creation / connection
- Drive folder and QR code creation
- backup export / import
- session continuation / play-again flows
- Quick Setup cheat sheets per game (box -> table -> first turn): player-count and expansion/module-aware steps with quantities, a session-only checklist, "Easy to forget" and "Start playing" cards, and a `Start game` action that starts the play timer. Guides are portable JSON in the repo-root `setup-guides/` folder, bundled into the APK and served as a remote catalog from GitHub
- challenge notifications via `ChallengeNotificationWorker` (WorkManager): deadline warning (3 days out), completion, and streak-broken alerts; gated on `POST_NOTIFICATIONS` permission

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
  - lifecycle hooks, activity-result launchers, auth wiring
  - handles `ACTION_QUICK_SCAN` from home-screen widget in both `onCreate` (cold start) and `onNewIntent` (resumed); calls `appViewModel.requestWidgetQuickScan()`
  - `launchMode="singleTop"` so widget taps call `onNewIntent` when the app is already running
- `ui/app/AppShell.kt`
  - app scaffold
  - header
  - bottom nav (4 tabs: NewPlay, History, Collection, Settings); Settings tabs are Sync, Preferences, Scan, Data. Sync is the first (`SettingsScreen(syncContent = { SyncScreen(...) })`) and also covers the accounts: its status rows sign in to Google, edit the BGG account and connect the sheet. The setup guide link is in Preferences
  - screen routing
  - cross-screen deep-link style callbacks between Collection, History, Players, and Log Play
  - consumes `pendingHistoryNavigation` requests from `AppViewModel`
  - consumes `pendingWidgetQuickScan` to navigate to the scan flow when the widget is tapped
  - header play-timer indicator (`AppHeader`, shown while `activeTimer != null`): tapping it
    opens Log Play for the timed game (clock keeps running, duration prefilled via
    `takePrefill()`); long-press shows a "Stop timer?" confirmation that calls `stopPlayTimer()`
- `auth/GoogleAuthManager.kt`
  - Google account selection / sign-in orchestration
- `core/di/AppContainer.kt`
  - lightweight manual DI container, one per process (`AppContainer.get(context)`): the activity, its view models and workers share the same repositories, so work a worker saves (a drafted guide) shows in an open screen at once
- `core/navigation/AppRoutes.kt`
  - central route definitions
- `AppViewModel.kt`
  - game search and recent games
  - session continuation / play-again setup
  - AI extraction handoff into review; Gemini model cycling; `scanRetryResult: StateFlow<ExtractedPlay?>` — set when a background retry resolves a previously malformed scan; `acceptRetryResult()` replaces the current extracted play and re-initialises editable players; `dismissRetryResult()` discards it; `cancelBackgroundRetry()` called automatically on save or discard
  - game recognition: `GameRecognitionEngine` ranks candidates from the local collection; confirmed scans saved as `GameRecognitionHint`; two autoswitch gates (TITLE_GATE, TEMPLATE_CATEGORY_GATE)
  - recognition hint management: `saveGameRecognitionHint`, `deleteGameRecognitionHint`, `replaceGameRecognitionHint`, `clearGameRecognitionHints`
  - player recognition: `PlayerRecognitionEngine` resolves scanned names to roster players; `initEditablePlayers` applies hints (confidence ≥ 0.70) and alias matches; `savePlayerHintsFromCurrentPlay` persists mappings on successful log; `clearPlayerRecognitionHints` exposed for Settings
  - `pendingWidgetQuickScan` StateFlow consumed by AppShell to navigate to scan on widget tap
  - editable log state integration (shared between log and edit flows)
  - roster and player alias management; fuzzy matching with Levenshtein distance
  - local play history and cached BGG history merge and deduplication (signature-based); sorted by date DESC, id DESC
  - play post/edit/delete flows (local and BGG)
  - local outbox posting for unposted plays (per-play and bulk)
  - record moment detection (first win, new high score, win streak)
  - session memory and chronicle: `savePlayMemory()` persists moods/quote to `play_memories` and triggers chronicle generation; `ensureChronicleForPlay()` auto-generates when opening a play with memory but no chronicle; concurrency managed via `chronicleJobs`, `chronicleInFlightSourceKeys`, `chronicleGenerationLock`; `chroniclePendingPlayIds: StateFlow<Set<String>>` drives the `...` placeholder; `chronicleEnabled: StateFlow<Boolean>` gates all generation and display; custom mood management: `addCustomMoodIfNew`, `deleteCustomMood`
  - expansion / sibling title detection (`GameRelations`); `findRelatedGames` uses `isExpansionOf()` helper supporting both separator-based (`"Root: Sub"`) and space-prefix-based (`"Root Sub"`) expansion names; detects when the selected game is itself a space-separated expansion and treats its prefix as the base
  - cross-tab navigation requests (`pendingHistoryNavigation`)
  - import/export and backup restore
  - sleeve manufacturer preference state (`sleevePreferredManufacturer`)
- `SyncViewModel.kt`
  - Google auth state
  - spreadsheet connection state
  - collection refresh (BGG + Sheets + sleeves, with play count backfill from history)
  - sleeve refresh and per-game exclusion state
  - CSV import/merge
  - Drive folder and QR code creation
  - full Sheets sync
  - sync log / progress state
  - every `runSync` action (refresh collection, sleeves, Sheets, CSV, folders, sleeve backup) runs with `SyncForegroundService` started: it does no work, it only holds a foreground notification ("Refresh collection") so Android does not freeze the process and its network when the user switches apps mid-sync. When the sync ends it is stopped (directly, not with another `startService`, which Android may refuse from the background) and, if the app is not on screen, `SyncForegroundService.finished` posts "X finished" / "X failed" with the last error from the sync log; tapping it opens Settings > Sync. `AppShell` asks for the notification permission when a sync starts (`rememberNotificationPermissionRequest`)
  - silent startup collection load (gated: only runs if last sync was more than 4 hours ago via `securePrefs.lastSyncedAt`; writes `lastSyncedAt` on successful completion so the gate is honoured on subsequent startups)
- `data/`
  - `BggApiClient.kt` -- low-level BGG HTTP/XML client and sleeve scraping
  - `BggRepository.kt` -- BGG feature layer (collection, search, play CRUD, history)
  - `GoogleApiClient.kt` -- Sheets / Drive API. Scopes are `SyncConfig.OAUTH_SCOPES` (used by both `GoogleAuthManager` and the API client): `drive.file` (only the BoardGames folders and QR images the app creates) and `spreadsheets`. Do not widen to the full `drive` scope: it is restricted and would need a security assessment for the Play release
  - `GeminiRepository.kt` -- AI extraction (scores + game detection), model discovery, fallback cycling; debug-logged under TAG "Gemini"; sets `ExtractedPlay.modelUsed` to the winning model name and `ExtractedPlay.isMalformed = true` when JSON parsing fails
  - `GeminiModels.kt` -- model selection shared by scan and chronicle; no versioned model name is hardcoded. The stored model preference defaults to blank (`GeminiModels.AUTO`): candidates are the `gemini-flash-latest` / `gemini-flash-lite-latest` aliases plus the key's cached model list, ranked stable > preview, Flash > Flash-Lite (Flash-Lite first for chronicles), newest version first. Pro, TTS, image and other specialised models are filtered out by name (`SPECIALISED`); `scripts/gemini_model_probe.py` sends the real scan and chronicle requests to every listed model to re-check that filter. A pinned model goes first with the automatic chain behind it. HTTP 404 (retired model) or a 400 saying the model cannot take image/JSON requests rotates to the next candidate, drops the model, clears the pin if it was the pinned one (`SecurePreferences.markGeminiModelUnavailable`) and refreshes the list; the list also refreshes when older than 24 h at scan time
  - `ScanImageQualityAnalyzer.kt` -- local pre-Gemini image readability checks; does not persist image, player, or score data
  - `CanonicalCollectionStore.kt` -- Room-backed live source of truth (DB v13); stores canonical games, logged plays, BGG play cache, play sessions, play memories, thumbnail cache, players, challenges, game recognition hints, player recognition hints, sleeve tracking, sleeve inventory, and setup guides (`setup_guides`, `setup_guide_catalog`); `getBggPlaysCache()` and `getLoggedPlays()` apply the `play_memories` overlay on read, with `parseMemoryFromNotes()` as fallback for plays with `$$mood:`/`$$quote:` lines in comments
  - `setupguide/` -- Quick Setup: `SetupGuideJson` (org.json format mapping), `SetupGuideValidator` (structural checks applied to every guide before it is shown or stored), `SetupGuideResolver` (pure: guide + player count + modules -> visible steps with amounts filled in), `SetupGuideIndex`, `BundledSetupGuideSource` (APK assets), `SetupGuideCatalogClient` (GitHub raw, quiet failure), `SetupGuideRepository` (layer resolution USER > newer of BUNDLED/CATALOG, availability map keyed by base/alias/module BGG ids, daily index refresh, then downloads every catalog guide newer than the local copy so all guides work offline; `AppShell.openQuickSetup` shows "available when you're online" for a catalog guide that is not downloaded yet)
  - `SessionMemoryJson.kt` -- `toSessionMemoryOrNull()`, `toJsonString()`, `parseMemoryFromNotes()` extension functions
  - `chronicle/` -- chronicle service pipeline: `SessionChronicleService` (plan + compose), `GeminiChronicleLineGenerator` (Gemini API, 4 retries, model fallback, 2.5 s timeout), `FallbackChronicleComposer` (deterministic offline fallback), `ChronicleLineGenerator` interface, `ChronicleRequest` and `ChronicleAiConfig` data classes
  - `BackupSerializer.kt` -- backup JSON import/export (format version 8; includes `memory` JSON per play; exports players and challenges from Room, and the user's own setup guides)
  - `SecurePreferences.kt` -- encrypted preferences (credentials, settings, roster, session, recognition hints, `chronicle_enabled`, `custom_moods`)
  - `QrGenerator.kt` -- QR code PNG generation and gallery save
  - `PlayShareSerializer.kt` -- play encode/decode for QR sharing
  - `BggImageCache.kt` -- thumbnail preload after sync
  - `BggCache.kt` -- file-based BGG collection cache
  - `BggPlaySync.kt` -- top-level BGG play cache refresh function
  - `CsvParser.kt` -- CSV row parsing for sync
  - `BackgroundNotifications.kt` -- channels `background_work` (low, ongoing progress) and `background_results` (default), the progress and result notification builders, `canNotify`, `isAppVisible`, and the notification tap actions; `SyncForegroundService.kt` and `setupguide/GuideDraftWorker.kt` use it. The manifest declares `FOREGROUND_SERVICE_DATA_SYNC`, `SyncForegroundService` and WorkManager's `SystemForegroundService` with `foregroundServiceType="dataSync"`
  - `ChallengeNotificationWorker.kt` -- WorkManager worker; fires deadline (3 days out), completion, and streak-broken notifications per active challenge; gated on `POST_NOTIFICATIONS` permission; uses `challenge_notif` SharedPreferences to suppress re-fires
  - `BggSyncWorker.kt` -- WorkManager background sync worker
- `model/`
  - `Models.kt` -- all core data classes (`GameItem`, `LoggedPlay`, `Player`, `SessionContext`, `RecordMoment`, `GameRecognitionHint`, `PlayerRecognitionHint`, `GameCandidate`, ...); `ExtractedPlay` carries `isMalformed: Boolean` (parse failed, partial data) and `modelUsed: String?` (Gemini model that produced the response); `SessionMemory` carries moods, quote, `chronicleLine`, `chronicleSourceKey`, `chronicleCreatedAt`; `trimMemorySuffix()` strips `$$mood:`/`$$quote:` lines from BGG comments for display
  - `SleeveDatabase.kt` -- `SleeveManufacturer` enum, `SleeveEntry`, `SleeveDatabase` object
- `ui/`
  - screens and shared Compose UI helpers
  - `ui/widget/SessionsWidget.kt` -- `SessionGlanceWidget` (open base, all layouts) + `SessionWidget` receiver; shows last session
  - `ui/widget/DailyInsightWidget.kt` -- `DailyInsightGlanceWidget` (overrides `computeSnapshot`) + `DailyInsightWidget` receiver; shows rotating stat insights
  - `ui/widget/StatsWidget.kt` -- `StatsGlanceWidget` (overrides `computeSnapshot`) + `StatsWidget` receiver; shows plays this month, top game, and most urgent active challenge with deadline/progress; accent driven by deadline urgency

## Source Of Truth

### Live Runtime Data

The live runtime source of truth is Room via `CanonicalCollectionStore`.

It stores:

- canonical merged collection snapshot (`GameItem` records)
- local logged plays
- cached BGG play history
- session memories (`play_memories` table, keyed by play ID; applied as an overlay on read; never cleared by BGG sync; fallback parses `$$mood:`/`$$quote:` from BGG notes when no entry exists)
- players (`players` table; migrated from `SecurePreferences` on first load if Room is empty)
- challenges (`challenges` table; migrated from `SecurePreferences` on first load if Room is empty)
- game recognition hints (`game_recognition_hints` table)
- player recognition hints (`player_recognition_hints` table)
- sleeve tracking overrides (`game_sleeve_tracking` table)
- setup guides: downloaded catalog copies and the user's own guides (`setup_guides`, source `CATALOG` / `USER`), plus the cached remote index (`setup_guide_catalog`); bundled guides are read straight from assets and are not copied into Room

### Preferences / Settings

`SecurePreferences` stores:

- BGG credentials (username, password)
- Gemini key, model endpoint, available models cache
- app theme (`app_theme`; legacy, no longer read by the UI, still written to backups)
- sleeve priority manufacturer (`sleeve_preferred_manufacturer`, `SleeveManufacturer` enum name)
- player roster (legacy; still written for backup compatibility; Room is authoritative at runtime)
- recent games (last 50)
- sync preferences (spreadsheet ID, its Google name `sync_spreadsheet_title` shown in the Sync Accounts rows and fetched once after sign-in when missing, sheet tab name, Google email)
- session context (active game, players, location, timestamp)
- sleeve exclusion list (game IDs)
- per-game insight key cache
- chronicle enabled flag (`chronicle_enabled`; boolean; default true)
- player avatars in plays (`show_player_avatars_in_plays`; boolean; default true; Settings > Preferences > Logging plays; provided to play rows as `LocalShowPlayerAvatarsInPlays`)
- custom mood templates (`custom_moods`; JSON array of user-defined mood label strings)
- challenges (legacy; still written for backup compatibility; Room is authoritative at runtime)

Do not move live collection/history state back into large JSON blobs in preferences.

### Backup Format

`BackupSerializer` owns import/export JSON (format version 8).

Backups can contain:

- collection snapshot (full `GameItem` array under `collectionSnapshots.__canonical_collection__`)
- local logged plays (including embedded `memory` JSON object per play)
- cached BGG plays (including embedded `memory` JSON object per play)
- player roster (read from Room via `CanonicalCollectionStore.getPlayers()`)
- challenges (read from Room via `CanonicalCollectionStore.getChallenges()`)
- recent games
- sleeve exclusions
- AI game recognition templates
- AI player recognition templates
- the user's own setup guides (`setupGuides`, format 8; each entry is a guide document; restoring adds or replaces them by game)
- settings (theme, spreadsheet config, sleeve manufacturer preference)
- optionally sensitive data (BGG password, Gemini API key)

Import is selective: only keys present in the backup JSON are applied; missing keys do not overwrite existing values.

## Key Product Flows To Understand

### Search -> Log Play

- `NewPlayScreen` loads recent games and local collection-backed search first
- every game search (Log Play, the Quick Guides `My games` tab, My Shelf, the challenge game picker) looks only in the collection by itself and never calls BGG on its own. With 2+ characters typed, the results end with an amber `Search BoardGameGeek for "..."` line (`ui/common/BggSearch.kt` `SearchBggRow`; also under the "No games in your collection match" empty state), which runs `data/BggGameSearch` and shows `BggSearchSheet`. What picking a result does depends on the screen: Log Play selects the game (`selectGame` + `onGameSelected`, so correction mode and change game work the same), Quick Guides opens its Quick Setup (the name comes from `AppViewModel.rememberSearchedGame` / `knownGameName` for the draft screen), My Shelf opens it to add to the collection, the challenge picker sets the game (`bggOnlyGameItem` for one outside the collection; editing such a challenge rebuilds it from the stored name)
- `BggGameSearch` (one instance per ViewModel: `AppViewModel.bggGameSearch`, `SyncViewModel.shelfBggSearch`) asks BGG for `exact=1` and `exact=0` and merges them: exact matches first, then the rest by name, at most 50 (`merge`, unit-tested in `BggGameSearchTest`); it reports "Could not reach BoardGameGeek" when both requests fail and "Nothing found" when BGG answered empty
- BGG search covers both base games and expansions (`type=boardgame,boardgameexpansion`)
- the Log Play search is debounced (800ms after typing stops) and filters `logPlayPool` (collection plus played games); a sheet composed inside a Dialog (the challenge form) must sit inside the dialog's content, or its window opens behind the dialog
- lists longer than 20 items show a draggable fast-scroll bar on the right edge (`NewPlayScreen.FastScrollBar`): amber pill thumb, animated opacity (idle 20% / scrolling 65% / dragging 80%), floating letter bubble that leads the thumb position, and haptic feedback (`HapticFeedbackType.TextHandleMove`) per letter section change
- selecting a game opens `LogPlayScreen` directly (form first); scanning is the `Scan scores` button on the form, which returns to the same form with the typed details intact (`AppViewModel.prepareScanFromLogPlay`, `AppShell.scanOpenedFromForm`)
- session context may prefill players/location
- AI extraction may prefill players/scores
- `ScanScreen` runs `ScanImageQualityAnalyzer` before sending an image to Gemini; poor scans show a non-blocking "This scan may be hard to read." warning with a reason and "Use anyway" / "Retake" actions
- expansion / sibling titles detected from name patterns and shown alongside base game
- the search field has a trailing camera (`CameraAlt`) icon button that triggers the quick scan flow via `onScanQuick`; tapping it exits any active correction mode and navigates directly to `ScanScreen`

### Quick Scan Correction Flow

When AI recognition picks the wrong game after a scan, the user can tap "Choose another game" from the banner in `LogPlayScreen`:

1. `AppViewModel.enterQuickScanCorrectionMode()` sets `_quickScanCorrectionMode = true`
2. `AppShell` navigates back to `NewPlayScreen` without clearing `_extractedPlay` or `_editablePlayers`
3. `AppViewModel.selectGame()` short-circuits its normal clear logic while correction mode is active
4. When the user picks a replacement game, `applyDetectedGameCorrection()` reads the preserved `_extractedPlay`, calls `initEditablePlayers()` with the original extracted players, applies the new game, and clears correction mode
5. `AppShell` then navigates directly to `LogPlayScreen` — no new scan

If the user presses back from `NewPlayScreen` while in correction mode, `exitQuickScanCorrectionMode()` is called and they remain on `NewPlayScreen` (back does not exit to the previous screen in this state).

### Log Play -> Local / BGG

- every BGG write is optimistic: the app stores and shows the expected result at once and talks to BGG in the background (log play, edit, delete, rating, collection status, sleeve marker)
- `AppViewModel.postPlay` always saves the play locally first and reports success; when online with credentials, `postPlaysInBackground` then posts it and promotes it to the BGG id. Ids being posted sit in `_expectedPostedPlayIds`, which makes `historyPlays` show them as posted; a failed post drops them back into the unposted outbox
- a background post changes the play id from the local UUID to the BGG id, so `promotedPlayIds` / `currentPlayFor` let `savePlayMemory` follow a play object the UI captured before the post landed, and `postLocalPlay` carries memory and late comment changes over
- play posts are serialized by `playPostMutex` (History's per-play and bulk post use the same path) so a play is never posted twice
- if offline or posting is unavailable, the play can still be saved locally
- extra related games may post separately; failures there can leave local follow-up plays
- unposted local plays are posted automatically once the device is online: `BggPlayPostWorker.enqueue` (unique work, network constraint, KEEP) is called at app start, when a play is saved offline, and when a background post fails. The worker and the app share `PlayPostLock` (one mutex, the local -> BGG id map, and a `postedInBackground` signal that makes `AppViewModel` reload history); every poster re-reads the play under the lock and skips it if it is already posted or gone, so a play is never posted twice. `deleteLocalPlay` takes the same lock and refuses a play that was just promoted (it is on BGG now). History's Post / Post all still work for posting right away
- tapping X or back when Log Play has any data (unsaved changes, editable players, or an extracted play) shows a discard confirmation dialog; the check reads `AppViewModel` StateFlow values directly to avoid a one-frame `LaunchedEffect` delay
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
  - `Challenges`
  - `Stats` -- a `Plays | Collection` switch; Plays includes per-player stat profiles and a `HeadToHeadSection` with `BoardFlowPickerField` player selectors
  - `Players`
- the Plays tab also acts as the outbox surface for unposted local plays

### Quick Setup

- entries: the `Quick Guides` tab in `NewPlayScreen` (opens on `My games`, which shares the Log Play search results; a game without a guide reads "No guide yet - draft one from the rulebook" and opens Quick Setup on its empty state; `All guides` lists `SetupGuideRepository.guides`), and the `Setup` button in `GameDetailsDialog` `HeaderSection`, shown when `SetupGuideRepository.availability` has the game's BGG id (base, alias, or a module `bggId`); both Collection and History wire it through `AppShell.openQuickSetup`
- every guide opens at 2 players (clamped to the guide's range); modules with the same `group` are a single choice (radio) and exactly one available member is always on. A group can be a game mode (Arcs: `Single game` / `Campaign: Act I` / `Campaign: Act II or III`, base steps conditioned on the mode); a module that `requires` a mode member is greyed out while another mode is chosen, with a "X: only with Y" hint (`SetupGuideResolver.lockedModules(..., enabled)`), and picking another mode drops the modules that needed the old one (`withDependents`)
- route `AppRoutes.QUICK_SETUP` (`quick_setup/{gameId}?players={players}`); bottom nav hidden; `QuickSetupViewModel` is nav-scoped and keeps player count, modules and checked step ids in its `SavedStateHandle` only - never persisted, never written into the guide
- guide content is data, not code: edit `setup-guides/*.json`, bump `version` in the guide and `index.json`, and run `:app:testDebugUnitTest` (`BundledSetupGuidesTest` validates every guide and every configuration). Paraphrase rulebooks; do not paste their text
- conditions are AND-only; express OR as two steps. Step ids must stay stable so ticks survive configuration changes
- `Start game` calls `AppViewModel.startPlayTimer` for the guide's base game
- the user's own guides: Settings > Preferences > Quick guides > `Import a guide` reads a guide `.json` (validated by `SetupGuideValidator`) and stores it as a `USER` row (`SetupGuideRepository.importUserGuide`). A `USER` guide wins over the bundled and downloaded copies and is never overwritten by catalog updates; it records the standard version it was based on (`basedOnVersion`), and when the standard guide gets newer Quick Setup shows `Updated guide available` with `Keep mine` / `Use updated guide`. Quick Setup's `Share guide` sends the guide on screen as a `.json` file (the catalog format); `Use standard guide` deletes the user's copy
- the in-app guide editor: Quick Setup's `Edit guide` opens route `AppRoutes.GUIDE_EDITOR` (`guide_editor/{gameId}`, bottom nav hidden, title "Edit guide") with `GuideEditorScreen` / `GuideEditorViewModel` (nav-scoped; the draft is the guide's JSON in its `SavedStateHandle`). It edits section names, step text and notes, adds / deletes / reorders steps and adds / deletes sections; the edit rules are pure functions in `data/setupguide/GuideEdits.kt` (unit-tested in `GuideEditsTest`). The grey line under each step (`GuideEdits.describeRules`, or "Quantities and conditions" when it has none) and the "Shows ..." line under each section name open `GuideRulesSheet`: per-player values for every `{name}` quantity in the step text (`setAmountValue`; "Any count" is the default, a count left empty falls back to it, clearing every value removes the quantity), the value or removal of existing special cases (`setCaseValue` / `deleteCase`; new cases are not created in the app), the player counts the step or section shows at (`setShownAtCounts`, stored as nothing / `minPlayers` / `maxPlayers` / `players` like the hand-written guides, at least one count stays on) and Either way / With / Without per module (`setModuleRule`). Sheet edits go straight into the draft, so Cancel still discards them. Save tidies empty steps and sections, validates, and stores the result as the `USER` guide (`SetupGuideRepository.saveEditedGuide`); Cancel or back with unsaved changes asks to discard (the top bar back arrow is routed to the screen through `guideEditorBackRequests` in `AppShell`). `SetupGuideRepository.userGuideChanges` makes an open Quick Setup reload after a save, import or removal
- AI drafts: Quick Setup for a game with no guide at all (not just one that is not downloaded yet) offers `Draft from rulebook` when a Gemini key is set. The user picks the rulebook PDF (the notification permission is asked for first); `GuideDraftService.start` (in `AppContainer`) copies it to `filesDir/guide-drafts/<gameId>.pdf` and queues `GuideDraftWorker` (unique work `guide_draft_<gameId>`, network constraint, expedited, foreground with a quiet "Drafting a guide for X" notification), so the draft keeps running when the user leaves the screen or the app. The worker runs `GuideDraftService.draftFromFile` and posts "X guide is ready" or what went wrong; tapping it opens Quick Setup for the game (`BackgroundNotifications.ACTION_OPEN_QUICK_SETUP` -> `MainActivity.handleIntent` -> `AppViewModel.requestOpenQuickSetup` -> `AppShell`). `QuickSetupViewModel` follows the work through `GuideDraftService.state` (a running draft shows "Drafting the guide ... you can leave this screen"; a failure seen while the screen is open is a snackbar). `draftFromFile` builds a `GuideDraftAiConfig` from the Gemini keys and `getGeminiModelCandidates(preferLite = false)`, with the model that drafted the last guide first, and calls `GeminiGuideDraftGenerator`: PDFs up to 14 MB go inline, larger ones (Gemini's limit is 50 MB) through the File API, uploaded once per key. Busy models are not waited for one by one: two models work on the rulebook at once (`PARALLEL_MODELS`), the first valid guide wins and the other request is cancelled, a failing model is replaced by the next candidate at once, a busy model (429/503) is skipped for 15 minutes (`markModelExhausted` with a short TTL), and a model still working after 4 minutes is given up (`MODEL_TIMEOUT_MS`). Measured: a busy model can take 30-255 s to say so, and a healthy one up to several minutes before its first byte, so a time-to-first-byte cutoff does not work. Full Flash models go before Flash-Lite: on the Arcs rulebook the full models made no factual errors, while Lite runs dropped setup steps and gemini-2.5-flash-lite gave a wrong Court row count that passes every check. Lite models are tried only after every full model, a Lite guide that arrives while a full model is still working is held as a fallback, and once one is held no further Lite model is asked. The prompt (`GuideDraftPrompt.RULES`) asks for no box inventory, every rulebook quantity spelled out, every amount shown in its step text, and setup numbers taken from the setup section (gemini-2.5-flash once used Ticket to Ride: Nordic Countries' mid-game "draw 3, keep 1" for the setup "deal 5, keep 2"); `SetupGuideValidator` also rejects an amount its step text does not use. Full models still make mistakes, so the "check it against the rulebook" label stays The prompt and the answer checks are pure in `GuideDraftPrompt` (`GuideDraftPromptTest`): the answer is parsed, the game id / name / version are forced, guessed BGG ids (aliases, module `bggId`) are dropped, provenance becomes `AI_DRAFT` with `aiModel` and `reviewed = false`, and `SetupGuideValidator` runs; an invalid answer is sent back once to the same model with its problems, then that model is given up. Keys rotate per model on 429. The draft is saved as the `USER` guide (`SetupGuideRepository.saveDraftGuide`); Quick Setup shows "Draft guide - check it against the rulebook." with `Mark as reviewed` (`markReviewed`), and `Delete guide` for a user guide with no standard guide behind it. Saving the draft in the editor also marks it reviewed
- Settings > Preferences > Quick guides also has `Update guides` (forced catalog check and download, reports how many were updated) and `Clear downloaded guides` (deletes `CATALOG` rows; built-in and user guides stay)

### Collection

- collection data is loaded through sync and propagated from the canonical Room snapshot
- tabs are `My Shelf` and `Sleeves`; collection stats live in Journal → Stats behind a `Plays | Collection` switch (`HistoryScreen`, `CollectionStatsTab`). Membership (owned / wishlist / played) is no
  longer separate tabs — it lives in the `My Shelf` filter sheet as two single-select
  dimensions:
  - **Show** (`OwnershipFilter`): `Owned` (default) / `Wishlist` / `Played, not owned` / `Any`
  - **Play status** (`PlayStatusFilter`): `Any` (default) / `Played` / `Unplayed`
  plus the `Players` / `Best for` / `Recommended for` player-count filters
  (`bestForMatches` / `recommendedForMatches` delegate to `playerCountMatches`;
  `isPlayedGame` keys off play history `gameId` plus `numPlays > 0`)
- played-but-not-owned games are cached as `GameItem`s during sync via
  `SyncViewModel.enrichPlayedGames` (using `BggApiClient.fetchThingDetails`), so they survive
  sync (`mergeGameItems` is additive), surface under `Show -> Played, not owned`, are
  searchable in Log Play (`AppViewModel.logPlayPool`), and resolve as game info from a play.
  Sleeve surfaces filter on `isOwned`, so they ignore played-only games.
- game detail dialog is a major cross-link hub into History and Players
- the detail dialog's header buttons `Log play` / `Setup` / `History` are all the same solid amber pill (`DialogPrimaryActionButton`); its bottom row `BGG` / `Rules` / `Drive` is centred `BoardFlowSecondaryButton(compact = true)`s, the same 32dp size as the header buttons (`BoardFlowActionTokens.SecondaryButtonMinHeight` / `SecondaryButtonContentPadding`); `Rules` appears when `RulebookLinks` (bundled `assets/rulebooks.json`, BGG id -> path in the public `boardgame-rulebooks` GitHub repo) has the game's id, and opens the PDF (or the folder when the game has several files) in the browser; entries that are full URLs (temporary RulesPal links for games with no PDF yet) are opened as-is; regenerate the JSON when rulebooks are added to that repo
- sleeve display respects per-game exclusion toggles

### Sync

- Sync (Settings, first tab) is the user-facing operational hub for:
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

Four tabs, each a list of sections (`BoardFlowSectionTitle` over a `BoardFlowFormGroup` of `BoardFlowSettingRow`s):

- **Sync**: accounts (Google, BGG showing the username, Sheet showing its Google name), BoardGameGeek, Sleeves, Google Sheets
- **Preferences**: Stats, Logging plays, Collection, Quick guides, Help
- **Scan**: Gemini (key and backup keys edited in dialogs, model picker, refresh) and Learned from scans
- **Data**: Backup and restore, Storage

Details:

- manages BGG credentials (Sync > Accounts > BGG > Edit)
- manages Google account and sheet connection (Sync > Accounts)
- manages Gemini configuration (key, backup keys, model, model discovery)
- there is no theme setting: the app has one dark amber theme (`ui/theme/`: `Theme.kt` colours and `BoardFlowColors`, `Type.kt`, `Shape.kt`, `Spacing.kt`); use those tokens instead of raw sizes, radii and colours
- manages sleeve manufacturer priority (`SleeveManufacturer`; persisted in `SecurePreferences`, exposed via `AppViewModel.sleevePreferredManufacturer`; used in `GameDetailDialog` via `SleeveEntry.preferredFor()`)
- manages import/export (backup includes recognition templates since format v3)
- can clear cached collection
- Scan tab: view / edit / delete individual recognition templates (`RecognitionTemplatesDialog`); bulk-clear all templates with confirmation
- Preferences tab: Chronicles toggle (on by default); turning it off cancels all in-flight generation jobs and hides chronicle cards throughout the app
- Preferences: Mood templates (`CustomMoodsDialog`) - view and delete custom moods saved during session memory entry (delete asks first: it also removes the mood from every play)

### AI Game Recognition

#### Engine (`data/GameRecognitionEngine.kt`)

- stateless; `rankCandidates(extractedPlay, collection, hints)` scores every entry and returns up to 5 ranked `GameCandidate` objects
- four scoring signals:
  1. **Title similarity** — Levenshtein distance + containment, up to 1.0; primary signal when a title is detected
  2. **Category-name-in-game-name** — legacy low-weight boost (up to 0.2) when detected category strings appear in the game name
  3. **Template category overlap** — compares detected scoring categories against `hint.normalizedCategories`; requires `>= MIN_TEMPLATE_CATEGORY_OVERLAP` (3) for a strong signal; weight is 0.75 / 0.50 / 0.30 depending on title strength so category-only sheets can still produce a high-confidence match
  4. **Saved title bonus** — +0.15 if the detected title matches a title stored in the hint from a prior confirmation
- `GameCandidate` carries `score`, `matchReason`, `primarySignal` (`"title"` or `"category-template"`), and `templateOverlap`

#### Autoswitch Gates (`AppViewModel.extractScores`)

Two gates decide whether to auto-switch the active game without user input:

- **TITLE_GATE**: `score >= 0.90`, `primarySignal != "category-template"`, Gemini confidence >= 0.95, score margin over 2nd candidate >= 0.15
- **TEMPLATE_CATEGORY_GATE**: `score >= 0.75`, `primarySignal == "category-template"`, `templateOverlap >= 3`, Gemini confidence >= 0.95, margin >= 0.15

If a gate fires, `AppViewModel` calls `applyDetectedGame()` silently and emits `ScanRecognitionResult.AutoSwitched`. If neither gate fires but candidates exist, they are exposed via `_gameCandidates` for the user to pick from.

#### Recognition Result State (`ScanRecognitionResult`)

Sealed class emitted to `_scanRecognitionResult` after each scan:

- `AutoSwitched(gameName)` — a gate fired; game was silently switched; `ScanResultBanner` in `LogPlayScreen` shows a success message
- `NoCollectionMatch(detectedTitle)` — Gemini detected a title but nothing in the collection matched; `ScanResultBanner` shows an info message with a "Choose another game" action
- `LowConfidence` — Gemini could not detect the game or confidence was below threshold; no banner shown

#### Candidate Suggestion Flow

When neither gate fires but `_gameCandidates` is non-empty, `LogPlayScreen` renders `GameSuggestionBanner` showing the top candidate with confidence and evidence text. From there:

- `AppViewModel.acceptGameSuggestion(game)` — saves a `GameRecognitionHint` for the confirmed game, applies it via `applyDetectedGame()`, and clears `_gameCandidates`
- `AppViewModel.dismissGameSuggestion()` — clears `_gameCandidates` without changing the active game
- `AppViewModel.dismissScanRecognitionResult()` — clears `_scanRecognitionResult` and exits correction mode if active
- `scanStartedWithGame: StateFlow<Boolean>` — true when the scan was started with a game already selected (id != 0); passed to the banner composables as `hasPreselectedGame` to control whether the "Choose another game" action is offered
- `clearExtractedPlay()` — public helper that nulls `_extractedPlay`; called by screens that need to discard scan data without clearing the full log play flow

`LogPlayScreen` also shows a passive `detectedGameHint` label ("AI detected: X") inside `CompactGameHeader` when Gemini identified a title different from the current game and no actionable banner is already showing.

#### Gemini Prompt and Response (`data/GeminiRepository.kt`)

The extraction prompt explicitly requests game identification fields alongside player scores:

- `detectedGameTitle` — best-guess game name from visible text, logos, or scoring structure; `null` if indeterminate
- `detectedGameConfidence` — float 0.0–1.0 reflecting certainty about the title
- `detectedScoringCategories` — list of scoring column/row header labels visible on the sheet
- `gameDetectionEvidence` — one sentence describing the visual cue used for identification

`parseGeminiResponse()` maps these to `ExtractedPlay` fields. Date handling: `null` or the literal string `"null"` from the model are both treated as absent (the app uses today's date); the model is instructed not to invent a date. On success the returned `ExtractedPlay` also carries `modelUsed` (the model that returned HTTP 200) and `isMalformed = false`. If JSON parsing throws, partial player extraction is attempted via regex, `isMalformed` is set to `true`, and the raw text is prefixed with a warning emoji.

Debug logging (`TAG = "Gemini"`) covers: start time, per-attempt model and HTTP status, total elapsed time, model fallback switches, and parse results.

#### Malformed Response Retry

When `extractScores` receives an `ExtractedPlay` with `isMalformed = true`, `AppViewModel` immediately launches a background coroutine (stored as `_retryJob`) that silently re-calls Gemini with the same image file and model parameters. The loading indicator is **not** shown during the retry — the user lands on `LogPlayScreen` with the partial data and can start editing. If the retry returns `isMalformed = false`, `_scanRetryResult` is set and `LogPlayScreen` shows a `ScanRetryBanner` ("Scan retry got a cleaner result. Apply it?") with "Apply update" / "Dismiss" buttons. Accepting calls `acceptRetryResult()` which replaces `_extractedPlay` and re-initialises editable players. The retry job is cancelled (and `_scanRetryResult` cleared) by `cancelBackgroundRetry()`, which is invoked from `clearExtractedPlay()` and both success paths in `postPlay()`, ensuring the background work stops when the user saves or discards the play.

#### Scan Image Quality (`data/ScanImageQualityAnalyzer.kt`)

Before Gemini extraction, captured or gallery-picked score-sheet images are checked locally for obvious readability issues:

- too dark
- too blurry
- too low resolution
- likely too far away / too much empty border

Good images continue to Gemini immediately. Poor images show a warning in `ScanScreen` with the first available human-readable reason, but the user can still choose "Use anyway" and continue the existing extraction flow. "Retake" returns to the camera/gallery choice. Keep this preflight local-only and separate from game recognition, player recognition, and Gemini prompt/response logic.

#### Hint Lifecycle (Game)

- A `GameRecognitionHint` stores the game's `objectId`, normalized title strings, normalized scoring category strings, `confirmedAt` timestamp, and `timesConfirmed` count
- `SecurePreferences.saveGameRecognitionHint()` merges with any existing hint for the same game (accumulates titles and categories, increments counter)
- `replaceGameRecognitionHint()` does a full replace without merging (used by the Settings edit dialog)
- `deleteGameRecognitionHint(gameObjectId)` removes one entry; `clearGameRecognitionHints()` removes all
- Hints are included in backup export/import (format v3 `recognitionHints` array; import bulk-replaces)
- Settings > Scan shows the hint count; the templates dialog has an amber pen (edit categories) and a red trash per template, plus bulk clearing with confirmation

### AI Player Recognition (`data/PlayerRecognitionEngine.kt`)

`PlayerRecognitionEngine` (stateless object) resolves a raw scanned player name to a roster `Player` using a three-tier lookup:

1. **Saved hints** (`PlayerRecognitionHint`) — keyed by `scannedNameNormalized`; confidence = `0.70 + timesConfirmed * 0.05` capped at `0.95`; if two hints for the same scanned name point to different players with close `timesConfirmed` (second ≥ half of first), the result is flagged ambiguous (confidence 0.55)
2. **Exact alias / display name** — case-insensitive; confidence 1.0
3. **Fuzzy (Levenshtein)** — threshold = `max(2, name.length / 3)`; confidence = `1 - dist / (name.length + 1)`

`AppViewModel.initEditablePlayers()` applies resolution for each scan-extracted player: if match confidence ≥ 0.70 and source ≠ "fuzzy", the scanned name is replaced with `player.displayName`. Fuzzy matches are not auto-applied. Raw scanned names are stored in `_originalScannedNames`.

`AppViewModel.savePlayerHintsFromCurrentPlay()` is called on both success paths in `postPlay`. It compares each final editable player name against its original scanned name; for any pair where a roster player is found for the final name and `normalizeForRecognition(originalScanned) ≠ normalizeForRecognition(rosterDisplayName)`, a `PlayerRecognitionHint` is saved (or incremented via `savePlayerRecognitionHint`).

`SecurePreferences.savePlayerRecognitionHint()` upserts by `(scannedNameNormalized, confirmedRosterPlayerId)` pair, incrementing `timesConfirmed` on collision.

Settings > Scan shows the count of saved player hints and a "Clear player recognition hints" action.

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
- local unposted plays should remain visible in History until they are posted
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
- `BggRepository.searchGames` accepts an `exact: Boolean` parameter that maps to `exact=1` / `exact=0` in the BGG XML API URL; `BggGameSearch` sends both and merges them
- search covers `type=boardgame,boardgameexpansion` so expansions appear alongside base games in external search results
- the BGG XML API is read-only for collections; `BggRepository.setCollectionStatus` posts to the undocumented `geekcollection.php` endpoint the website's own status checkboxes use (same endpoint family as `rateGame`), so it can break without notice
- collection writes need the entry's `collid` (`BggRepository.getCollectionId`) or BGG creates a duplicate entry instead of updating the existing one
- `geekcollection.php` reports failures as HTTP 200 with an HTML `messagebox error` div (an expired session gives "You must login to use the collection utilities."), so a 2xx alone does not mean the write landed - `extractCollectionError` checks for that box
- xmlapi2 collection reads need a logged-in session: real usernames return 401 unauthenticated, while an unknown username returns HTTP 200 with an `<errors>` document, so `getCollectionId` treats both as failures rather than "no collid" (a null collid would create a duplicate entry)
- the write path is confirmed against a live account: `action=savedata` with an empty `collid` created a new entry and the flag showed up in the XML API
- a successful save answers with an HTML fragment of the new status labels (e.g. `<div class='wanttoplay'>Want To Play</div>`), NOT JSON and with no `collid`, so `setCollectionStatus` returns null for a newly created entry - re-resolve it with `getCollectionId`
- clearing every status flag does NOT delete a collection entry (verified): it stays with all flags 0. Removal is a separate `action=delete` post with the `collid` (`BggRepository.deleteCollectionEntry`), which answers HTTP 200 with an empty body
- the eight status flags map to BGG's own checkbox labels as: `own`=Own, `prevowned`=Prev. Owned, `fortrade`=For Trade, `wanttoplay`=Want to Play, `want`=Want in Trade, `wanttobuy`=Want to Buy, `preordered`=Pre-ordered, `wishlist`=Wishlist - note `want` is "Want in Trade", not a general want (see the table on `BggCollectionStatus`)
- `scripts/bgg_collection_probe.py` exercises all of the above against a real account (dry run by default)
- collection refresh reads every entry's status flags and `collid` in one `brief=1` request (`BggApiClient.fetchCollectionStatuses`, applied by `SyncViewModel.applyCollectionStatuses` after played games are added) and stores them in `GameItem.bggValues` (`withSyncedCollectionEntry` / `syncedCollectionEntry` / `hasSyncedCollectionStatus` in `Models.kt`); the sync does not touch `ownership` from this data, so sheet-only games keep their flags
- adding a game that is not on the shelf: a My Shelf search of 2+ characters ends with `Search BoardGameGeek for "..."` (`SearchBggRow`), which opens `BggSearchSheet` ("Add to your collection") with `SyncViewModel.shelfBggSearch` results. Tapping a result (`openBggSearchResult`) reuses the snapshot game if there is one, otherwise builds a `GameItem` from `BggApiClient.fetchThingDetails` (`gameItemFromBgg`, shared with `enrichPlayedGames`) and stages it in `stagedShelfGames`; the detail dialog then opens straight on the status editor (`GameDetailsDialog(startWithCollectionEditor = true)`, Save reads `Add to collection`). The staged game joins the Room snapshot when `applyCollectionStatusUpdate` gets a user edit with an entry for a game the snapshot does not have; nothing is added if the user cancels
- the collection editor lives on the game detail dialog: a "Collection" row opens `CollectionStatusEditorDialog` (built on `AnimatedDialog` + BoardFlow chips/buttons, not `AlertDialog`), which reads the synced status (`AppViewModel.loadCollectionStatus`) and only calls BGG when the game has no synced status yet; it edits a local draft so partial toggles are never posted, and writes on Save
- saves, removals and fallback reads are emitted as `CollectionStatusUpdate` on `AppViewModel.collectionStatusUpdates`; `AppShell` forwards them to `SyncViewModel.applyCollectionStatusUpdate`, which patches the Room snapshot (a user edit also updates own / wishlist, so the shelf follows immediately)
- `CollectionStatusUiState` carries the `gameId` it belongs to and both screens filter on it, so opening a second game never shows the first game's status while the new one loads
- sleeve tracking is backed up to BGG as a `[sleeves:sleeved|to-sleeve|possible|no]` token in the collection entry's Private Info comment (`BggSleeveMarker`); the rest of the comment is the user's text and is preserved. Only games that already have a collection entry are written - the backup never creates entries
- the Private Info write is verified against a live account: `geekcollection.php` `action=savedata` `fieldname=ownership` replaces the whole block (fields left out are blanked) and leaves the status flags alone, so `BggRepository.saveSleeveMarker` reads the block first (`getPrivateInfo`) and posts every field back
- `getPrivateInfo` reads the site's JSON endpoint (`/api/collections?objectid=..&objecttype=thing&userid=..`, userid from `/api/users/current`), not xmlapi2: the XML collection is cached and serves stale private info for a while after a write, and resending a stale read undoes the previous edit (seen live). The JSON keys equal the form field names; a game not in the collection returns `{"items":[]}`
- xmlapi2 only includes `<privateinfo>` with `showprivate=1` and WITHOUT `brief=1` (brief drops it), and only for entries that have any private info
- precedence for sleeve status is local edit (`game_sleeve_tracking`) > spreadsheet `sleeved` value > BGG marker; `SyncViewModel.applyCollectionStatuses` reads markers with the status sync and only fills games that have neither, persisting them to `game_sleeve_tracking`
- a sleeve status change mirrors to BGG right away (`maybeMirrorSleeveTrackingToBgg`); Settings > Sync's "Back up sleeve status to BGG" (`backupSleeveStatusToBgg`) pushes every game whose marker differs, one read + one post per game, throttled

## History / Roster Notes

- saved roster players are distinct from arbitrary logged names
- a player can have a default avatar colour (`Player.color`, `#RRGGBB`, blank = automatic from the name), picked in `EditPlayerDialog` and saved by `AppViewModel.updatePlayerColor`. `AppShell` provides `LocalPlayerColors` (name and aliases -> colour) so every `PlayerAvatar` uses it; a colour recorded on a play (red, blue, ...) still wins inside that play. Stored in the `players` table (`color`, migration 12 -> 13) and written to backups only when set
- history rows should show all logged players, even if names are similar
- general stats may treat unsaved names differently from roster views
- roster-oriented views should stay roster-based
- editable player rows should use stable UI identity and survive configuration changes where practical
- fuzzy player matching uses Levenshtein distance (threshold ~1/3 of input length); matches shown as suggestions, not auto-applied
- `PlayerResultEditorCard` shows a `MatchedPlayerChip` ("Matched [name]") when the typed name resolves to an exact roster player; the chip is styled as a `SuggestionChip` at the same size as fuzzy suggestions
- the name field is only auto-renamed to `player.displayName` when the field is **not** focused; if the user is actively typing, the rename is deferred until they leave the field (`onFocusChanged` guard on the `LaunchedEffect(exactMatch?.id, nameFocused)`)
- player cards collapse only when the user explicitly adds another player (`collapseCompletePlayers()` called from `addEditablePlayer` and `addPlayerFromRoster`); there is no reactive auto-collapse on field completion

## Sleeve Notes

- `SleeveManufacturer` enum lives in `SleeveDatabase.kt` (7 values: AUTO, TLAMA_DIAMOND, PALADIN, ULTRA_PRO, SAPPHIRE, SLEEVE_KINGS, ARCANE_TINMEN)
- `SleeveEntry.preferredFor(manufacturer)` returns the (brand, productName) pair for the chosen brand, falling back to the best available if that brand does not carry this size
- the user-selected manufacturer is read from `SecurePreferences.sleevePreferredManufacturer`, exposed as `AppViewModel.sleevePreferredManufacturer: StateFlow<SleeveManufacturer>`
- `GameDetailDialog` reads it at composition time and passes it through `SleevesBlock` -> `SleevesSection`
- per-game exclusions are stored in `SecurePreferences` as a `Set<String>` of game objectIds; managed via `SyncViewModel.toggleSleeveGameExclusion` / `excludeAllSleeveGames` / `includeAllSleeveGames`

## UI Conventions

- build screens from the shared kit in `ui/common/BoardFlowKit.kt` (`BoardFlowCard`, `BoardFlowSectionTitle`, `GameListRow`, `GameCover`, `BoardFlowFormGroup` / `BoardFlowFormRow` / `BoardFlowInlineField`, `BoardFlowSettingRow` / `BoardFlowSettingValue`, `BoardFlowTextField`, `BoardFlowInfoPill`, `BoardFlowEmptyState`, `BoardFlowErrorBanner`) and the buttons, sheets and dialogs in `BoardFlowUi.kt`; do not style private surfaces, fields or rows per screen
- forms are grouped rows on a tonal surface (icon, label, value), not one outlined box per field; surfaces separate by tone, not by borders
- buttons are pills that hug their label (never `fillMaxWidth`), all in `BoardFlowUi.kt`:
  - primary (`BoardFlowButton`): solid amber, 40dp. One per view: the action that moves the task forward (Save, Log play, Start game, Play again)
  - secondary (`BoardFlowSecondaryButton`): amber outline and label on a transparent fill, same 40dp height. Alternatives next to the primary (Scan, Add player, Retake, the game detail links)
  - cancel: white text (`BoardFlowInlineAction(neutral = true)`), left of the save button; `large = true` beside a 40dp button
  - destructive (`BoardFlowDestructiveButton`): outlined red, 40dp (Remove, Clear rating, Clear collection cache). The action in `BoardFlowConfirmationDialog` is a solid red pill with a white Cancel. Red means data loss only
  - a main Edit action is an amber pen icon next to the red delete icon on the left of the action row, with the primary pill alone on the right; a minor edit is amber "Edit" text
  - two controls that sit next to each other have the same height and label size
- amber (`colorScheme.primary`) is for emphasis, not for everything tappable: titles (game names), key numbers, the buttons above, text actions and the selected state. Chevrons, expanders, share and overflow icons, form-row icons and values are grey or white. The exception is `BoardFlowSettingRow` (Settings, Sync), whose leading icon is amber, or red for a destructive row. Do not enlarge type or controls beyond the Material defaults: the original density is part of the look
- on game art or the camera, pills are `Color.Black` at 50% with white labels; the one amber control on the camera is the shutter
- the winner row is a translucent amber fill with no outline; session and chronicle cards are translucent grey (`Color.White` at 10%) with no outline
- every tappable element is at least 48dp (`Dimens.MinTouchTarget`); dates shown to the user read `Oct 1, 2026`
- every create, update and delete gets a snackbar via `LocalBoardFlowMessenger.current.show(message, actionLabel, onAction)` (hosted by `AppShell`); reversible local deletes (local plays, players, challenges, pause, archive) offer Undo instead of a confirmation dialog. Confirmations stay for BGG writes and irreversible actions. Explicit refreshes run without a "refresh again?" prompt
- the top bar shows the destination (Log Play, Journal, Collection, Settings); the tab row shows the sub-location
- the top bar shows the screen title, with a back arrow on pushed screens (scan, log play, quick setup, QR import); the bottom bar is a Material `NavigationBar`
- every screen uses the kit. Two treatments, by purpose:
  - forms and editors (Log Play, edit play, create challenge, player edit, sleeve inventory, account dialogs): a headline, grouped rows, and white Cancel text plus the save pill at the bottom right
  - display screens (Challenges, Stats, game detail, Sleeves, Quick Setup): keep the content the screen always showed, on plain tonal cards with no outline and no colour tint; on one card only the key number, a status badge and real actions carry colour, secondary toggles ("Show 2 counted games") are white
- status colours come from `BoardFlowColors` (`Success` done, `Warning` paused, `colorScheme.error` missed); do not composite amber over a surface, it turns brown
- an editable value in a read-only grid keeps its white value and gets a grey chevron (`DetailCell` in `GameDetailDialog`)
- text input outside a form group is `BoardFlowTextField` (tonal fill, label inside); inside a form group it is `BoardFlowInlineField`
- labels are sentence case ("Refresh collection", not "Refresh Collection")
- tab rows are `ScreenTabRow`: the label size is computed once so the widest label in `ScreenTabs.AllLabels` fits one line in a quarter of the screen (the most tabs any screen has), and every tab row uses that size; add new tab labels to `ScreenTabs.AllLabels`. Every tabbed screen also switches tabs on a horizontal swipe (`swipeToNavigateTabs`)
- Settings and Sync are lists of sections: a `BoardFlowSectionTitle` (with an optional one-line `supporting`) over one `BoardFlowFormGroup` of `BoardFlowSettingRow`s (amber icon, title, grey detail, then a `BoardFlowSettingValue`, a switch or a chevron). Every action is a row, not a button; clear / delete rows are `destructive = true` (red). Text entry (Gemini key, backup keys) opens a dialog from its row. Results are snackbars
- Journal play rows list every player on their own line (`HistoryListPlayerRow`)
- Log Play hides its tabs, search field and "Playing" row while the list scrolls down and brings them back on the way up; it follows the drag direction (`NestedScrollConnection`), not the list position, so a short list does not flicker

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
./gradlew.bat :app:testDebugUnitTest
```

Also use this when startup behavior, resources, or packaging may be affected:

```sh
./gradlew.bat :app:assembleDebug
```

If you only changed docs or a very small behavior fix, compile is usually enough.

Release builds are shrunk with R8. Before a release, smoke-test the release variant: build it signed with the debug key so it installs over the debug app without losing data (`./gradlew.bat :app:assembleRelease -Pandroid.injected.signing.store.file=<debug.keystore> -Pandroid.injected.signing.store.password=android -Pandroid.injected.signing.key.alias=androiddebugkey -Pandroid.injected.signing.key.password=android`), then build the real one with `:app:assembleRelease` / `:app:bundleRelease`. Releases: bump `versionCode` / `versionName`, tag `vX.Y.Z`, attach the APK to a GitHub release; Google Play steps are in `play-store/PLAY_RELEASE.md`.

On this Windows machine Java and git sit behind HTTPS inspection: add `-Djavax.net.ssl.trustStoreType=Windows-ROOT` to Gradle when it needs to download dependencies, and use `git -c http.sslBackend=schannel` for fetch / pull / push.

## Important Runtime Note

Google sign-in and Google Sheets / Drive access depend on external Firebase / Google Cloud OAuth configuration. A successful compile does not guarantee runtime sign-in success if SHA fingerprints or OAuth client setup are wrong.

## Dependency Notes

Current notable choices:

- Java 17 / Kotlin JVM target 17, Kotlin 2.0
- Android Gradle Plugin 8.9.3, Gradle 8.14, compile and target SDK 36 (Android 16), min SDK 26
- Compose + Material 3
- Navigation Compose
- Credential Manager + Google Identity
- OkHttp
- CameraX
- Coil
- Room
- WorkManager, Jetpack Glance (widgets), ZXing (QR)

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
