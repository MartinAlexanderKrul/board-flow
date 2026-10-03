# Insights, challenges and recommendations

BoardFlow makes your play history feel alive without points, levels or random rewards. Everything here is computed from the plays you log. This document describes the shipped mechanics and where they live.

All paths are relative to `app/src/main/kotlin/cz/nicolsburg/boardflow/`.

## Design Principles

- deterministic rules over random rewards
- no pressure-heavy gamification loops
- signals appear only when the data supports them
- reuse existing history data instead of adding a parallel progression system

## Insight Rarity

Defined in `model/Models.kt` as `InsightRarity`.

| Tier | Label | Meaning |
| --- | --- | --- |
| `COMMON` | Moment | baseline observation |
| `NOTABLE` | Notable | emerging pattern or small milestone |
| `RARE` | Landmark | meaningful achievement |
| `EPIC` | Chronicle | large achievement or memorable trend |
| `LEGENDARY` | Legacy | rare long-term record |

Rarity drives the visual treatment on the stats surfaces. The tier colours live in `InsightRarityColors` (`ui/theme/Theme.kt`):

- accent colour and gradient
- emphasis of the card
- shimmer and haptic feedback on the strongest cards

## Insight Surfaces

Core files:

- `ui/history/PlayStatsHelpers.kt`
- `ui/history/InsightStripCard.kt`
- `ui/history/PlayStatsTab.kt`
- `ui/collection/GameDetailDialog.kt`

Current insight surfaces:

- `ContextualInsightStrip`
- `PlayInsightStrip`
- `HeroObservationCard`
- `PeriodReviewCard`
- `Table Brief`
- post-log `RecordMoment`

## Shipped Insight Categories

The current system includes:

- milestone observations
- approaching-milestone nudges
- rivalry observations
- dormant-game nudges
- anniversary observations
- patron-game observations
- period review summaries

`RecordMoment` currently surfaces:

- first win
- new high score
- win streak

## Game Mastery

`ui/collection/GameDetailDialog.kt` renders a mastery pill in `YourStatsCard`.

Current labels:

| Plays | Label |
| --- | --- |
| `1-4` | Learning |
| `5-14` | Familiar |
| `15-29` | Comfortable |
| `30-49` | Practiced |
| `50-99` | Deep |
| `100+` | Mastered |

This is intentionally quiet UI. There is no progression bar or ceremony.

## Hero Observation Motion

`HeroObservationCard` in `PlayStatsTab.kt` currently uses:

- spring entrance scale from `0.95` to `1.0`
- one-time shimmer for `EPIC` and `LEGENDARY`
- `LongPress` haptic on `EPIC` and `LEGENDARY` reveal

## Period Review

`buildPeriodReview()` creates an auto-generated review card at the top of Journal > Stats > Plays.

Trigger windows:

- January 1-7: previous year review
- days 1-5 of other months: previous month review

Content includes:

- total plays
- unique games
- new players
- one highlight sentence when there is enough supporting data

## Session Memory

BoardFlow supports a lightweight journaling layer tied to individual plays.

Memory can include:

- moods
- quote
- note
- chronicle line

Primary files:

- `data/SessionMemoryJson.kt`
- `data/CanonicalCollectionStore.kt`
- `data/chronicle/SessionChronicleService.kt`
- `ui/history/HistoryScreen.kt`

### Persistence

Session memories live in the Room table `play_memories`.

Important notes:

- the Room database is at version 13 (`CanonicalCollectionStore`)
- `play_memories` is independent of BGG sync
- read paths overlay stored memory onto both local and cached BGG plays
- if no Room memory exists, legacy `$$mood:` and `$$quote:` lines in comments can still be parsed as fallback

### Chronicle Generation

Chronicles are generated through `SessionChronicleService`.

Flow:

1. Build a source key from game, players, moods, quote, and related memory inputs.
2. Reuse the existing chronicle when the source key still matches.
3. Persist memory immediately, then launch generation in the background if needed.
4. Try Gemini first.
5. Fall back deterministically when Gemini fails or is unavailable.

Chronicle behavior:

- generation can be disabled globally through the `chronicle_enabled` preference
- opening a play with memory but no chronicle can trigger generation automatically
- pending plays expose a placeholder state through `chroniclePendingPlayIds`

