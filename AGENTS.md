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
- AI score extraction from images with Gemini
- saved player roster with aliases and optional BGG usernames
- collection browsing across owned / wishlist / sleeves
- game detail drill-ins with history and player links
- history tabs for plays, stats, and players
- Google Sheets / Drive sync
- spreadsheet creation / connection
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
  - search, log play, history, roster, local play persistence, outbox posting, import/export
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
  - bottom nav
  - screen routing
  - cross-screen deep-link style callbacks between Collection, History, Players, and Log Play
- `auth/GoogleAuthManager.kt`
  - Google account selection / sign-in orchestration
- `core/di/AppContainer.kt`
  - lightweight manual DI container
- `core/navigation/AppRoutes.kt`
  - central route definitions
- `AppViewModel.kt`
  - game search and recent games
  - session continuation / play-again setup
  - AI extraction handoff into review
  - editable log state integration
  - roster and player alias management
  - local play history and cached BGG history merge behavior
  - play post/edit/delete flows
  - local outbox posting for unposted plays
  - import/export and backup restore
- `SyncViewModel.kt`
  - Google auth state
  - spreadsheet connection state
  - collection refresh
  - sleeve refresh
  - full Sheets sync
  - sync log / progress state
- `data/`
  - API clients, parsers, storage, backup serialization, Room adapters, persistence helpers
- `ui/`
  - screens and shared Compose UI helpers

## Source Of Truth

### Live Runtime Data

The live runtime source of truth is Room via `CanonicalCollectionStore`.

It stores:

- canonical merged collection snapshot
- local logged plays
- cached BGG play history

### Preferences / Settings

`SecurePreferences` stores:

- credentials and tokens
- app theme and settings
- player roster
- recent games
- sync-related preferences
- backup compatibility helpers

Do not move live collection/history state back into large JSON blobs in preferences.

### Backup Format

`BackupSerializer` owns import/export JSON.

Backups can contain:

- collection snapshot
- local logged plays
- cached BGG plays
- player roster
- sleeves exclusions
- settings
- optionally sensitive data

## Key Product Flows To Understand

### Search -> Log Play

- `NewPlayScreen` loads recent games and local collection-backed search first
- if no local collection result matches, search may fall back to BGG XML search
- selected games move into `LogPlayScreen`
- session context may prefill players/location
- AI extraction may prefill players/scores

### Log Play -> Local / BGG

- if online with credentials, the app can post to BGG
- if offline or posting is unavailable, the play can still be saved locally
- extra related games may post separately; failures there can leave local follow-up plays
- local unposted plays are intentionally user-controlled from History rather than silently auto-posted on startup

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

### Sync

- Sync screen is the user-facing operational hub for:
  - BGG readiness
  - Google readiness
  - sheet connection
  - refresh collection
  - refresh sleeve sizes
  - sync to Sheets
  - create/connect spreadsheet
  - review sync log

### Settings

- manages BGG credentials
- manages Google sheet connection access points
- manages Gemini configuration
- manages theme
- manages import/export
- can clear cached collection

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
- play posting/edit/delete is authenticated
- retry and error handling exist in the repository layer
- BGG XML search outside the local collection requires `BGG_XML_API_TOKEN`
- if BGG XML token-based search is unavailable, fail quietly to empty results instead of noisy user-facing errors where possible

## History / Roster Notes

- saved roster players are distinct from arbitrary logged names
- history rows should show all logged players, even if names are similar
- general stats may treat unsaved names differently from roster views
- roster-oriented views should stay roster-based
- editable player rows should use stable UI identity and survive configuration changes where practical

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
