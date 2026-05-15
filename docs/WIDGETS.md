# BoardFlow Widgets

BoardFlow ships two home-screen widgets. Both are implemented with Jetpack Glance and share a single base class (`SessionGlanceWidget`) that owns all layouts, size tiers, and rendering. The only difference between them is the content each one supplies.

Widget files live in `app/src/main/kotlin/…/ui/widget/`.

---

## Shared architecture

### `SessionGlanceWidget` (base class)

`ui/widget/SessionsWidget.kt` — `open class SessionGlanceWidget : GlanceAppWidget()`

Owns everything visual: size registration, layout composables, header-bitmap renderer, and the `WidgetSnapshot` data model. Neither widget subclass touches layouts or rendering.

#### `WidgetSnapshot` — unified content model

```
header       String   Category label, e.g. "Last Session". Rendered as a Cinzel bitmap
                      prefixed with "Board Flow – ", so the widget shows
                      "Board Flow – Last Session".
primaryText  String   Bold main line shown in every non-tiny tier
subtitleText String   Dimmed secondary line — Small and Expanded tiers (empty = hidden)
detailText   String   Text block below the divider — Expanded tier only (empty = hidden)
accentColor  Color    Drives header bitmap colour
gameId       Int      BGG game ID (0 = none). When non-zero, tapping the widget body
                      opens History filtered to that game.
```

Subclasses fill these fields in `computeSnapshot()`. Layouts are blind to content type.

#### Header rendering

`renderHeader(context, "Board Flow – $header", 17f, accentColor)` produces a single `Bitmap` using the Cinzel Decorative Bold typeface. The bitmap is displayed at a fixed `height` in dp so every widget shows the same physical title height regardless of text length.

#### Accent colours

| Name | Hex | Used for |
|---|---|---|
| NEUTRAL / AMBER | `#FEB316` | Session widget, common/notable insights |
| BLUE | `#7EA7FF` | Rare insights |
| TEAL | `#80CBC4` | Epic / Legendary insights |

---

### Responsive size tiers

Both widgets use `SizeMode.Responsive` with nine registered sizes (1×1 through 3×3 cells). The dispatcher in `WidgetRoot` picks a tier based on the allocated `LocalSize`:

| Tier | Condition | Content | Camera size |
|---|---|---|---|
| **Tiny** | width < 90 dp | Camera button only (1×1) | 24 dp |
| **Compact** | height < 90 dp | Header bitmap (18 dp tall) · `primaryText` · camera | 30 dp |
| **Small** | height ≥ 90 dp, not tall+wide | Header bitmap (22 dp tall) · `primaryText` · `subtitleText` · camera | 38 dp |
| **Expanded** | height ≥ 140 dp, width ≥ 160 dp | Header bitmap (22 dp tall) · `primaryText` · `subtitleText` · divider · `detailText` · camera | 46 dp |

The camera icon is vertically centred in the widget for all non-tiny tiers.

**Tapping the body** — if `snapshot.gameId != 0`, launches `OpenPlayCallback` which opens the app and navigates to History filtered to that game. Otherwise opens the app at its last screen.  
**Tapping the camera icon** — launches `QuickScanCallback` which jumps straight into Quick Scan.

---

## 1. Session Widget (`SessionWidget`)

**File:** `ui/widget/SessionsWidget.kt`  
**Info:** `res/xml/sessions_widget_info.xml`

| Property | Value |
|---|---|
| Default size | 1 × 1 cells (resizes up) |
| Resizable | Yes — horizontal and vertical |
| Description | *"Your last board game session — game, players, scores, and result. Resize from a quick-tap icon up to a full session summary."* |

**What it shows:** The most recently logged play session.

| Snapshot field | Content |
|---|---|
| `header` | `"Last Session"` → displayed as `"Board Flow – Last Session"` |
| `primaryText` | `"Everdell  ·  Today"` |
| `subtitleText` | `"4 players  ·  Martin won"` (empty if no players) |
| `detailText` | One line per player: name, score, ★ for winner |
| `gameId` | BGG game ID of the last session — tapping opens History for that game |

**Update model:** Refreshes every 5 minutes via an `AlarmManager` alarm broadcasting `ACTION_ROTATE_SESSION`.

---

## 2. Daily Insight Widget (`DailyInsightWidget`)

**File:** `ui/widget/DailyInsightWidget.kt`  
**Info:** `res/xml/daily_insight_widget_info.xml`

| Property | Value |
|---|---|
| Default size | 3 × 1 cells (resizes down to 1×1) |
| Resizable | Yes — horizontal and vertical |
| Description | *"A daily insight from your play history — milestones, rivalries, dormant games, seasonal patterns, and more."* |

**What it shows:** One observation from `buildSmartObservations()` (the same engine behind the Stats hero card), rotating to the next each day, sorted by rarity so the most significant insights appear first.

| Snapshot field | Content |
|---|---|
| `header` | Observation category, e.g. `"Patron Game"` → displayed as `"Board Flow – Patron Game"` |
| `primaryText` | Full observation sentence |
| `subtitleText` | *(empty)* |
| `detailText` | *(empty)* |
| `gameId` | `0` — body tap opens the app at its last screen |

**Rotation logic:** On each new day, the widget advances to the next observation in rarity-descending order. The current day and last-shown text are persisted in `DailyInsightWidgetPrefs`.

**Accent colour** reflects observation rarity:

| Rarity | Colour |
|---|---|
| LEGENDARY / EPIC | TEAL `#80CBC4` |
| RARE | BLUE `#7EA7FF` |
| NOTABLE / COMMON | NEUTRAL `#FEB316` |

**Update model:** Refreshes every 5 minutes via an `AlarmManager` alarm broadcasting `ACTION_ROTATE_DAILY_INSIGHT`.

---

## Extending the widgets

### Adding content to an existing widget

Both widgets only override `computeSnapshot()`. To change what a widget displays, edit that method and populate the `WidgetSnapshot` fields — no layout changes needed.

### Adding a new widget

1. Create a class that extends `SessionGlanceWidget`.
2. Override `computeSnapshot()` to return a `WidgetSnapshot`.
3. Add a `GlanceAppWidgetReceiver` subclass.
4. Register the receiver in `AndroidManifest.xml` with an `appwidget-provider` XML.
