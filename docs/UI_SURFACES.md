# UI surfaces

Every screen, dialog and sheet in BoardFlow, and the shared components they are built from. Use it to find where something lives before changing it. The visual rules (colours, buttons, spacing) are in the **UI Conventions** section of [`AGENTS.md`](../AGENTS.md).

All paths are relative to `app/src/main/kotlin/cz/nicolsburg/boardflow/`.

## Design system

### Theme (`ui/theme/`)

| File | Contents |
| --- | --- |
| `Theme.kt` | The single dark amber colour scheme, `BoardFlowColors` (success, warning, gold, celebration), `InsightRarityColors` |
| `PlayerColors.kt` | Named play colours, automatic avatar colours, dark ink for light fills |
| `Type.kt` | Material 3 default type scale |
| `Shape.kt` | `BoardFlowShape`: Tiny, Small, Cover, Control, Card, Sheet, SheetTop, Pill |
| `Spacing.kt` | `Spacing` steps and `Dimens` (touch target, icon sizes, field height) |

### Layout and content (`ui/common/BoardFlowKit.kt`)

| Component | Use |
| --- | --- |
| `BoardFlowCard` | Tonal card without an outline |
| `BoardFlowSectionTitle` | Section heading with an optional one-line explanation and trailing action |
| `BoardFlowFormGroup`, `BoardFlowFormRow`, `BoardFlowFormDivider` | Grouped form rows on a tonal surface (icon, label, value) |
| `BoardFlowInlineField` | Borderless text input inside a form row |
| `BoardFlowTextField` | Filled text field for inputs outside a form group |
| `BoardFlowSettingRow`, `BoardFlowSettingValue` | Settings and action rows: icon, title, detail, then a value, switch or chevron; `destructive` rows are red |
| `GameListRow`, `GameCover` | Game rows and box art with an initial as a fallback |
| `BoardFlowInfoPill` | Small read-only pill |
| `BoardFlowEmptyState`, `BoardFlowErrorBanner` | Empty and error states |
| `LocalBoardFlowMessenger` | Snackbar messages with an optional action (Undo), hosted by `AppShell` |

### Controls, dialogs and sheets (`ui/common/BoardFlowUi.kt`)

| Component | Use |
| --- | --- |
| `BoardFlowButton` | Primary action: solid amber pill, one per view |
| `BoardFlowSecondaryButton` | Amber outline pill; `compact = true` for 32dp rows |
| `BoardFlowDestructiveButton` | Outlined red pill for actions that remove data |
| `BoardFlowInlineAction` | Text action; `neutral` for Cancel |
| `BoardFlowIconButton`, `BoardFlowFilterChip` | Icon buttons and filter chips |
| `PlayerAvatar` | Coloured initials, using the player's chosen colour (`LocalPlayerColors`) |
| `AnimatedDialog` | Large dialog shell with motion and bounded height |
| `BoardFlowConfirmationDialog` | Confirm / cancel for BGG writes and irreversible actions |
| `BoardFlowModalBottomSheet`, `BoardFlowPickerSheet`, `BoardFlowPickerField` | Sheets for filters, pickers and long-press actions |
| `SectionCard` | Legacy grouped card, kept for older screens |

### Navigation and motion

| Component | File | Use |
| --- | --- | --- |
| `ScreenTabRow`, `ScreenTabs` | `ui/common/ScreenTabRow.kt` | Tab row; one label size for every screen, sized so the longest label fits a four-tab row |
| `swipeToNavigateTabs` | `ui/common/ModifierExtensions.kt` | Switch tabs with a horizontal swipe |
| `BoardFlowTabContent` | `ui/common/BoardFlowMotion.kt` | Tab content that slides in from the side of the new tab |
| `BoardFlowAnimatedVisibility`, `BoardFlowPullRefreshContainer` | `ui/common/BoardFlowMotion.kt` | Panels that collapse on scroll; pull to refresh |
| `NavTransitions` | `ui/app/NavTransitions.kt` | Fade through between bottom bar screens, short slide into pushed screens |

### Other shared surfaces

| Component | File |
| --- | --- |
| `GameSearchField`, `SearchFieldActionButton` | `ui/common/GameSearchField.kt` |
| `PlayerResultEditorCard` (player rows in log, edit and QR import) | `ui/common/PlayerResultEditorCard.kt` |
| `GameBackdrop` (game art behind headers) | `ui/common/GameBackdrop.kt` |
| `BoardFlowCameraScene`, `BoardFlowCameraActionPanel`, `BoardFlowCameraPermissionPrompt` | `ui/common/BoardFlowCameraUi.kt` |

## App shell

Source: `ui/app/AppShell.kt`

