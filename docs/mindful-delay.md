# Mindful Delay — Plan and Status

This document captures the scope, UX, data model, phased plan, and the current status of the Mindful Delay feature.

## Summary

- Per-app delays are configured in Settings only (simple, centralized).
- Home long-press on a slot shows a small menu with a Mindful Delay toggle (enable/disable) for that slot's app.
- Drawer search gets a rounded, padded search field.
- Drawer gains an A–Z fast scroll index on the right.
- Mindful overlay shows a clear countdown; "Open Now" becomes enabled after 2 seconds.

## Current Status

- Phase 1 complete: Settings → Mindful Delay → Manage Delayed Apps
  - Per-app delay storage and editing in Settings
  - “Add App” wired to app picker + stepper
- Phase 2 complete: Home long-press slot menu toggle (Enable/Disable; no seconds edit)
- Phase 3 complete: Apply per-app/global delay in launch pipeline (Home + Drawer)
  - Overlay shows countdown; “Open Now” unlocks after 2s

### Next Phases

- Phase 4: Drawer polish (rounded search + A–Z fast scroll)
- Phase 5: Overlay visual enhancements

## Usage

- Configure per-app delays at Settings → Mindful Delay → Manage Delayed Apps.
- If a per-app value is not set, the global default delay applies.
- A Home-screen toggle will be available in Phase 2.

## Scope

- Add Settings → Mindful Delay section:
  - Global default delay (seconds) editable.
  - Manage Delayed Apps: enable/disable apps for delay and edit each app's delay.
- Add Home slot long-press menu:
  - “Set/Replace App” (existing behavior)
  - “Mindful Delay: Enable/Disable” (no seconds editing here)
- Apply delay when launching from both Home and Drawer.
- Improve Drawer search field visuals (rounded box).
- Add A–Z fast scroll for the Drawer list.
- Improve Mindful overlay visuals; “Open Now” unlocks after 2 seconds.

Out of scope for this iteration:

- Editing per-app seconds from Home or Drawer (Settings only for editing seconds).

## Data Model

- `Prefs.mindfulDelayMs: Int` — global default (ms)
- `Prefs.mindfulDelayedApps: Set<String>` — packages with delay enabled
- Per-app overrides stored as encoded entries in a `Set<String>` preference: `"package|seconds"`
  - Helpers: `getPerAppDelaySeconds`, `setPerAppDelaySeconds`, `removePerAppDelay`

## UX Flows

### Home: Toggle Mindful Delay

1) Long-press a Home slot
2) Show a simple menu with:
   - Set/Replace App
   - Mindful Delay: [Enable]/[Disable]
3) If enabled, the app is added to the delayed list. Delay seconds are controlled in Settings.

ASCII — Home Slot Long-Press

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
- Manage Delayed Apps (enable/disable, edit per-app seconds)

ASCII — Settings

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
| (+) Add App + (picker)                           |
|                                                  |
| Back                                             |
+--------------------------------------------------+
```

ASCII — Edit Delay (Bottom Sheet)

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

ASCII — Drawer Header

```
+--------------------------------------------------+
|   🔍  [  Search apps...                     ]    |
|      (rounded box, padded, consistent height)    |
+--------------------------------------------------+
```

### Drawer: A–Z Fast Scroll

- Right-side alphabet index; touching/dragging jumps to the first item for that letter.
- Show a transient letter overlay while dragging.

ASCII — Drawer with Index

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

- App icon/name prominent, large countdown, circular progress, optional intention note.
- “Open Now” (disabled for first 2 seconds) and “Cancel”.
- Small “Edit delay” button navigates to the app's entry in Settings.

ASCII — Overlay

```
+--------------------------------------------------+
|                 Mindful Pause                    |
|  [App Icon]  Instagram                           |
|                                                  |
|  “What's your intention?”                        |
|  [ Start a note… ]                               |
|                                                  |
|                 00:05                            |
|        (big circular progress)                   |
|                                                  |
|  [ Open Now ] (enabled after 2s)   [ Cancel ]    |
|                                                  |
|  Delay: 5s   [ Edit ]                            |
+--------------------------------------------------+
```

## Acceptance Criteria

- Enabling from Home adds/removes the app to/from delayed list without changing its seconds value.
- Delay resolution: per-app seconds if set; else global default; else no delay.
- Delay applies when launching from Home and Drawer.
- Drawer search visually wrapped in a rounded box.
- A–Z index scrolls/jumps correctly and shows a letter overlay while active.
- Overlay shows countdown; “Open Now” unlocks after 2s; “Cancel” aborts.

## Implementation Phases

1) Settings — Global default + Manage Delayed Apps (list + edit per-app seconds) [COMPLETED]
2) Home — Long-press menu with Mindful Delay toggle (no seconds edit) [COMPLETED]
3) Manage screen — “Add App” wired to app picker + stepper [COMPLETED]
4) Apply per-app/global delay in the launch pipeline (Home + Drawer) [COMPLETED]
5) Drawer UI — Rounded search field + A–Z fast scroll [PENDING]
6) Overlay UI — visual enhancements [PENDING]

## Next Up (Focus)

- Drawer rounded search styling to improve clarity and contrast.
- Drawer A–Z fast scroll index with transient letter overlay while dragging.

## Notes & Risks

- Keep Settings the single place to edit seconds to minimize UI complexity.
- Ensure renamed apps are indexed under their display label for A–Z.
- Multi-profile support: index and launch must include user handle.
