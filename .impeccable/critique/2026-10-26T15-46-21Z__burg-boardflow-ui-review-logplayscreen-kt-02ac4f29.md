---
target: Log Play flow
total_score: 29
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
target_identity: "file:C:\\SOURCE\\PERSONAL\\BoardFlow\\board-flow\\app\\src\\main\\kotlin\\cz\\nicolsburg\\boardflow\\ui\\review\\LogPlayScreen.kt"
target_fingerprint: "sha256:77149916fbfbb2a965a65ba900173db31c093bd3f467e3715454c9b92279a6b6"
target_path: "C:\\SOURCE\\PERSONAL\\BoardFlow\\board-flow\\app\\src\\main\\kotlin\\cz\\nicolsburg\\boardflow\\ui\\review\\LogPlayScreen.kt"
timestamp: 2026-10-26T15-46-21Z
slug: burg-boardflow-ui-review-logplayscreen-kt-02ac4f29
---
Method: design review only (detector not applicable, native platform)

Target: Log Play flow (game picker in NewPlayScreen -> LogPlayScreen form -> post-save card). Evidence: source read plus emulator captures (Pixel 8 Pro image, 1.0 and 1.3 font scale, portrait and landscape).

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 3 | Save button says "Saving...", "Save locally" offline, footer summary; good |
| 2 | Match System / Real World | 3 | "AI output", "Quantity" and "Incomplete play" need a word of context |
| 3 | User Control and Freedom | 3 | Discard confirmation, Undo patterns, Play again / Edit play after save |
| 4 | Consistency and Standards | 3 | Post-save card drifts from the system (caps, gradient, shadow, ad hoc alphas, off-grid spacing) |
| 5 | Error Prevention | 3 | Score field filters letters; Rating accepts any number, no 1-10 check |
| 6 | Recognition Rather Than Recall | 3 | Suggested players, location suggestions, expansion chips |
| 7 | Flexibility and Efficiency | 3 | Scan, score sheet, Play again, good picks; no quick score stepping |
| 8 | Aesthetic and Minimalist Design | 3 | Form is calm; post-save card is the noisiest surface in the flow |
| 9 | Error Recovery | 3 | Error banner above the save bar keeps the form intact |
| 10 | Help and Documentation | 2 | More options toggles carry no explanation of what they change on BGG |
| **Total** | | **29/40** | **Good** |

## Design Specificity Verdict

LLM assessment: The form is specific to this product. Box art behind the game's name, "Also log an expansion" chips, a score-sheet entry with the game's own drawn icons, suggested players as one-tap pills, and a footer that summarises "2 players, Oct 26, 2026" next to the single amber save pill: nothing here is a generic form. The post-save card is the exception: concentric glow rings, an amber wash, an all-caps eyebrow and fireworks are the category's default "celebration" kit and could sit in any habit tracker.

Deterministic scan: not applicable (native platform).

## Overall Impression

The table-side path is fast and disciplined: game, players, trophy, score, one amber pill. The weakest moment is the end: the post-save card, the peak of the flow, is the least readable and least on-system surface, and it misstates the date for back-dated plays.

## What's Working

- One primary action, always in the thumb zone: the save pill lives in a fixed footer with a live summary ("2 players · Oct 26, 2026 · saved on this device"), and its label changes for offline and multi-game saves ("Save 2 plays locally").
- Player rows collapse to one line (avatar, name, trophy, score), so a four-player game fits on one screen; saved players are added as single taps from "Suggested".
- Type scales cleanly at 1.3x font scale; no clipping in the form, the footer or the player rows.

## Priority Issues

**[P1] Post-save card text is too faint to read in a dim room**
- Why it matters: the card sits on Material's default card colour (#3D3D3E) and uses Ash at 40-60% alpha for the date line, "Challenges", "Still in progress", challenge titles and counts. That is roughly 2:1 contrast; even full Ash on that card is about 4:1. PRODUCT.md commits to readability in dim rooms, and this is the moment people look at the phone together.
- Fix: put the card on Graphite (surface), use full Ash for secondary text and Chalk for anything worth reading; drop the alpha ladder.
- Suggested command: /impeccable polish

**[P1] Post-save card shows today's date for a back-dated play**
- Why it matters: logging last night's game the next morning shows today's weekday and date on the confirmation, which reads as if the date was not saved.
- Fix: format the saved play's date (anchorPlay) instead of LocalDate.now().
- Suggested command: /impeccable harden

**[P1] Winner toggle and score fields are unclear to TalkBack**
- Why it matters: the trophy is announced as "Winner" whether or not it is on, and every score field is just "Score", so a screen-reader user cannot tell whose score they are typing or who won.
- Fix: give the trophy toggleable semantics with a state ("John Doe, winner" / "not winner") and label each score field with the player's name.
- Suggested command: /impeccable audit

**[P2] Post-save card drifts from DESIGN.md**
- Why it matters: all-caps game name breaks the Sentence Case Rule, the amber gradient wash breaks the No Brown Rule, a 16dp shadow breaks flat tonal depth, paddings are 10/11/14/20/22dp off the 4dp scale, and the close button is 32dp (below the 48dp floor).
- Fix: sentence-case title in amber, remove the wash and shadow, use Spacing tokens, 48dp close target.
- Suggested command: /impeccable polish

**[P2] Detected-game banner secondary lines are faint and off-grid**
- Why it matters: "Detected game", the confidence line and the evidence line use Ash at 50-60% on Raised Graphite (about 2.5:1), and the card pads 14/10/3dp.
- Fix: full Ash, Spacing tokens.
- Suggested command: /impeccable polish

## Persona Red Flags

**Casey (distracted, one-handed at the table)**: Save, Add player and scores are in the bottom half; good. The post-save card's date and challenge lines are unreadable at arm's length in low light, and the close X is a 32dp target in the top corner, the hardest spot to reach.

**Sam (TalkBack)**: Trophy announces "Winner" without on/off state; score fields are all "Score"; the "Try next" header toggles but announces no role or state.

**Riley (stress tester)**: Logs yesterday's game and the confirmation says today. Types "99" in Rating and it is accepted without a hint that BGG wants 1-10.

## Minor Observations

- The picker screen shows "Log Play" three times (top bar, tab, nav) and in title case while the form says "Log play".
- "AI output" toggle and "Raw AI response" card are developer-facing language on a user surface; the card also carries an outline and 14dp padding.
- The "Who's playing?" picker panel uses an outline and labelSmall chips, the smallest text in the flow.
- Landscape stretches the form to the full window width; usable, but rows become very wide.

## Questions to Consider

- Does the post-save moment need fireworks and glow rings, or would the winner's name in amber on a quiet card feel more like the ledger?
- Should the More options toggles explain what they do on BGG in a supporting line?
- Would a score stepper or a "same as last time" for repeat players save more taps than the keyboard?
