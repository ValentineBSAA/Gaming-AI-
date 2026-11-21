# Gaming AI v0.2.0 pre-hardware release candidate: one-install readiness gate

**Target:** RG Rotate / 720 × 720 physical touchscreen / Android 12 / user-selected 512 GB removable microSD.

**Goal:** maximize practical readiness before delivery without pretending emulator or CI can test an unseen retail device. Do NOT generate a new APK link to the user until this release branch has passed all automatic tests and the exact main-branch artifact is verified.

## Included before first install

| Subsystem | Candidate implementation | Evidence required |
|---|---|---|
| Premium square interface | Compact midnight glass, fast quick tiles, scrollable screens, touch feedback, reduced motion | 720 square emulator at density 320/font 130%, density 400/font 150% and 200%; hands-on later |
| Gamepad | B back, L1/R1 page in app only | Inject synthetic Android key codes in emulator; physical mapping later |
| Floating companion | Grayscale 3-layer orb, custom colors, drag/hide/status, position reset and resize clamp | PNG checks + Android compile + permission test; PPSSPP/screen swivel later |
| SD storage | SAF removable SD subfolder; checksummed two-slot memory and profile snapshots; strict no-internal fallback | Pure rule tests, CI, readback checks, real SD unplug test later |
| Profiles | General + starter game titles, 80 custom profiles, note-title recovery | Dynamic selector & filtering regression, real SD later |
| Voice | Installed English offline voices, audition, favorites, pitch/speed/mute, on-device-only optional PTT | No INTERNET permission, Voice Lab launch, air-gapped sound check on hardware |
| Backups | Legacy notes-only and COMPLETE profiles+notes export/restore with SHA-256, preview and emergency prior-state export | Rule tests/CI; real interrupted-write/recovery trial later |
| First-day setup | System read checks, optional SD write/read/delete, overlay permission help, persistent manual checks, redacted diagnostic report | Launch in emulator; manual hardware checks remain NOT TESTED |
| History safety | Compaction requires preview + confirmation and verified recovery backup | Code review + unit tests; SD round trip on device |
| Quiet background | No live mic, no screen capture, no in-game overlay audio, no live GPU blur | Manifest/static policy audit; PPSSPP performance on hardware |

## Release gate before providing an APK

- [ ] G0: Exact source SHA and version 0.2.0 / versionCode 11 recorded. Main-branch CI `testDebugUnitTest`, `assembleDebug`, grayscale PNG and privacy preflight all **SUCCESS**.
- [ ] G1: 720 × 720 Android 12 emulator test PASS: dashboard, Voice Lab, Memory Library, Game Profiles and First-day Setup by actual taps; capture screenshots and UI trees, test font scaling at 130/150/200%.
- [ ] G2: Synthetic Android KEYCODE_BUTTON_B navigation from Memory Library to dashboard and L1/R1 page calls PASS. Physical controller mapping remains unknown.
- [ ] G3: Verify APK artifact exists and is not expired. Document filename, commit SHA and actual APK SHA-256 when available, rather than artifact-ZIP digest. Debug-signed sideload only, no store release promise.
- [ ] G4: Read code/README for false claims, accidental INTERNET or broad-file permission, hardcoded paid keys, automatic screenshot saving, voice auto-enrollment. Keep disabled/incomplete features clearly identified.
- [ ] G5: No need for another feature iteration before physical receipt unless this gate finds a blocking defect.

## One-pass on-device acceptance sequence (aim: a single coherent report)

1. Install candidate APK (only after G0–G5). Open First-day Setup and note actual panel px/dp, Android version and firmware behavior. Test touch scrolling at regular/large fonts and swivel open/close. These cannot be pre-certified.
2. Select **a subfolder** under the real removable microSD (not root). Run write/read/delete test, save a temporary General note, force-close/reopen, confirm it remains. Never select internal or cloud drive. If rejected, report authority/document ID category via a redacted diagnostic, not the full path.
3. Add a custom game profile, save a pinned note, search/edit/pin/unpin in Memory Library. Remove an empty custom profile, but the app must refuse removing any game with notes.
4. Create COMPLETE backup, inspect count/checksum, change a noncritical memory, restore approved backup and verify recovery. Also keep a copy **off-card**. Do not simulate catastrophic SD corruption with your only real user data; use a separate test folder/card.
5. Preview offline TTS voices in **Airplane mode**. Test favorites, rate/pitch, mute/stop and optional on-device PTT. Record chosen voice and whether local recognizer exists. If voice absent, keep text fallback; no paid speech silently.
6. Enable orb with explicit Android permission, drag/color/reset/hide it; then launch PPSSPP. Check whether orb steals taps, game input, sound, FPS or battery. Test swivel with orb visible, restart and repeat.
7. Try B/L1/R1 in Gaming AI and verify they don't interfere with gameplay while PPSSPP is foreground. Check Bluetooth/audio route and touchscreen.
8. Use the First-day Setup **Copy diagnostic report**, review for privacy and send the report with any screenshots of the *app UI* you choose. It excludes notes, URI and microphone recordings. Mark each test individually PASS / NEEDS ACTION / NOT TESTED.

## Hardware blockers left open, by design

- Natural live AI conversation, a free local natural neural voice, game understanding and background spoken companionship **are not present**. Voice Lab still returns deliberately scripted responses.
- Persistent screenshots and auto-delete of emulator files are not implemented. Future vision must use opt-in transient RAM frames with no destructive access to PPSSPP, and be tested for thermals/battery.
- Exact firmware gamepad key codes, physical display usable dp, TTS provider and mic behavior, swivel changes, direct Android Settings shortcuts and PPSSPP overlay performance cannot be inferred from generic emulator tests.
- Storing *all* APK/settings/voice-model bytes on microSD is impossible on stock Android. **Only Gaming AI memory/profile/exports** use user-selected SD; Android retains app binaries, preferences and permission grants internally.

## Release discipline

One comprehensive candidate instead of many small downloads, but not a guarantee that the first physical install needs zero fixes. If a hardware issue appears, diagnose with the first-day report and batch all reproducible defects into one post-device repair release. Never label unrun physical tests as green.
