# BoardFlow Logging Reference

All logging uses Android's native `android.util.Log`. Each module defines a `TAG` constant so logs can be filtered by tag in Logcat.

---

## Log Levels

| Level | Usage |
|-------|-------|
| `Log.d` | Debug — detailed diagnostic data, normal-path trace |
| `Log.i` | Info — important operations completing successfully (play posted, login, sync) |
| `Log.w` | Warning — recoverable issues: HTTP rate-limit, fallback triggered, parse degraded |
| `Log.e` | Error — non-recoverable failures: all retries exhausted, unexpected HTTP, scan failed |

---

## Tags

### `QuickScan`
**File:** `AppViewModel.kt`  
**Keyword to filter:** `QuickScan`

Covers the quick-scan correction flow — entering/exiting correction mode and game selection within it.

| Keyword | Level | Meaning |
|---------|-------|---------|
| `Entering correction mode` | `d` | User tapped "wrong game" from LogPlay; scan data preserved |
| `Correction game selected` | `d` | User picked a new game in correction mode |
| `Re-initialized N player(s)` | `d` | Extracted players re-applied after correction |
| `Correction mode cleared` | `d` | Mode exited; includes reason string |

---

### `AutoSwitch`
**File:** `AppViewModel.kt`  
**Keyword to filter:** `AutoSwitch`

Covers the scan pipeline: starting a scan, game auto-detection gate decisions, sync of unposted plays, and scan errors.

| Keyword | Level | Meaning |
|---------|-------|---------|
| `Scan started` | `d` | Image handed to Gemini; logs preselected game |
| `gate=TITLE_GATE` | `d` | Auto-switch fired via title match; full gate breakdown |
| `gate=TITLE_GATE(strong)` | `d` | Auto-switch with relaxed 90% Gemini threshold (strong title match) |
| `gate=TEMPLATE_CATEGORY_GATE` | `d` | Auto-switch fired via category template |
| `gate=BLOCKED` | `d` | Auto-switch did not fire; logs all gate values |
| `Syncing N unposted play(s)` | `i` | Offline sync started |
| `Sync complete: N/M play(s)` | `i` | Offline sync finished; shows success ratio |
| `Scan failed` | `e` | Gemini extraction returned a failure |

---

### `PlayerRecognition`
**Files:** `AppViewModel.kt`, `PlayerRecognitionEngine.kt`  
**Keyword to filter:** `PlayerRecognition`

Covers player name resolution (hint → alias → fuzzy) and hint lifecycle.

| Keyword | Level | Meaning |
|---------|-------|---------|
| `hint 'X' -> 'Y'` | `d` | Resolved via saved scan hint; includes confidence and ambiguity flag |
| `alias 'X' -> 'Y'` | `d` | Exact display name or alias match |
| `fuzzy 'X' -> 'Y'` | `d` | Levenshtein match; includes distance and confidence |
| `no match 'X'` | `d` | Name not resolved; left as scanned |
| `saved hint 'X' -> 'Y'` | `d` | New hint saved after play was logged |
| `all player recognition hints cleared` | `d` | User cleared all hints in Settings |

---

### `GameRecognition`
**File:** `GameRecognitionEngine.kt`  
**Keyword to filter:** `GameRecognition`

Covers candidate scoring for each game in the collection against Gemini's extracted evidence.

| Keyword | Level | Meaning |
|---------|-------|---------|
| `rankCandidates: title='X'` | `d` | Ranking started with a detected title |
| `rankCandidates: no title` | `d` | Ranking using categories only |
| `[GameName] title=N%` | `d` | Per-game score breakdown: title sim, category boost, template overlap, final score |

---

### `Gemini`
**File:** `GeminiRepository.kt`  
**Keyword to filter:** `Gemini`

Covers Gemini API calls: extraction lifecycle, model fallback, model listing, and response parsing.

| Keyword | Level | Meaning |
|---------|-------|---------|
| `Starting extraction` | `d` | New extraction started; logs model, file name/size, available model count |
| `Attempt N/M` | `d` | Per-attempt trace with current model |
| `Success` | `d` | HTTP 200; logs model used, attempt number, timing |
| `HTTP 503/429 ... switching model` | `w` | Rate-limited; trying next model |
| `HTTP 503/429 ... no fallback model` | `e` | Rate-limited with no fallback — all models exhausted |
| `HTTP N error` | `e` | Unexpected HTTP error from Gemini |
| `Failed after N attempts` | `e` | All retry attempts consumed |
| `Found N model(s)` | `d` | Model list fetch succeeded |
| `Model list HTTP N` | `w` | Non-200 response from model list endpoint |
| `Model list error` | `w` | Exception fetching model list |
| `Parsed: date= players= game= conf=` | `d` | Successful JSON parse; key extracted fields |
| `Parse error` | `w` | JSON parse failed; partial extraction attempted |