## Challenges

Challenges are personal goals computed directly from play history.

Core files:

- `model/Models.kt`
- `AppViewModel.kt`
- `ui/challenges/ChallengesScreen.kt`
- `ui/history/HistoryScreen.kt`

### Challenge Types

Current supported types:

- `PLAY_N_TIMES`
- `PLAY_SPECIFIC_GAME`
- `PLAY_N_DISTINCT`
- `PLAYER_WIN_STREAK`
- `PLAY_WITH_GROUP_N_TIMES`
- `PLAY_STREAK`
- `PLAY_N_UNPLAYED`

### Progress Model

`ChallengeProgress` currently tracks:

- `currentCount`
- `goalCount`
- `remainingText`
- `isComplete`
- `isFailed`
- `isActive`
- `fraction`

### Persistence

Challenges are stored in Room (`challenges` table in `CanonicalCollectionStore`).

On first load, `AppViewModel` migrates challenges from `SecurePreferences` to Room if the Room table is empty. `SecurePreferences` retains a legacy copy for backup compatibility.

### Lifecycle

Current shipped lifecycle:

- load (from Room; migrates from SecurePreferences on first load)
- auto-create monthly challenge when needed
- create
- edit (`AppViewModel.updateChallenge`)
- pause and resume (`AppViewModel.pauseChallenge`, status `PAUSED`)
- archive and restore (`AppViewModel.archiveChallenge`, status `ARCHIVED`)
- delete
- every change shows a snackbar; pause, archive and delete offer Undo
- live progress calculation via `AppViewModel.getChallengeProgressList()`
- push notifications via `ChallengeNotificationWorker` (WorkManager):
  - deadline warning when 3 days remain and challenge is incomplete
  - completion notification when `currentCount >= targetCount`
  - streak-broken notification for `PLAY_STREAK` type challenges

The Challenges tab in Journal (`ChallengesTabContent`) shows the Challenge Board summary, then collapsible sections: Active, Paused, Missed (the period ended before the goal was reached), Completed and Archived. Status colours: amber for active, `BoardFlowColors.Warning` for paused, `BoardFlowColors.Success` for completed, and the error colour for missed. A challenge whose name repeats its goal shows only the name.

## Recommendations

BoardFlow currently ships two recommendation systems controlled by `recommendationsEnabled`.

### New Play Recommendations

`NewPlayScreen` shows recommendation lanes when:

- the query is blank
- recommendations are enabled
- enough collection and history context exists

### Post-Save Good Picks

The post-save card in `LogPlayScreen` shows a collapsible "Try next" section after logging when:

- recommendations are enabled
- the player count can be matched against owned games

### Player Count Fit Rules

The current scoring priority in `AppViewModel.playerCountFitScore()` is:

| Source | Score | Result |
| --- | --- | --- |
| `notRecommendedPlayers` | `0.0` | filtered out |
| `bestPlayers` | `2.0` | strongest fit |
| `recommendedPlayers` | `1.6` | strong fit |
| official `minPlayers-maxPlayers` | `1.2` | fallback fit |
| no match | `0.0` | filtered out |

### Data Sources

The relevant collection fields live on canonical games:

- `bestPlayers`
- `recommendedPlayers`
- `notRecommendedPlayers`
- `minPlayers`
- `maxPlayers`

The `notRecommendedPlayers` field was added in migration `6 -> 7`.

## Quick Reference

| Feature | Primary file |
| --- | --- |
| rarity model | `model/Models.kt` |
| contextual and smart observations | `ui/history/PlayStatsHelpers.kt` |
| insight strips | `ui/history/InsightStripCard.kt` |
| stats hero card | `ui/history/PlayStatsTab.kt` |
| mastery pill | `ui/collection/GameDetailDialog.kt` |
| challenge state | `AppViewModel.kt` |
| challenge UI | `ui/challenges/ChallengesScreen.kt` (Journal > Challenges) |
| pre-log recommendations | `ui/search/NewPlayScreen.kt` |
| post-log recommendations | `ui/review/LogPlayScreen.kt` (post-save card) |
| chronicle orchestration | `data/chronicle/SessionChronicleService.kt` |