- Top bar with the destination name, and a back arrow on pushed screens. A play timer chip appears while a game is timed: tap it to log the play, long-press to stop the timer
- Bottom navigation: **Log Play, Journal, Collection, Settings**. It is hidden on pushed screens (Scan, Log play form, Quick Setup, QR import)
- Snackbar host for `LocalBoardFlowMessenger`
- Discard confirmation when leaving the Log play form with unsaved changes

## Log Play

Source: `ui/search/NewPlayScreen.kt`

- Tabs **Log Play** and **Quick Guides**. They switch on tap or swipe, and the tabs, search and "Playing" row slide away while scrolling down
- Search looks only in your collection. With 2+ characters, the results end with an amber `Search BoardGameGeek for "..."` line (also under "No games in your collection match"), shared by every game search (Log Play, Quick Guides, My Shelf, the challenge picker). It opens a sheet of BGG results titled for what a pick does: "Log a play", "Open a quick guide", "Add to your collection", "Pick a game"
- `PlayingNowBanner` while a play timer runs, with a Log result button
- `SessionContinueBanner` to continue the last session, and a change-game notice
- `GameSearchField` with the quick scan camera button
- "Who's playing?" row for preselected players
- Recommendation lanes ("Good picks right now"), then all games, with a fast-scroll bar for long lists
- **Quick Guides:** `My games` (default; games without a guide are dimmed) and `All guides`

## Scan

Source: `ui/scan/ScanScreen.kt`

- Camera, gallery and manual entry
- Photo review: `Retake` (secondary) and `Use photo` (primary)
- Local quality warning ("This scan may be hard to read") in the warning colour: `Use anyway` (secondary) and `Retake` (primary)
- Extraction progress and error states

## Log play form

Source: `ui/review/LogPlayScreen.kt`

- Game art header with the game name, and the detected-game hint after a scan
- Main group: Date, Duration, Location, Notes. **More options:** Quantity, Incomplete play, Count in BGG stats
- "Also log an expansion" chips (`RelatedGamesBanner`)
- Players: suggested player pills (under a "Suggested" label), `Add player`, `Scan`, and a `PlayerResultEditorCard` for each player
- Scan banners: `ScanResultBanner`, `GameSuggestionBanner`, `ScanRetryBanner`; `AiOutputCard` with the raw response and model
- Bottom bar: player count, date and "saved on this device" when offline, and `Log play` / `Save locally`
- `PostSaveCard` after saving: headline (record moment), players with a translucent amber winner row, challenge progress (completed ones in green), `Play again`, `Edit play`, `Change game`, and "Try next" picks

## Quick Setup

Source: `ui/setup/QuickSetupScreen.kt`

- Header with the game, steps done and a progress bar
- Player count and content (modules) chips
- One checklist card per setup section. Once every step is ticked, the checklist folds into a single "All N setup steps done" row
- `Easy to forget` and `Start playing` cards; the attribution line with `Share guide` (sends the guide as a `.json` file) and, for the user's own version, `Use standard guide`
- `Updated guide available` card when the user's own version is older than the standard guide: `Keep mine` / `Use updated guide`
- `Edit guide` opens the guide editor
- An AI draft shows "Draft guide - check it against the rulebook." with `Mark as reviewed`; a guide of your own with no standard guide behind it has `Delete guide`
- `Start game` starts the play timer and returns to Log Play with the Playing now banner. The screen stays awake while open
- No guide: "No setup guide for X yet" with white `Back` and the `Draft from rulebook` pill (needs a Gemini key; picks a PDF, then "Reading the rulebook" with a spinner while Gemini drafts it). A standard guide that is not downloaded yet says to connect instead

## Guide editor

Source: `ui/setup/GuideEditorScreen.kt`

