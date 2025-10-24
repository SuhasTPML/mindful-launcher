# Mindful Delay – Implementation Plan

This document captures the agreed scope and UI/UX for adding configurable per‑app delays, a Home long‑press toggle, visual improvements in the app drawer, and a refined mindful overlay.

## Summary

- Per‑app delays are configured in Settings only (simple, centralized).
- Home long‑press on a slot shows a small menu that includes a Mindful Delay toggle (enable/disable) for that slot’s app.
- Drawer search gets a rounded, padded search field.
- Drawer gains an A–Z fast scroll index on the right.
- Mindful overlay gets a clearer countdown UI with optional intention note.

## Status (Live)

- [x] Phase 1 — Settings: Manage Delayed Apps implemented (per-app delay edit + disable, launch resolution uses per-app > global).
- [x] Home bottom sheet: Mindful Delay toggle (Enable/Disable) with theme-aware colors; disabled when slot has no app.
- [x] Drawer polish: rounded search container and A–Z index added.
- [ ] Transient letter overlay while dragging A–Z.
- [ ] Overlay visual enhancements (icon, circular progress, intention note, Edit link).

## Next Phase Details (Drawer Enhancements)

- Add transient letter overlay while dragging A–Z index
  - Centered overlay with current letter, large text, themed colors (colorSurface/colorOnSurface)
  - Updates as you drag; hides 500–800 ms after last movement
  - Haptic feedback on letter change (if available)
  - Fallback '#' for non‑Latin/unknown initials
- Improve index rail touch handling
  - Handle ACTION_DOWN/MOVE/UP; capture and prevent accidental list scroll during drag
  - Slight scale/alpha effect on the active letter in the rail (optional)
- Accessibility
  - Content description updates for TalkBack ('Jump to letter G')
  - Ensure focus is not stolen; list remains scrollable outside the rail

Acceptance
- Overlay appears on drag, updates smoothly, hides after delay
- Jump positions correctly even with renamed labels and multi‑profile
- Light/dark theme contrast is readable

## Scope

- Add Settings → Mindful Delay section:
  - Global default delay (seconds) editable.
  - Manage Delayed Apps: enable/disable apps for delay and edit each app’s delay.
- Add Home slot long‑press menu:
  - “Set/Replace App” (existing behavior)
  - “Mindful Delay: Enable/Disable” (no seconds editing here)
- Apply delay when launching from both Home and Drawer.
- Improve Drawer search field visuals (rounded box).
- Add A–Z fast scroll for the Drawer list.
- Improve Mindful overlay visuals; “Open Now” unlocks after 2 seconds.

Out of scope for this iteration:
- Per‑app delay editing from Home or Drawer (Settings only for editing seconds).

## Data Model

- `Prefs.mindfulDelayMs: Int` – global default (ms)
- `Prefs.mindfulDelayedApps: Set<String>` – packages with delay enabled
- `Prefs.mindfulDelayPerAppSeconds: Map<String, Int>` – per‑app overrides in seconds (fallback to global)

## UX Flows

### Home: Toggle Mindful Delay

1) Long‑press a home slot
2) Show a simple menu (see wireframe) with:
   - Set/Replace App
   - Mindful Delay: Enable/Disable
3) If enabled, the app is added to the delayed list. Delay seconds are controlled in Settings.

ASCII – Home Slot Long‑Press

```
+-------------------------------------------+
| Edit Home App (Slot #N)                   |
| ----------------------------------------  |
| • Set/Replace App                         |
| • Mindful Delay: [Enable]/[Disable]       |
|                                           |
| [Close]                                   |
+-------------------------------------------+
```

### Settings: Mindful Delay

- Global default (e.g., 5s)
- Manage Delayed Apps (enable/disable, edit per‑app seconds)

ASCII – Settings

```
+--------------------------------------------------+
| Mindful Delay                                    |
| Global Default: 5s   [ Edit ]                    |
|                                                  |
| Manage Delayed Apps                              |
| ------------------------------------------------ |
| [x] Instagram         Delay: ( 5s )  [ Edit ]    |
| [x] YouTube           Delay: ( 8s )  [ Edit ]    |
| [ ] Reddit            Delay: ( — )   [ Enable ]  |
|                                                  |
| (+) Add App → (picker)                           |
|                                                  |
| Back                                             |
+--------------------------------------------------+
```

ASCII – Edit Delay (Bottom Sheet)

```
+------------------------------------+
| Edit Delay for Instagram           |
|                                    |
| Delay (seconds)                    |
| [ 0 ]  [ 3 ]  [ 5 ]  [ 8 ]  [ 10 ] |
| (or stepper:  [-]   5   [+] )      |
|                                    |
| [ Save ]                 [ Cancel ]|
+------------------------------------+
```

### Drawer: Search Field

Rounded, padded search bar at the top of the drawer for clarity.

ASCII – Drawer Header

```
+--------------------------------------------------+
|  ⌕  [  Search apps...                     ]      |
|      (rounded box, padded, consistent height)    |
+--------------------------------------------------+
```

### Drawer: A–Z Fast Scroll

- Right‑side alphabet index; touching/dragging jumps to the first item for that letter.
- Show a transient letter overlay while dragging.

ASCII – Drawer with Index

```
+-----------------------------------------+--+
| A                                       |A |
| Audible                                 |B |
| Authenticator                           |C |
| ...                                     |D |
| Gmail                                   |M |
| Google Calendar                         |N |
| ...                                     |Z |
+-----------------------------------------+--+
```

### Mindful Overlay (Improved)

- App icon/name prominent, large countdown, circular progress, optional intention note input.
- “Open Now” (disabled for first 2 seconds) and “Cancel”.
- Small “Edit delay” button navigates to app’s entry in Settings.

ASCII – Overlay

```
+--------------------------------------------------+
|            Mindful Pause                         |
|  [App Icon]  Instagram                           |
|                                                  |
|  “What’s your intention?”                        |
|  [ Start a note… ]                               |
|                                                  |
|             00:05                                |
|        (big circular progress)                   |
|                                                  |
|  [ Open Now ] (enabled after 2s)   [ Cancel ]    |
|                                                  |
|  Delay: 5s   [ Edit ]                            |
+--------------------------------------------------+
```

## Acceptance Criteria

- Enabling from Home adds/removes the app to/from delayed list without changing its seconds value.
- Delay resolution: per‑app seconds if set; else global default; else no delay.
- Delay applies when launching from Home and Drawer.
- Drawer search visually wrapped in a rounded box.
- A–Z index scrolls/jumps correctly and shows a letter overlay while active.
- Overlay shows countdown; “Open Now” unlocks after 2s; “Cancel” aborts.

## Implementation Phases

1) Settings – Global default + Manage Delayed Apps (list + edit per‑app seconds)
2) Home – Long‑press menu with Mindful Delay toggle (no seconds edit)
3) Apply per‑app delay in the launch pipeline (Home + Drawer)
4) Drawer UI – Rounded search field + A–Z fast scroll
5) Overlay UI – visual enhancements and “Open Now” gating

## Notes & Risks

- Keep Settings the single place to edit seconds to minimize UI complexity.
- Ensure renamed apps are indexed under their display label for A–Z.
- Multi‑profile support: index and launch must include user handle.
