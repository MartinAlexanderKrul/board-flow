# BoardFlow

BoardFlow is an Android app for tracking board game plays with BoardGameGeek, Google Sheets / Drive, local offline history, roster-aware player stats, sleeve data, and AI-assisted score extraction.

It is built with Jetpack Compose and uses Room as the live local source of truth for collection data, local play history, and cached BGG play history.

## What The App Does

BoardFlow combines several related workflows in one app:

- search your collection or BGG for a game to log
- continue a recent session or use "play again" flows
- log a play directly to BGG when online
- save a play locally when offline
- post saved local plays later from History via an outbox-style "Unposted plays" section
- scan a score sheet photo and extract players / scores with Gemini
- manage a saved player roster with aliases and optional BGG usernames
- review play history, aggregate stats, and saved-player activity
- edit or delete logged plays
- browse your collection, wishlist, and sleeve coverage
- sync your collection to a Google Sheet
- create or connect a Google spreadsheet
- create Drive folders / QR-related assets through the Google sync pipeline
- export and import app data backups

## Main User Flows

### 1. Log Play

Screens involved:

- `ui/search/NewPlayScreen.kt`
- `ui/scan/ScanScreen.kt`
- `ui/review/LogPlayScreen.kt`

Flow:

1. Pick a game from recent games, local collection search, or BGG search.
2. Optionally continue the last session for the same game or use a "play again" flow from History / Collection.
3. Optionally scan a scoresheet image and let Gemini prefill players and scores.
4. Review and edit the play in the compact card-based Log Play sheet.
5. Post to BGG if online and authenticated, or save locally for later if offline / posting fails.

Notable behavior:

- game search prefers the local loaded collection, then falls back to BGG XML search
- session context can prefill players and location
- log form tracks unsaved changes
- player rows are keyed UI items with shared editing UI between log and edit flows
- matched roster players are explicit; non-exact fuzzy matches require user confirmation

### 2. Edit Existing Play

Screen involved:

- `ui/history/HistoryScreen.kt` (`EditPlayDialog`)

Behavior:

- opens from History play details
- uses the same compact player editor visual language as Log Play
- preserves fields on configuration changes with saveable state
- stays open when save fails so the user can correct and retry
- updates the local Room record, and updates BGG too when the play is already posted there

### 3. History

Screen involved:

- `ui/history/HistoryScreen.kt`

Tabs:

- `Plays`
- `Stats`
- `Players`

Current behavior:

- local plays and cached BGG plays are merged for display
- local-only plays can be deleted locally
- BGG-backed plays can be deleted from BGG and pruned from cache
- an `Unposted plays` card in Plays acts as the manual outbox for local plays not yet posted to BGG
- stats support date filters and richer insight cards
- players tab is roster-only and sorts by most recent saved-player activity
- history rows always show all logged players and do not collapse same/similar names into one row
- unsaved logged names can still be grouped as `Unknown` in general stats where appropriate

### 4. Collection

Screen involved:

- `ui/collection/CollectionScreen.kt`
- `ui/collection/GameDetailDialog.kt`

Tabs / areas:

- `Owned`
- `Wishlist`
- `Sleeves`

Behavior:

- collection data comes from the canonical Room snapshot managed by sync
- game detail dialog links collection data with history and player insights
- collection-to-history and collection-to-player deep links are supported
- sleeve data is merged from BGG, scraping, and local exclusions
- the `Owned` tab now strictly means owned games

### 5. Sync

Screen involved:

- `ui/sync/SyncScreen.kt`
- `ui/sync/SpreadsheetModal.kt`

Behavior:

- manages readiness for BGG, Google account, and spreadsheet connection
- supports refreshing BGG collection
- supports refreshing sleeve sizes
- supports full Google Sheet sync
- supports creating a spreadsheet from BGG
- supports connecting an existing spreadsheet
- shows a sync log with summary + detailed dialog
- can clear local sync log state

### 6. Settings

Screen involved:

- `ui/settings/SettingsScreen.kt`

Sections:

- `Accounts`
- `Appearance`
- `AI`
- `Data`

Behavior:

- manage BGG username/password
- manage Google account and spreadsheet connection
- set Gemini API key and model endpoint
- switch app theme
- export backup JSON, optionally including sensitive data
- import backup JSON
- clear cached collection data

## Data Model And Storage

### Live Local Source Of Truth

The live runtime store is Room via:

- `data/CanonicalCollectionStore.kt`

It stores:

- canonical merged collection snapshot
- local logged plays
- cached BGG play history

### Preferences / Secrets

`data/SecurePreferences.kt` stores:

- BGG credentials
- Gemini configuration
- theme and app settings
- recent games
- player roster
- sync-related preferences
- backup compatibility helpers

### Import / Export

`data/BackupSerializer.kt` handles backup JSON import/export.

Backups can include:

- collection snapshot
- local logged plays
- cached BGG plays
- player roster
- sleeves exclusions
- settings
- optionally sensitive data like credentials / keys

## Architecture

Primary entry points:

- `MainActivity.kt`
- `ui/app/AppShell.kt`

Core modules:

- `auth/GoogleAuthManager.kt`
- `core/di/AppContainer.kt`
- `core/navigation/AppRoutes.kt`
- `AppViewModel.kt`
- `SyncViewModel.kt`

High-level ownership:

- `MainActivity`
  - Android lifecycle and activity-result plumbing
- `AppShell`
  - navigation graph, scaffold, header, and bottom tabs
- `AppViewModel`
  - game search
  - log play state
  - players / roster
  - local play history
  - cached BGG play history interaction
  - local posting / edit / delete flows
  - import/export
  - session context
- `SyncViewModel`
  - Google account state
  - spreadsheet connection
  - collection refresh and canonical collection loading
  - sleeve refresh
  - Google sync log and progress
- `data/`
  - BGG API, Google APIs, Room persistence, backup serialization, parsing helpers

## Package Layout

Main source root:

```text
app/src/main/kotlin/cz/nicolsburg/boardflow/
  AppViewModel.kt
  MainActivity.kt
  SyncViewModel.kt
  auth/
    GoogleAuthManager.kt
  core/
    di/
      AppContainer.kt
    navigation/
      AppRoutes.kt
  data/
    BackupSerializer.kt
    BggPlaySync.kt
    BggRepository.kt
    CanonicalCollectionStore.kt
    GeminiRepository.kt
    GoogleApiClient.kt
    SecurePreferences.kt
  model/
    ...
  ui/
    app/
      AppShell.kt
    collection/
      CollectionScreen.kt
      GameDetailDialog.kt
    common/
      ...
    history/
      HistoryScreen.kt
    players/
      PlayersScreen.kt
    review/
      LogPlayScreen.kt
    scan/
      ScanScreen.kt
    search/
      NewPlayScreen.kt
    settings/
      SettingsScreen.kt
    sync/
      SpreadsheetModal.kt
      SyncScreen.kt
    theme/
      ...
```

## External Integrations

### BoardGameGeek

Used for:

- collection refresh
- BGG XML game search outside the loaded collection
- play history fetch / refresh
- play logging and editing
- sleeve-related metadata inputs

Notes:

- authenticated and unauthenticated BGG collection flows both exist
- play posting/editing uses authenticated flows
- BGG XML search outside the local collection requires `BGG_XML_API_TOKEN`
- when token-backed search is unavailable, the app should fail quietly to an empty result state

### Google Identity / Sheets / Drive

Used for:

- account selection
- Drive / Sheets authorization
- spreadsheet creation / connection
- pushing collection data to Sheets
- Drive-related sync helpers and asset generation

### Gemini

Used for:

- AI-assisted score extraction from images

The app supports:

- user-provided Gemini API key
- configurable model endpoint
- model discovery / fallback behavior

## UX / Product Notes

Current app behavior intentionally includes:

- manual user-controlled posting of saved local plays from History instead of silent auto-post on startup
- roster-aware but explicit player matching
- preserved local history even when offline
- shared compact player editing UI between log and edit flows
- history navigation from Collection and player-centric screens
- stats and players views that are based on saved roster entities where appropriate

## Build

From repo root:

```sh
./gradlew.bat :app:compileDebugKotlin
./gradlew.bat :app:assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/board-flow-debug.apk
```

Install on a connected device/emulator:

```sh
./gradlew.bat :app:installDebug
```

## Configuration

### Required / common app settings

Set in Settings as needed:

- BGG username
- BGG password
- Gemini API key
- Gemini model endpoint

Google sign-in / Sheets / Drive also require valid Firebase / Google Cloud OAuth setup:

- `google-services.json`
- Android + Web OAuth clients
- correct SHA fingerprints for debug/release

Compile success does not guarantee runtime Google auth success if that external setup is wrong.

## Verification

Recommended local verification after meaningful changes:

```sh
./gradlew.bat :app:compileDebugKotlin
```

Use this too when startup/resources/packaging changed:

```sh
./gradlew.bat :app:assembleDebug
```

## Text Encoding

- keep source and docs in UTF-8
- be careful with PowerShell bulk rewrites
- prefer plain ASCII punctuation where practical
- if you see mojibake like `Ã‚Â·` or `Ã¢â‚¬Â¦`, fix it before committing