- A form: game name, then one grouped card per section (section name field and a grey "Shows always / with X" line, then each step's text and optional note, a grey line with its quantities and conditions, and move up / move down / delete). The grey lines have a chevron and open the quantities sheet: per-player values for each `{n}` in the text, special cases, the player-count chips the step shows at, and Either way / With / Without per module, with a `Done` pill. `Add step` per section, `Add section` at the end, a red delete per section
- White `Cancel` and the `Save` pill at the bottom; leaving with unsaved changes asks `Discard changes?`. Save stores the guide as your version

## Journal

Source: `ui/history/HistoryScreen.kt`

- Tabs **Plays, Challenges, Stats, Players** (tap or swipe). The tab content slides with the tab
- **Plays:** search with QR import and filters, the challenge strip, the unposted plays outbox (`Post`, `Post all`), and play rows listing every player
- `PlayDetailsDialog`: hero header, session card, players (winner row in translucent amber), highlights (moods and quote), chronicle; red delete and amber edit icons, `Play again`
- `EditPlayDialog`, `SharePlayQrDialog`, `SessionHubDialog` (session summary, memory, plays, rename, `ShareSessionQrDialog`)
- **Challenges** (`ui/challenges/ChallengesScreen.kt`): the Challenge Board summary, then Active, Paused, Missed, Completed and Archived sections; a `New challenge` button; `CreateChallengeDialog`; a menu with Edit, Pause or Resume, Archive or Restore, and Delete (with Undo)
- **Stats:** a `Plays` / `Collection` switch
  - Plays (`ui/history/PlayStatsTab.kt`): time range options (All time, year, month, 30 days), summary, play history heatmap, top games, players, rivalries, `HeadToHeadSection`, insights
  - Collection (`ui/collection/CollectionStatsTab.kt`): overview, play depth, complexity, sleeve coverage, top played, unplayed shelf
- **Players** (`ui/players/PlayersScreen.kt`): roster, player detail, `EditPlayerDialog` (name, aliases, BGG username, avatar colour); delete with Undo

## QR import

Source: `ui/history/QrPlayImportScreen.kt`

- Camera and "From image" scanning of play (`BFPLAY1:`) and session (`BFSESS1:`) codes
- `QrPlayImportReview`: the Log play form layout (grouped rows, quantity stepper, switches) with the player editor
- `QrSessionImportReview`: session summary and its plays in one group; imports all of them

## Collection

Source: `ui/collection/CollectionScreen.kt`

- Tabs **My Shelf** and **Sleeves** (tap or swipe; content slides with the tab)
- **My Shelf:** search, filter sheet (Show: Owned / Wishlist / Played, not owned / Any; Play status; player-count filters), game rows with a small white rating, pull to refresh
- Adding a game: under the search results the shared amber `Search BoardGameGeek for "..."` line (see Log Play) opens the "Add to your collection" sheet with BGG results (year, "In your collection" for games already there, a spinner on the row being opened). A result opens the game detail dialog on its collection status editor, whose save pill reads `Add to collection`; the snackbar says "Added to your BGG collection"
- **Sleeves** (`ui/collection/SleevesScreen.kt`): summary with `To sleeve` / `All owned`, game selector (`All` / `None`), size groups with owned counts, `SleeveInventorySheetContent`

### Game detail

Source: `ui/collection/GameDetailDialog.kt`

- Game art header with rating and status chips, and three equal amber buttons: `Log play`, `Setup`, `History`. The header fades out before the compact title bar appears
- Insight strip, mastery, your stats, player-count advice (Best for, Great with, Avoid), overview, ratings (`RatingPickerDialog`), collection status (`CollectionStatusEditorDialog`), sleeves
- Centred compact secondary buttons: `BGG`, `Rules`, `Drive`

## Settings

Source: `ui/settings/SettingsScreen.kt`

Every tab is a list of sections: a `BoardFlowSectionTitle` over one group of `BoardFlowSettingRow`s.

- **Sync** (`ui/sync/SyncScreen.kt`): Accounts (Google, BGG with your username, Sheet with its Google name), BoardGameGeek, Sleeves, Google Sheets (sync, CSV import, Drive folders and QR codes, the save-QR switch); the log bar and `LogDialog`; `GoogleManageDialog`, `BggEditDialog`, `SpreadsheetConnectDialog`
- **Preferences:** Stats, Logging plays (recommendations, chronicles, mood templates via `CustomMoodsDialog`), Collection (sleeve brand), Quick guides (update guides, import a guide, clear downloaded guides), Help (setup guide)
- **Scan:** Gemini (API key via `GeminiKeyDialog`, backup keys via `BackupKeysDialog`, model picker, refresh models) and Learned from scans (`RecognitionTemplatesDialog`, player hints, red clear rows)
- **Data:** Backup and restore (include secrets switch, export, import) and Storage (red clear cache row)

## Intro

Source: `ui/intro/IntroScreen.kt`

Shown once on first launch: features, getting started steps and a settings reference. The same content opens from Settings > Preferences > Setup guide.

## System-owned surfaces

Launched by the app but drawn by Android: document pickers (backup export and import, CSV), the photo picker for scans and QR images, the camera permission prompt, external links and share sheets.

## Maintenance

- Build new surfaces from the components above; do not style one-off cards, fields or buttons
- Use `AnimatedDialog` for app dialogs, `BoardFlowConfirmationDialog` only for BGG writes and irreversible actions, and bottom sheets for pickers and long-press menus
- Reversible local deletes get a snackbar with Undo instead of a confirmation
- Update this file when a screen, dialog or shared component is added or removed
