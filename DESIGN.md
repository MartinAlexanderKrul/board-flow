---
name: BoardFlow
description: A board game play journal for Android, dark graphite with one amber voice.
colors:
  meeple-amber: "#FEB316"
  amber-deep: "#C98C00"
  amber-pale: "#FFDFA0"
  amber-graphite: "#252420"
  amber-graphite-deep: "#201E18"
  graphite-night: "#0E0E0F"
  graphite: "#1C1C1E"
  graphite-raised: "#2A2A2C"
  graphite-edge: "#3D3D3E"
  graphite-hairline: "#2E2E2F"
  chalk: "#F0F0F0"
  ash: "#9E9E9E"
  ink: "#131314"
  alarm-red: "#FF5252"
  alarm-red-container: "#4D1010"
  alarm-red-pale: "#FFB4AB"
  victory-green: "#5BC98A"
  victory-green-container: "#16301F"
  paused-orange: "#FF9F43"
  paused-orange-container: "#3A2610"
  note-blue: "#7FB4E6"
  note-blue-container: "#14283A"
typography:
  headline:
    fontFamily: "Roboto, sans-serif"
    fontSize: "24px"
    fontWeight: 400
    lineHeight: "32px"
  title-large:
    fontFamily: "Roboto, sans-serif"
    fontSize: "22px"
    fontWeight: 400
    lineHeight: "28px"
  title:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16px"
    fontWeight: 500
    lineHeight: "24px"
    letterSpacing: "0.15px"
  body-large:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: "24px"
    letterSpacing: "0.5px"
  body:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
    letterSpacing: "0.25px"
  body-small:
    fontFamily: "Roboto, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: "16px"
    letterSpacing: "0.4px"
  label:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14px"
    fontWeight: 500
    lineHeight: "20px"
    letterSpacing: "0.1px"
rounded:
  tiny: "2px"
  small: "4px"
  cover: "8px"
  control: "12px"
  card: "16px"
  sheet: "24px"
  pill: "9999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  xxl: "32px"
components:
  button-primary:
    backgroundColor: "{colors.meeple-amber}"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "8px 18px"
    height: "40px"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.meeple-amber}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "8px 18px"
    height: "40px"
  button-secondary-compact:
    backgroundColor: "transparent"
    textColor: "{colors.meeple-amber}"
    rounded: "{rounded.pill}"
    padding: "4px 10px"
    height: "32px"
  button-destructive:
    backgroundColor: "transparent"
    textColor: "{colors.alarm-red}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "8px 18px"
    height: "40px"
  button-cancel:
    backgroundColor: "transparent"
    textColor: "{colors.chalk}"
    typography: "{typography.label}"
  card:
    backgroundColor: "{colors.graphite}"
    textColor: "{colors.chalk}"
    rounded: "{rounded.card}"
    padding: "16px"
  form-group:
    backgroundColor: "{colors.graphite}"
    rounded: "{rounded.card}"
  form-row:
    textColor: "{colors.chalk}"
    height: "56px"
  setting-row:
    textColor: "{colors.chalk}"
    height: "64px"
  text-field:
    backgroundColor: "{colors.graphite-raised}"
    textColor: "{colors.chalk}"
    rounded: "{rounded.control}"
    height: "48px"
  game-list-row:
    backgroundColor: "{colors.graphite}"
    rounded: "{rounded.control}"
    height: "64px"
  info-pill:
    backgroundColor: "{colors.graphite-edge}"
    textColor: "{colors.chalk}"
    rounded: "{rounded.pill}"
    padding: "6px 10px"
  mood-chip:
    backgroundColor: "{colors.note-blue-container}"
    textColor: "{colors.note-blue}"
    rounded: "{rounded.pill}"
    padding: "6px 12px"
  snackbar:
    backgroundColor: "{colors.graphite-edge}"
    textColor: "{colors.chalk}"
    rounded: "{rounded.control}"
  bottom-sheet:
    backgroundColor: "{colors.graphite}"
    rounded: "{rounded.sheet}"
  navigation-bar:
    backgroundColor: "{colors.graphite-night}"
    textColor: "{colors.ash}"
  navigation-bar-selected:
    textColor: "{colors.meeple-amber}"
---

# Design System: BoardFlow

## Overview

**Creative North Star: "The Game Night Ledger"**

