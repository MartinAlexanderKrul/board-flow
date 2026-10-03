# Logging

BoardFlow logs through `android.util.Log`. Each feature area has its own tag, so you can follow one flow in Logcat without the noise of the others. Paths are relative to `app/src/main/kotlin/cz/nicolsburg/boardflow/`.

> Network bodies and most traces are for debug builds. Release builds turn HTTP logging off (`HttpLoggingInterceptor.Level.NONE`), and passwords and API keys are always redacted.

## Levels

| Level | Use |
| --- | --- |
| `Log.d` | detailed trace and normal-path diagnostics |
| `Log.i` | important successful operations |
| `Log.w` | recoverable problems and degraded paths |
| `Log.e` | hard failures or exhausted retries |

## Main Tags

### `QuickScan`

File: `AppViewModel.kt`

Used for:

- entering correction mode
- selecting a replacement game during correction
- re-initializing extracted players after correction
- clearing correction mode

Typical messages:

- `Entering correction mode`
- `Correction game selected`
- `Re-initialized N player(s)`
- `Correction mode cleared`

### `AutoSwitch`

File: `AppViewModel.kt`

Used for:

- scan start
- game-recognition gate decisions
- unposted-play sync progress
- scan failure reporting

Typical messages:

- `Scan started`
- `gate=TITLE_GATE`
- `gate=TEMPLATE_CATEGORY_GATE`
- `gate=BLOCKED`
- `Syncing N unposted play(s)`
- `Sync complete`
- `Scan failed`

### `PlayerRecognition`

Files:

- `AppViewModel.kt`
- `data/PlayerRecognitionEngine.kt`

Used for:

- hint resolution
- alias resolution
- fuzzy resolution
- no-match trace
- hint save events
- clearing all saved hints

Typical messages:

- `hint 'X' -> 'Y'`
- `alias 'X' -> 'Y'`
- `fuzzy 'X' -> 'Y'`
- `no match 'X'`
- `saved hint 'X' -> 'Y'`
- `all player recognition hints cleared`

### `GameRecognition`

File: `data/GameRecognitionEngine.kt`

Used for:

- candidate-ranking start
- title-only and category-template ranking
- per-game score breakdown

Typical messages:

- `rankCandidates: title='X'`
- `rankCandidates: no title`
- score breakdown lines per candidate

### `Gemini`

Files: `data/GeminiRepository.kt`, `data/chronicle/GeminiChronicleLineGenerator.kt`

Used for:

- extraction start and per-attempt trace
- SSE stream lifecycle (first chunk received)
- key rotation and model rotation decisions
- zero-quota detection
- model listing
- parse success and parse degradation

Typical messages (score extraction):

```
GEMINI request score-extract start initialModel=gemini-flash-latest file=score_….jpg size=…B availableModels=N availableApiKeys=N
GEMINI request score-extract attempt=1/10 model=gemini-flash-latest key=1/2 url=…key=REDACTED
GEMINI response score-extract attempt=1/10 model=gemini-flash-latest code=200 elapsedMs=…ms streaming=true
GEMINI stream-started score-extract model=gemini-flash-latest attempt=1
GEMINI success score-extract model=gemini-flash-latest attempt=1 totalMs=… accumulated=…chars
GEMINI parsed score-extract date=… players=N game=… conf=… categories=N
```

Key rotation (RPM rate limit, extra key available):

```
GEMINI zero-quota score-extract model=gemini-flash-latest — skipping key rotation
GEMINI rotate-key score-extract http=429 model=gemini-flash-latest key=2/2 attempt=1/10
GEMINI rotate-model score-extract http=429 from=gemini-flash-latest to=gemini-flash-lite-latest resetKey=1/2 attempt=2/10
```

Model exhaustion and fallback:

```
GEMINI zero-quota score-extract model=gemini-flash-latest — skipping key rotation
GEMINI rotate-model score-extract http=429 from=gemini-flash-latest to=gemini-flash-lite-latest resetKey=1/2 attempt=1/10
```

Parse degradation (JSON mode active; should be rare):

```
GEMINI parse-error score-extract error=…
```

Model listing:

```
GEMINI request list-models start
GEMINI response list-models api=v1beta code=200 body=…
GEMINI success list-models api=v1beta count=N
```

Model choice is automatic (`data/GeminiModels.kt`): the `gemini-flash-latest` aliases first, then the key's own model list, newest stable Flash first. A model that answers HTTP 404 or cannot take the request is dropped and the next one is tried.

### `Chronicle`

File: `data/chronicle/GeminiChronicleLineGenerator.kt`

Chronicle line generation. Messages follow the Gemini pattern, with `chronicle` in place of `score-extract`, and fall back to the offline composer when every attempt fails.

### `ScanQuality`

File: `data/ScanImageQualityAnalyzer.kt`

Used for:

- local image readability checks before Gemini

Typical messages:

- `resolution: WxH < MIN`
- `Could not decode image`
- `avg luma=...`
- `laplacian variance=...`
- `content area ratio=...`
- `result: OK`
- `result: POOR [...]`

### `BggApiClient`

File: `data/BggApiClient.kt`

Used for:

- BGG XML calls
- sleeve fetch and parse paths
- low-level API diagnostics

Typical messages:

- `ThingDetail id=...`
- `Fetching BGG sleeves for gameId=...`
- `BGG sleeves HTTP ...`
- `Fetching BGG sleeve API for gameId=...`
- `Parsed BGG sleeve API for gameId=...`
- `Relevant sleeve lines ...`

### `BggRepository`

File: `data/BggRepository.kt`

Used for:

- login
- play post
- play delete

Typical messages:

- `Login success for ...`
- `Play logged: gameId=...`
- delete confirmation-step traces

### `BggPlayPostWorker`

File: `data/BggPlayPostWorker.kt`

Background posting of unposted local plays once the device is online.

Typical messages:

- `Posted play <local id> -> <BGG id>`
- `Failed to post play <id>: ...`

### `SetupGuides`

Files: `data/setupguide/`

Quick Setup catalog refresh, guide downloads and validation failures. Network failures are logged and otherwise silent.

### `RulebookLinks`

File: `data/RulebookLinks.kt`

Loading of the bundled `rulebooks.json` that backs the Rules button in game detail.

## HTTP Logging

`BggApiClient` and `BggRepository` use `HttpLoggingInterceptor`: full request and response bodies in debug builds, nothing in release builds. Lines are logged through the module's tag, and the BGG password is redacted. Debug logs still contain session cookies, so do not share raw debug Logcat output.

## Logcat Filter Examples

```text
tag:ScanQuality | tag:Gemini | tag:GameRecognition | tag:AutoSwitch
```

Full scan path.

```text
tag:QuickScan | tag:PlayerRecognition
```

Quick scan correction and scanned-player resolution.

```text
tag:BggApiClient | tag:BggRepository | tag:BggPlayPostWorker
```

BGG network activity and background posting.

```text
level:warn
```

Warnings and errors only.

## Maintenance Notes

- add new tags only when a feature area has enough complexity to justify filtering independently
- prefer stable message prefixes so developers can search exact substrings over time
- keep logs descriptive but avoid dumping secrets, raw auth values, or entire sensitive payloads
