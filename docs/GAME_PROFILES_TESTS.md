# Custom microSD Game Profiles • v0.1.7

## What is implemented
- Default profiles: General, Midnight Club 3: DUB Edition, Midnight Club: LA Remix, and The 3rd Birthday.
- User-created titles (maximum 80, 1–64 characters, case-insensitive uniqueness) stored as two alternate 16 KiB-limited, revisioned JSON snapshots on the selected removable microSD, never in app private files.
- The dashboard game selector and Memory Library game filter combine defaults + custom titles + game titles preserved from previous note records. Voice Lab inherits the selected game.
- Deleting a custom profile requires confirmation **and no existing notes**. Deleting a name does not delete games, ROMs, screenshots, save states, or notes. No automatic game detection.
- The existing note-backup function is **notes-only**. The profile index is not included in the exported JSON yet.

## Automated checks
- [ ] Profile policy tests: duplicate, starter-title collision, bad names, max count, removal rules, recovery of historical game names.
- [ ] All earlier orb, storage, voice and memory JVM tests pass; APK builds on clean CI at v0.1.7/8.
- [ ] No INTERNET or global file-storage permission added.
- [ ] New activity is registered, Voice Lab unaffected, the main selector no longer uses a hardcoded positional game-name lookup.

## First-day handheld checks (NOT RUN)
- [ ] Fresh install with 512 GB removable microSD; select subfolder and write/read/delete test.
- [ ] Add 'God of War: Chains of Olympus'; verify it appears in dashboard and Memory Library, and in Voice Lab after selecting it.
- [ ] Save a pinned note to custom game, close/relaunch, read it, then try to remove its profile: removal must be blocked.
- [ ] Add an unused profile and remove after confirmation; no existing notes changed.
- [ ] Remove microSD, revoke permission, corrupt one profile snapshot, leave another valid: recover or fail closed without internal fallback.
- [ ] Upgrade from v0.1.6 with old snapshots and notes: preserve existing names, saved text and pins.
- [ ] Font size 100%/150%/200%, Android 720×720 screen, keyboard, swipe scrolling and gamepad: no clipped essential actions.
- [ ] Perform airplane-mode game/voice tests. No recording, emulation scanning or paid calls.

## Proposed future milestones (NOT IMPLEMENTED)
First-day guided hardware onboarding; full combined backup with index + memory data; per-game knowledge cards; temporary consent-based screenshot analysis; quiet/coach/chatty conversation modes; optional emulator game identity detection only after privacy and platform feasibility testing.

**Do not mark the physical-device gate complete from CI.** Android emulator evidence is still under investigation and does not prove a real RG Rotate touchscreen experience.