BoardFlow is a record book kept at the table. Its job is to hold plays, scores, winners and shelves in tidy, dense rows that read in a dim room at a glance, and then get out of the way. The surfaces are near-black graphite in three tonal steps, the type is the Material 3 default scale in Roboto, and a single amber, Meeple Amber, marks what matters: the game's name, the key number, the winner, the one action that moves the task forward.

Density is part of the identity. The type scale and control sizes stay at Material defaults; a larger, heavier scale was tried in a redesign and made every screen feel oversized, so it was dropped. Rows group onto tonal cards instead of standing in outlined boxes, and surfaces separate by tone, not by borders. The only colour that is not the app's own is the games' box art, which appears as covers and as a darkened header behind a game's name.

Controls are compact and quiet: pills that hug their labels, one solid amber pill per view, grey for everything that is merely tappable. The app has one theme, dark, with no light mode and no theme setting.

**Key Characteristics:**
- Dark graphite in three tonal steps (night, card, raised), each about 1.2:1 against the one below
- One accent, Meeple Amber, used for emphasis and the primary action, never for chrome
- Material 3 default type and density, tabular numbers for scores
- Grouped form rows on tonal cards; no outlines around fields
- Compact pill buttons that never stretch to full width
- Box art as the only imagery, always darkened under text

## Colors

A cool near-black graphite ladder with one warm amber voice and a small set of status colours that each carry one meaning.

