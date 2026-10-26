# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Hobbyist board gamers who use BoardGameGeek: they have a BGG account and a collection there, and they log most of their plays. The main moment of use is at the table on game night, often in dim light, between games or right after one ends, with the phone in one hand and friends waiting. Secondary moments: picking what to play, setting up a game from a guide, and browsing stats, challenges and the collection at home.

## Product Purpose

BoardFlow is a board game play journal for Android. It makes logging a play fast enough to do every time (pick a game, players, scores, winner; or scan a paper score sheet; or score on a built-in sheet), keeps BGG in sync in the background, and turns the logged history into stats, challenges, records and session memories. Quick Setup guides get a game to the table, and score sheets close it out.

Success: a play is logged before the box is packed away, and the BGG record is right without the user thinking about it.

## Positioning

- **Speed at the table.** Logging takes seconds: collection-only search, "Good picks right now", score sheet scanning with Gemini, built-in score sheets that fill totals and winner, and offline-first saving that posts to BGG later.
- **BGG-native.** BGG is the system of record. Plays post to BGG, collection status, ratings and sleeve markers write back to it, and the collection refreshes from it. BoardFlow does not keep a competing account or cloud of its own.
- **Open community content.** Setup guides (`setup-guides/`) and score sheets (`score-sheets/`) are open JSON in the repo, bundled and updated from GitHub. AI drafts become shared content once a user reviews them, through a PR the maintainer merges.

## Operating Context

- Game night: logging after a game, a play timer started from a guide, session continuation and "Play again" across a night of several games.
- Paper score pads photographed and read by Gemini (the user's own API key); rulebook PDFs handed to Gemini to draft guides and score sheets.
- BGG account (credentials stored on device); optional Google Sheets / Drive sync of the collection; QR codes to share a play between phones; home-screen widgets with a quick-scan camera button.
- Expansions, big boxes and editions follow BGG's model (an expansion played with its base game is its own BGG play, paired back into one sitting in the app).

## Capabilities and Constraints

- Native Android app in Kotlin / Jetpack Compose (Material 3), min SDK 26, target 36. Distributed as APKs on GitHub releases; Google Play release prepared in `play-store/`.
- Offline first: Room is the runtime source of truth; every BGG write is optimistic and retried.
- AI features (scan, chronicle, guide and score-sheet drafts) need the user's own Gemini key and degrade gracefully without one.
- Google scopes stay `drive.file` + `spreadsheets`; the full `drive` scope is ruled out for the Play release.
- BGG collection writes use undocumented endpoints and can break without notice.
- Terminology: play, sitting/session, Journal (Plays, Challenges, Stats, Players), My Shelf, Sleeves, Quick Setup / Quick Guides, score sheet, outbox, chronicle, record moment.
- Full feature and architecture record: `AGENTS.md`, `README.md`, `docs/UI_SURFACES.md`.

## Brand Commitments

- Name: BoardFlow ("BoardFlow: Board Game Journal" on Play).
- An independent app, not affiliated with or endorsed by BoardGameGeek; never imply otherwise.
- No ads, no analytics, no accounts of its own; data stays on the phone and in accounts the user connects (`play-store/privacy-policy.md`, `play-store/data-safety.md`).
- Voice: plain, short, sentence case, concrete ("Last played 47 days ago, 9 plays"); plain ASCII punctuation where practical.
- Guides and score sheets paraphrase rulebooks and draw their own icons; rulebook text and printed art are never copied.

## Evidence on Hand

- Store graphics and phone screenshots: `play-store/graphics/` (icon, feature graphic, `phone/01`-`08`).
- Content counts from the repo: 267 setup guides, score sheets for 60+ games.
- No user testimonials, ratings, download numbers or press exist; do not invent them.

## Product Principles

1. The table comes first: any change to logging is judged by how fast and how safely a play gets saved with friends waiting.
2. Never lose a play: save locally first, show the expected result at once, and sync in the background.
3. BGG stays right: the app mirrors BGG's model (plays, expansions, collection flags) rather than inventing its own, and never writes something BGG would misread.
4. Content is data and shared: guides and score sheets are open, reviewable JSON, improved by users, checked by tests.
5. AI assists, the user confirms: scans, drafts and recognition are suggestions until the user accepts or reviews them.

## Accessibility & Inclusion

Readable in dim game-night rooms: keep contrast high on the dark surfaces and every tappable element at least 48dp. No formal standard (WCAG / TalkBack audit) is committed to yet.
