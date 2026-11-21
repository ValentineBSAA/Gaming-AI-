# RG Rotate Gaming AI: delivery and test roadmap

**Status:** planning / first prototype implementation. **Target:** Anbernic RG Rotate, Android 12, square touchscreen, 512 GB removable microSD. **Repo:** this repository only. **Core rule:** a phase is not DONE unless its acceptance tests have evidence; a GitHub commit is not proof of a working APK.

## Non-negotiable product rules

1. One consistent companion personality across games, friendly and responsive but never an incessant commentator.
2. No Piper, Whisper→LLM→Piper chains, or mandatory complex local inference stack. Live conversation will use a real-time speech-capable provider after cost/privacy review; **no secrets in APK or repo**.
3. Long-term user memory lives in a selected folder on the **removable microSD**, not an internal-storage fallback. Android may still retain the installed APK, preferences including a folder permission URI, OS cache and temporary process data internally. The absolute claim that *all* app bytes live on SD is not feasible.
4. Grant access through Android Storage Access Framework. Android 11+ can restrict choosing the SD volume **root**; select a dedicated *subfolder*. Stop with an error when the card is removed, the grant is revoked or a write fails.
5. Screen capture is **off by default**, requires explicit Android consent, and must not silently save images. Process transient frames in RAM, discard them after extraction, and do not upload them without an explicit network-vision opt-in.
6. Memory is bounded. Exact recall of every session forever is **not** promised. Important/pinned notes must be protected from automatic compaction, and the user can inspect, correct, export and delete them.
7. Preserve game progress and emulator data: never modify PPSSPP or game files in the first releases. Don't claim to control emulators or read their internals without evidence.
8. First builds should work offline for the on-device memory shell. Speech AI/network providers are later, opt-in, and can cost money.

## Gate 0 — Repository and hardware baseline [ ]
Deliverables: buildable Android project, CI producing a debug APK, baseline README, documented package name and privacy constraints.
Acceptance:
- G0.1 GitHub Actions clean checkout builds **assembleDebug** and unit tests. Artifact has a valid .apk.
- G0.2 No tokens, passwords, recorded voice or user data in source control or CI artifacts.
- G0.3 No test labeled passed unless backed by run evidence.
- G0.4 RG Rotate hardware specs and Android version reconfirmed on retail unit.
**Blocked until physical device:** install, screen/layout, actual microSD card identity, thermals/battery, microphone, game interaction.

## Gate 1 — Offline SD memory shell (v0.1 prototype) [ ]
Deliverables: touch-friendly square-screen app; explicit Choose SD Folder; voice-dictated note *if system recognition service is present*; manual game-tagged notes; pinned memory; readback; compaction; SD round-trip test; no screenshot files.
Acceptance:
- G1.1 On a genuine 512 GB microSD, user chooses a **subfolder**; selected provider URI is persisted across app restarts/reboots.
- G1.2 Internal/emulated/cloud selections are rejected; user memories do not silently spill to internal storage if SD absent.
- G1.3 Create 100 notes tagged across three games; cold restart; all saved notes can be loaded with accurate tags and timestamps.
- G1.4 Repeat a note; compact; duplicate unpinned copies merge, and pinned notes remain.
- G1.5 Unmount SD; add note fails clearly, not silently; remount and verify previous notes.
- G1.6 Run SD self-test; temporary test document is removed even on failure paths where provider permits.
- G1.7 Rotate open/closed, screen on/off: no data loss or distorted important controls; actual swivel state behavior remains exploratory.
- G1.8 Force close during a write and restore from backup: test partial-write recovery before claiming resilience.
- G1.9 On device: baseline startup time, memory footprint, UI legibility, and battery measurement recorded.
**Prototype scope limit:** game detection, actual screenshots, always-on mic, AI reasoning and speaking back are **not** in v0.1.

## Gate 2 — Natural voice and presence (v0.2) [ ]
Deliverables: optional real-time cloud speech session, one persistent companion personality, push-to-talk and explicit stop/mute, foreground/background audio support without stealing game input.
Acceptance:
- G2.1 Ten 15-minute sessions across Wi-Fi and Bluetooth earbuds without lost access to game controls.
- G2.2 On-device mic+speaker and earbuds verified; no feedback loops. Test Bluetooth reconnects, phone/sleep interruptions.
- G2.3 Interrupt ("barge-in") speech, switch games, mute and disconnect in under a few taps; no stuck mic.
- G2.4 Measure 50 turn latencies (median / p95) and provider costs; set performance budget from actual baseline.
- G2.5 Leave game running for one hour; voice app does not cause unacceptable slowdown or thermal drain.
- G2.6 Permission revoked, no network, provider outage, revoked token: clear error; offline memory shell remains usable.
- G2.7 Credentials held by a secure token broker, never stored in GitHub/CI, ROM dumps, logs, or client APK.