### Primary
- **Meeple Amber** (#FEB316): game names and titles, key numbers (play counts, scores), the winner's trophy, the selected tab and nav item, text actions, focused field labels, and the fill of the one primary button per view. Text on it is Ink.
- **Deep Amber** (#C98C00): the tertiary and inverse-primary role; a darker amber for the rare place a second amber step is needed.
- **Pale Amber** (#FFDFA0): text and icons on the amber-graphite containers.
- **Amber Graphite** (#252420) and **Deep Amber Graphite** (#201E18): warm graphite containers for the primary, secondary and tertiary container roles. They stay graphite so amber surfaces never turn brown.

### Neutral
- **Graphite Night** (#0E0E0F): the page background, the top bar, the tab row and the bottom navigation bar.
- **Graphite** (#1C1C1E): cards, form groups, list rows, bottom sheets and dialogs.
- **Raised Graphite** (#2A2A2C): text field fills, raised form groups, the score box in player rows, filled cells inside a card.
- **Graphite Edge** (#3D3D3E): the outline role, snackbars and info pills (the highest container step).
- **Graphite Hairline** (#2E2E2F): dividers between rows inside a form group.
- **Chalk** (#F0F0F0): primary text, values, and the Cancel action.
- **Ash** (#9E9E9E): labels, supporting lines, placeholders, chevrons, overflow and form-row icons.
- **Ink** (#131314): text and icons on Meeple Amber and on the status colours.

### Status
- **Alarm Red** (#FF5252) on **Alarm Red Container** (#4D1010), with **Pale Alarm Red** (#FFB4AB) text: destructive actions, errors, missed challenges.
- **Victory Green** (#5BC98A) on #16301F: done, completed challenges, success.
- **Paused Orange** (#FF9F43) on #3A2610: paused or warning state.
- **Note Blue** (#7FB4E6) on #14283A: informational states and mood chips.

Two palettes sit outside the system on purpose: player colours (`PlayerColors`: the BGG colour names a play records, plus eight automatic avatar colours, with Ink initials on light fills) and the stats insight rarity tiers (teal, blue, purple, gold gradients), which are used only on insight cards so they read as rewards, not UI state.

### Named Rules
**The One Voice Rule.** Meeple Amber is for emphasis, not for everything tappable. Titles, key numbers, the selected state, text actions and the primary pill carry it; chevrons, expanders, share and overflow icons, form-row icons and values are Ash or Chalk. The single exception is the leading icon of a settings row, which is amber (red when destructive).

**The No Brown Rule.** Never composite amber over a surface at partial opacity to make a tint; it turns brown. Use the amber-graphite containers, or amber at 12-16% only for a selected chip, list item or nav indicator.

**The Red Means Loss Rule.** Alarm Red appears only where data can be lost or something failed. It is never a decorative accent.

## Typography

**Display Font:** Roboto (Android system default, through Material 3)
**Body Font:** Roboto
**Label/Mono Font:** Roboto, with tabular figures (`tnum`) for scores and counts

**Character:** One family at the stock Material 3 scale. Hierarchy comes from weight, colour (Chalk against Ash, amber for the name or number that matters) and position, not from size jumps.

### Hierarchy
- **Headline** (400, 24px, 32px line): form and editor headlines.
- **Title Large** (400, 22px, 28px line): the top bar's destination title and game names on art headers.
- **Title** (500, 16px, 24px line, 0.15px tracking): section titles over form groups and cards, empty-state titles.
- **Body Large** (400, 16px, 24px line): form-row values and the main line of list rows.
- **Body** (400, 14px, 20px line): general text, row labels.
- **Body Small** (400, 12px, 16px line): supporting lines under rows, setting details, dates under titles.
- **Label** (500, 14px, 20px line, 0.1px tracking): button and tab labels, sentence case.

### Named Rules
**The Stock Scale Rule.** Do not enlarge type or controls beyond Material defaults. The original density is part of the look.

**The Sentence Case Rule.** Every label, title and button is sentence case ("Refresh collection"), never title case or all caps.

## Layout

A single-column phone layout on a 4dp grid. Screens have 16px horizontal padding; cards pad 16px inside; related items sit 8px apart, sections within a card 12px, major sections 24px, and the space before a primary call to action or at the bottom of a modal is 32px.

The top bar names the destination (Log Play, Journal, Collection, Settings) and gains a back arrow on pushed screens; a `ScreenTabRow` under it names the sub-location, and every tabbed screen also switches tabs on a horizontal swipe. The bottom navigation bar has four destinations and hides on pushed screens (Quick Setup, the guide editor, the score sheet). Log Play hides its tabs, search and "Playing" row while the list is dragged down and returns them on the way up.

Forms end with an action bar at the bottom right: white Cancel text, then the amber pill. Long lists (over 20 items) get a draggable fast-scroll thumb with a letter bubble. Every tappable element is at least 48dp in both axes; visible buttons are 40dp inside that touch area.

Motion is short and functional: 120ms press scale to 97%, 140ms quick fades, 180ms visibility and resize, 200ms dialogs (from 92% scale), and a 260ms slide-plus-fade between tabs and screens, all on the standard fast-out-slow-in curve.

## Elevation & Depth

The system is flat. Depth comes from tonal layering only: Graphite Night page, Graphite cards, Raised Graphite fields and inner cells, Graphite Edge for floating bits like snackbars. Each step is about 1.2:1 against the one below, so cards read without borders or shadows. Dialogs and sheets sit on the same Graphite as cards over a black scrim.

On game art and the camera, controls become translucent black (50%) with white labels, so they stay a step darker than whatever is underneath; the secondary-button container on cards is black at 34% for the same reason.

### Named Rules
**The Tone Not Line Rule.** Surfaces separate by tone. Do not add outlines around cards, form groups or session and chronicle cards; the hairline divider exists only between rows inside one group.

## Shapes

Three radii and a pill. Controls (fields, chips, list rows, snackbars, small banners) are gently rounded at 12px; cards, form groups and banners at 16px; dialogs and bottom sheets at 24px (top corners only for sheets). Buttons, avatars, badges, info pills and status pills are full pills. Game covers use 8px, loading marks 4px, and heatmap cells 2px. Screens never write their own radius; they use the shared shapes.

## Components

### Buttons
Compact pills that hug their label, never full width; a wide slab reads as a banner, not as something to press. All press to 97% scale.
- **Shape:** full pill.
- **Primary:** solid Meeple Amber with Ink label, 40px tall, 8px by 18px padding. One per view: the action that moves the task forward (Save, Log play, Start game, Play again). The game detail header's Log play / Setup / History trio is an agreed exception.
- **Secondary:** transparent fill, amber label and a 1px amber outline at 60%, same 40px height; for alternatives beside the primary (Scan, Add player, Retake). A compact 32px variant with 4px by 10px padding serves rows of links (BGG, Rules, Drive). On art or the camera the outline is dropped and the pill is translucent black.
- **Cancel:** Chalk text, no container, left of the save pill.
- **Destructive:** transparent with Alarm Red label and a red outline at 55%. Inside a confirmation dialog the action is a solid red pill beside white Cancel.
- **Edit:** a main edit action is an amber pen icon next to a red delete icon on the left of the action row; a minor edit is amber "Edit" text.

### Chips
- **Filter chips:** Raised Graphite, Chalk label; selected is amber at 14% with an amber label.
- **Related games and expansions:** outlined pills in one scrolling row under the play details.
- **Mood chips:** Note Blue on its container, full pill.
- **Info pills:** Graphite Edge (or black 50% on art), 14px icon, Chalk label.

### Cards / Containers
- **Corner Style:** 16px.
- **Background:** Graphite; the winner row is translucent amber with no outline; session and chronicle cards are white at 10% with no outline.
- **Shadow Strategy:** none; see Elevation & Depth.
- **Border:** none, except an optional amber outline at 28% for an accented section card.
- **Internal Padding:** 16px.
- **Display screens** (Challenges, Stats, game detail, Sleeves, Quick Setup) keep their content on plain tonal cards with no tint; on one card only the key number, a status badge and real actions carry colour.

### Inputs / Fields
- **Form groups:** a Graphite card of rows, each 56px tall with an Ash icon, a label in an 88px column, and a Chalk value or Ash placeholder; hairline dividers between rows. Inside a group, text entry is an inline field with no box.
- **Standalone text field:** Raised Graphite fill, 12px radius, 48px, label inside, no indicator line.
- **Focus:** the label and trailing icon turn amber; the fill does not change.
- **Pickers and dates:** every date opens the shared date picker; durations read "45 min" / "1 h 20 min"; dates read "Oct 1, 2026".
- **Switches:** the shared switch with a stronger off state than Material's default, which is too faint on dark cards.

### Navigation
- **Top bar:** Graphite Night, Title Large destination, back arrow on pushed screens, a play-timer pill while a timer runs.
- **Tab row:** Graphite Night, amber selected label and indicator, Ash unselected, one label size for every tab row (fitted so the widest label fits a quarter of the screen).
- **Bottom bar:** Material navigation bar on Graphite Night, four destinations; selected icon and label amber over a 16% amber indicator, unselected Ash.

### Settings rows
Settings and Sync are lists of sections: a Title over one form group of 64px rows, each with an amber leading icon, a Chalk title, an Ash detail line, and a trailing value, switch or chevron. Every action is a row, not a button; clear and delete rows are red.

### Game art header
Log Play, the score sheet and the game detail dialog open with the game's box art darkened behind a cover thumbnail (8px radius) and the game's name in Chalk or amber. Controls on it are black-50% pills with white labels.

### Player row
A Graphite card with a coloured initials avatar (the player's colour, or one picked from the name), the name in Chalk, a trophy toggle (amber when the winner, Ash otherwise), the score in a Raised Graphite box with tabular figures (amber for the winner), and a grey expand chevron.

## Do's and Don'ts

### Do:
- **Do** build every screen from the shared kit (`BoardFlowCard`, `BoardFlowFormGroup` / `BoardFlowFormRow`, `BoardFlowSettingRow`, `GameListRow`, `BoardFlowTextField`, the buttons in `BoardFlowUi.kt`) and the tokens in `ui/theme/`.
- **Do** keep one solid amber pill per view and make it the action that moves the task forward.
- **Do** separate surfaces by tone: Graphite Night (#0E0E0F), Graphite (#1C1C1E), Raised Graphite (#2A2A2C).
- **Do** keep every tappable element at least 48dp and two neighbouring controls the same height and label size.
- **Do** confirm every create, update and delete with a snackbar, and offer Undo for reversible local deletes.
- **Do** use tabular figures for scores and counts.

### Don't:
- **Don't** stretch buttons with full width.
- **Don't** colour chevrons, expanders, share or overflow icons amber; they are Ash.
- **Don't** composite amber over a surface to tint it; it turns brown.
- **Don't** put outlines around fields, cards, session or chronicle cards.
- **Don't** enlarge type or controls beyond the Material 3 defaults.
- **Don't** write raw radii, colours or spacing in a screen; use `BoardFlowShape`, `BoardFlowColors`, `Spacing` and `Dimens`.
- **Don't** use red for anything but data loss and errors.
- **Don't** add a light theme or a per-screen colour scheme.