---

### `ScanQuality`
**File:** `ScanImageQualityAnalyzer.kt`  
**Keyword to filter:** `ScanQuality`

Covers image quality pre-checks before Gemini is called.

| Keyword | Level | Meaning |
|---------|-------|---------|
| `resolution: WxH < MIN` | `d` | Image too small |
| `Could not decode image` | `w` | Bitmap decode failed; analysis skipped, scan proceeds |
| `avg luma=N` | `d` | Brightness measurement |
| `laplacian variance=N` | `d` | Blur/sharpness score |
| `content area ratio=N` | `d` | Estimated fraction of image containing score-sheet content |
| `result: OK` | `d` | Image passed all checks |
| `result: POOR [kinds]` | `d` | Image failed one or more checks; lists issue kinds |

---

### `BggApiClient`
**File:** `BggApiClient.kt`  
**Keyword to filter:** `BggApiClient`

Covers BGG XML API and sleeve data fetching.

| Keyword | Level | Meaning |
|---------|-------|---------|
| `-->` / `<--` request lines | `d` | OkHttp basic request/response logging in debug builds only; headers carrying tokens/cookies are redacted |
| `ThingDetail id=` | `i` | Game detail fetched: weight, player counts, age, language dep |
| `Fetching BGG sleeves for gameId=` | `i` | Sleeve page fetch started |
| `BGG sleeves HTTP N` | `w` | Non-200 from sleeve HTML page |
| `BGG sleeves fetch failed` | `w` | Exception fetching sleeve page |
| `Fetching BGG sleeve API for gameId=` | `i` | Card-sets JSON API call started |
| `BGG sleeve API response for gameId=` | `i` | Raw API response snippet |
| `Parsed BGG sleeve API for gameId=` | `i` | Card sets successfully parsed from API |
| `BGG sleeve API returned no card sets` | `i` | API returned empty/no-sleeve result |
| `BGG sleeve API HTTP N` | `w` | Non-200 from sleeve JSON API |
| `BGG sleeve API failed` | `w` | Exception from sleeve API |
| `Parsing BGG sleeves url=` | `i` | HTML sleeve parse started; logs page title and key signals |
| `Relevant sleeve lines` | `i` | Lines containing sleeve/card/mm keywords |
| `No relevant sleeve lines` | `i` | No sleeve-related content found in page |
| `Parsed sleeve data` | `i` | Card sets successfully extracted from HTML |
| `BGG script sources` | `i` | Script URLs logged when no sleeve data found (debug aid) |
| `BGG window signals` | `i` | window.* assignments logged when no sleeve data found |
| `BGG API hints` | `i` | API/GraphQL URLs logged when no sleeve data found |
| `BGG sleeve script hints` | `i` | Inline scripts mentioning sleeves/cardsets |
| `BGG sleeve API object keys` | `i` | Top-level JSON keys in API response (object form) |
| `BGG sleeve API array length` | `i` | Array length in API response (array form) |

---

### `BggRepository`
**File:** `BggRepository.kt`  
**Keyword to filter:** `BggRepository`

Covers BGG login, play logging, and play deletion.

| Keyword | Level | Meaning |
|---------|-------|---------|
| `-->` / `<--` request lines | `d` | OkHttp basic request/response logging in debug builds only; headers carrying tokens/cookies are redacted |
| `Login success for` | `i` | Session cookie obtained |
| `Play logged: gameId=` | `i` | Play saved on BGG; logs game, date, player count, returned play ID |
| `Delete play confirmation step` | `i` | First delete POST response snippet |
| `Delete play confirm step` | `i` | Second (final) delete POST response snippet |

---

## Logcat Quick-filter Examples

```
# All scan pipeline (quality check → Gemini → game recognition → auto-switch)
tag:ScanQuality | tag:Gemini | tag:GameRecognition | tag:AutoSwitch

# Full play-log flow
tag:AutoSwitch | tag:PlayerRecognition | tag:BggRepository

# Quick-scan correction
tag:QuickScan | tag:PlayerRecognition

# Sleeve lookup
tag:BggApiClient

# Errors and warnings only
level:warn
```
