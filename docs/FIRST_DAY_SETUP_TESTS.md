# RG Rotate first-day setup and complete backup • v0.1.8

This file distinguishes **automated evidence** from *manual real-device testing*. GitHub Actions cannot establish that a retail RG Rotate has a particular voice pack, microphone service, PPSSPP audio route or swivel behavior.

## First-run guided experience
- [ ] A "Start Here" card is clearly visible or reachable on the 720 × 720 dashboard; layout scales at Android 100%, 150% and 200% font settings.
- [ ] Setup Guide shows actual device screen physical pixels, usable dp, Android release and manufacturer/model, without exposing private identifiers.
- [ ] microSD not configured: NEEDS ACTION; no fallback to app-internal storage. User selects SAF removable subfolder on main screen.
- [ ] microSD configured: read status displays saved notes and profile counts. Write/read/delete remains an explicit button. A failed test never claims PASS and does not delete unrelated files.
- [ ] Overlay permission OFF: Android special-access settings route opens or manual instructions appear. GRANTED says permission only, not in-game proof.
- [ ] Voice setup opens Voice Lab; on-device ASR absence leaves text input available; never routes to cloud recognition.
- [ ] All four manual checks (touch, swivel, offline voice in airplane mode, PPSSPP+orb) default to unchecked, then show USER REPORTED PASS only when selected.
- [ ] Copy diagnostic report never contains note text, SD URI/path, game saves, microphone audio, screenshots, credential information, or private contents. Test with no SD and revoked SD.
- [ ] No automatic microphone/overlay permission request; Android permission settings still require user action.

## Complete backup / restore
- [ ] Empty data bundle writes a JSON file containing both empty arrays; SHA-256 digest verified on readback; no internal data fallback.
- [ ] Create 12 saved notes (including pinned) and 3 custom game names. Complete export readback matches all records, order, IDs, timestamps, pins and titles.
- [ ] Change a character in the JSON after export; checksum rejection must happen **before** any mutation, and both current stores remain unchanged.
- [ ] Replace checksum or import invalid version: reject without changing either dataset.
- [ ] Oversized notes, duplicate IDs, duplicate/built-in game names and too many records: reject without writing.
- [ ] Import preview shows note count/pin count/game count and SHA-256 prefix. Cancel at file picker, first dialog, or second dialog: no content changes.
- [ ] Confirm restore: emergency complete export of current state is created first, then both snapshot families are written and read back with matching checksum.
- [ ] Simulate SD unplug/low space during one of the two writes. If rollback completes, verify old content. If rollback cannot complete, show recovery filename; do not claim success.
- [ ] Legacy notes-only exports can still be inspected/restored using dedicated options; complete import rejects notes-only files with friendly unsupported-format message.
- [ ] Warn clearly that backup is **plaintext JSON** on the same microSD, is not encrypted, does not include app preferences or emulator saves, and needs an external second copy for disaster protection.

## Automated vs physical gates
- [ ] JVM CombinedBackupRules checksum, tamper/dedup, range validation tests; original GameProfilePolicy, MemoryPolicy, TouchUi layout and voice tests all PASS.
- [ ] Android 12 / 720 × 720 emulator screenshot smoke test with a touch navigation into Voice Lab PASS. Record CI link and PNG evidence; emulator proof is not a retail hardware pass.
- [ ] Main-branch v0.1.8 debug APK build and artifact PASS; artifact should be available for user to sideload.
- [ ] Real RG Rotate testing with 512GB microSD, PPSSPP, touch and swivel, 720×720 fonts, read/write/remove/reinsert, battery/headphones: **NOT RUN** before the handheld arrives.

## Post-device priorities (plan, not implementation)
1. Diagnose actual voice choices and latency before locking in a free natural voice.
2. Polish screen layouts based on actual 720×720 screenshots; avoid GPU-heavy live blur.
3. Game-aware assistant requires opt-in, an explicit mechanism to identify the emulator/game, measured local performance and appropriately bounded memory.
4. Visual frame analysis only with foreground consent, temporary in-memory frames and strict no-delete policy for emulator files.
5. Real-time AI and quiet/coach/chatty modes remain future milestones, not claimed by Voice Lab.
