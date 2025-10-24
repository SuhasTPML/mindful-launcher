# Mindful Delay – Current Status

- Phase 1 complete: Settings → Mindful Delay → Manage Delayed Apps
  - Per-app delay storage and editing in Settings
  - Launch resolution uses per-app seconds if set; else global default
  - Applies when launching from Home and Drawer (overlay shows countdown; Open Now unlocks after 2s)

- Next phases
  - Phase 2: Home long-press slot menu with Mindful Delay Enable/Disable (no seconds edit)
  - Phase 3: Manage screen “Add App” wired to app picker
  - Phase 4: Drawer polish (rounded search + A–Z fast scroll)
  - Phase 5: Overlay visual enhancements

## Next Phase Details (Drawer Enhancements)

- Add transient letter overlay while dragging A–Z index
  - Centered overlay with current letter, large text, themed colors (colorSurface/colorOnSurface)
  - Updates as you drag; hides 500–800 ms after last movement
  - Haptic feedback on letter change (if available)
  - Fallback “#” for non‑Latin/unknown initials
- Improve index rail touch handling
  - Handle ACTION_DOWN/MOVE/UP; capture and prevent accidental list scroll during drag
  - Slight scale/alpha effect on the active letter in the rail (optional)
- Accessibility
  - Content description updates for TalkBack (“Jump to letter G”)
  - Ensure focus is not stolen; list remains scrollable outside the rail

Acceptance
- Overlay appears on drag, updates smoothly, hides after delay
- Jump positions correctly even with renamed labels and multi‑profile
- Light/dark theme contrast is readable

- Usage
  - Configure per-app delays at Settings → Mindful Delay → Manage Delayed Apps
  - If a per-app value is not set, the global default delay applies
  - Toggle from Home will be added in Phase 2