## Gate 3 — Long-term learning (v0.3) [ ]
Deliverables: stable fact records, per-game profiles, session summaries, confidence/provenance, opt-in carryover, user-visible memory controls and backup.
Acceptance:
- G3.1 Ten game sessions remain distinguishable; summaries are accurate and not fabricated.
- G3.2 Correct a mistaken game memory; new responses follow correction.
- G3.3 User can pin, edit, export, delete individual memories and **wipe all** with confirmation.
- G3.4 Stress test 10,000 synthetic events; compact within configured size budget while honoring protected data; report information loss.
- G3.5 Export and restore on fresh installation; encrypted backup available before enabling sensitive notes.
- G3.6 Switch from Midnight Club 3 to The 3rd Birthday and back; companion personality consistent, game context isolated.

## Gate 4 — Opt-in visual game awareness (v0.4) [ ]
Deliverables: consent-driven Android MediaProjection foreground service, bounded frame rate, RAM-only frame processing, game screen interpretation with uncertainty.
Acceptance:
- G4.1 Explicit OS capture permission on every capture session as required; easy stop, privacy indicator.
- G4.2 During a 60-minute session no PNG/JPEG/raw capture residue in user SD folder, app cache or logs.
- G4.3 With provider network opt-out, frames never leave device.
- G4.4 30 labeled screenshots for each target game: The 3rd Birthday, Midnight Club 3 DUB Edition, Midnight Club LA Remix. Record precision/error on actual scene recognition; say “not sure” when appropriate.
- G4.5 Battery, thermals, frame rate and impact on PPSSPP measured; throttle/stop analysis on overheating or weak battery.
- G4.6 Test sensitive UI, notifications, payment/sign-in screens: pause/blank capture.
- G4.7 Verify screen-off, swivel close, task switching, SD removal and background mic behavior.

## Gate 5 — Companion playfulness / active game participation (v0.5+) [ ]
Deliverables: reaction timing, optional commentary, hints only when requested, safe per-game guides, light custom games with AI opponent if feasible.
Acceptance:
- G5.1 30-minute quiet mode: no unsolicited speech.
- G5.2 30-minute relaxed cruise: companion talks without drowning out game audio.
- G5.3 Spontaneous reactions grounded in observed state, not pretending to see a screen without consent.
- G5.4 No automatic emulator control or in-game player takeover without separately verified integration.

## Repeat-on-every-build regression matrix

- **Install/update:** fresh install, upgrade from previous data version, app cold start, no destructive schema change.
- **Storage:** SD mounted/unmounted/reinserted, revoked URI, full/read-only SD, low space, corrupt/partial file, 100/10,000 notes, round-trip, backup/restore.
- **Privacy:** microphone muted/unmuted, OS permission denied, screen capture denied/stopped, airplane mode, no user content in logs or artifacts.
- **Device:** 720×720 square UI, display rotation/swivel, touch target, speaker/earbuds, Wi-Fi disconnect, Bluetooth disconnect, low battery/thermal.
- **Games:** general app, PPSSPP + The 3rd Birthday, Midnight Club 3, Midnight Club LA Remix, idle vs racing vs menus, app pause/resume.
- **Release proof:** source commit SHA, CI workflow run link, APK artifact checksum, tests passed/failed, firmware version, date, tester notes.

## Release report template (copy for each build)

- Version/build, commit SHA, APK hash:
- CI unit tests: PASS/FAIL with run link:
- Hardware RG Rotate Android version/SD identity:
- Physical test cases: PASS / FAIL / BLOCKED / NOT RUN:
- Battery/temperature/latency/storage metrics:
- Bugs + severity + reproduction steps:
- Regression compared with previous build:
- Decision: **SHIP TEST APK**, **FIX**, or **BLOCKED**.

### Implementation priority

1. **v0.1 source + first CI build**
2. Physical RG Rotate + 512 GB microSD: run Gate 1 tests
3. Only then integrate live AI voice and persistent semantic memory
4. Only then implement screenshots and optional vision
