# Gaming AI · RG Rotate companion

**Pre-device release candidate v0.2.0 source** for the Anbernic RG Rotate (Android 12, 720×720 display). This is the start of a single-personality voice-first gaming companion, not yet the complete AI. See [the gated roadmap](docs/ROADMAP.md) for acceptance tests and later features.

## Implemented in v0.2.0 RC

- Square-screen touch interface for tagged notes about **Midnight Club 3: DUB Edition**, **Midnight Club: LA Remix**, **The 3rd Birthday**, and general conversations.
- SAF picker for an **existing subfolder on a removable microSD**; persist the folder permission. Reject internal/cloud selections and fail closed if SD is unavailable.
- Notes stored on SD as two alternate, revisioned JSON snapshots; new snapshots include field-sensitive SHA-256 checksums, and legacy snapshots remain readable. No user conversation contents stored in app preferences.
- Add/review/pin memories. Compaction is explicit, requires a preview/confirmation, and makes a verified recovery backup before removing duplicate/older unpinned notes. Pinned memories are protected. The **256 KiB / 500-record** working library is not a generative long-term-memory engine; archived history must be copied off-card for disaster recovery.
- Optional **three-layer PNG floating companion orb** with real status indication (IDLE / READY / SAVED / NO SD / ERROR); draggable and tap-to-open. Requires explicit Android “display over other apps” approval and displays a foreground notification with **Hide orb**.
- SD write/read/delete self-test. Main-screen notes are entered manually; the separate Voice Lab offers opt-in **on-device-only** speech recognition if the firmware provides it. There is no network-recognition fallback and no in-game voice service.
- **Voice Lab:** no-credit voice audition using installed non-network English Android TTS voices, selectable voice/pitch/speed, mute/stop, opt-in Android 12 on-device speech recognition when available, and a deliberately scripted game-tagged demonstration that can read existing microSD notes. Does NOT converse generatively or speak over PPSSPP.
- Automated policy tests and GitHub Actions APK build pipeline. No background microphone capture, screen recording, cloud data transfer or service keys in this build. Voice Lab requests microphone access only if the user taps on-device push-to-talk.

## Build

The CI workflow downloads Android SDK 34 and Gradle 8.7 and runs:

```sh
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

The Android-debug-signed APK is uploaded to the **Android debug APK** workflow artifact after CI succeeds. Debug APK is Android-debug signed, suitable for sideloaded testing. It is not a release-signed artifact. CI must pass before any new candidate is announced. The APK remains a sideloaded debug build, not a store-signed public release.

## On the device

1. Put a **512 GB microSD** in the RG Rotate. In Android's Files picker, create/open an ordinary folder on the card, e.g. `GamingAI`, **not the card root**.
2. Install the CI-built debug APK when it exists, open **Gaming AI Rotate**, choose the SD subfolder, grant access.
3. Use **Test SD write/read/delete**. Add a tagged memory, close/reopen app, inspect the saved note.
4. Optional: open **Voice Lab** to preview installed offline voices and use on-device-only push-to-talk when supported. It does not automatically save dictated notes.
5. Optional: choose **Show draggable orb over games**, allow the Android overlay permission, return to the app, and tap Show again. Drag the orb, tap it to reopen the app, or use Hide from the notification. Its status reflects the last operation; it is not claiming to listen or view gameplay.\n6. Test card removal, reinsert, permission revoke, screen orientation and PPSSPP coexistence as specified in [docs/ROADMAP.md](docs/ROADMAP.md).

## Important limitations

- Android normally installs APKs and keeps OS permissions/preferences internally. **Persistent note contents go on the microSD**, but not literally *every byte* of this Android app.
- Android 11+ commonly blocks the root of an SD volume in the folder picker; select a dedicated subfolder.
- v0.2.0 does **not** capture gameplay, delete screenshots from other apps, know what game is running, launch games, converse using generative AI, or speak while another app is foregrounded. Those are separately gated features, not claims of an existing app.
- Some SD providers may expose unusual document IDs, in which case the conservative removable-card check rejects them. That case must be investigated on actual RG Rotate hardware before relaxing protection.
- Not verified on physical RG Rotate hardware. CI compilation, emulator UI checks and physical acceptance are three different gates.

**Privacy:** an overlay foreground service is used only after you explicitly enable it; no account, no network permission, no telemetry, no screenshots, no hardcoded credentials. Voice Lab has on-demand mic permission only; Android's external TTS/ASR engines have their own data-handling policies. Do not commit real gaming session data.

## Personalize the orb (v0.1.3)

Use **Color your companion** in the app. RGB sliders choose any 24-bit color and show the color hex code; presets provide one-tap choices. Selecting automatic status colors switches the hue by genuine state: ready mint, saved ice, missing SD amber, errors coral. Tap **Show draggable orb over games** and grant Android overlay permission. Colors persist across app restarts. No AI mood detection or game watching exists yet.

See [palette details](docs/ORB_PALETTE.md) and [test checklist](docs/OVERLAY_TESTS.md).


## Voice Lab · no API fees (v0.1.3)

Open **Voice Lab** from the main app. Preview installed offline English Android voices, change the voice, speed and pitch, mute or stop, and test typed scripted replies. On-device microphone use is optional: the button reports if the Android 12 speech service is unavailable, and requests mic permission only on tap. No automatic cloud fallback. This does not promise a natural feminine voice, background conversation or an AI model. See [voice QA and test plan](docs/VOICE_LAB_TESTS.md).


## Guided offline voices (v0.1.9)

Open **Voice Lab & offline voice guide**. The four-step wizard checks Android's TTS voice data, opens the engine's own optional voice-pack installer, offers Android TTS Settings (with accessibility fallback), and refreshes the catalog on return. None of those actions silently installs software; Android or the speech-engine app controls approval, network usage and downloads. The catalog filters US/UK/other English/offline favorites, reports the engine's **claimed** quality, and lets you audition/save favorite voices. An engine's offline flag does not prove voice data is downloaded: **test in Airplane mode**. If no engine or compatible Settings shortcut exists, the app explains how to navigate manually. This is still a scripted/no-credits offline demo, not a generative conversational AI. See [Voice Lab tests](docs/VOICE_LAB_TESTS.md).


## v0.1.9 RG Rotate square touchscreen readiness

A scrollable, card-based interface with swipeable shortcut chips, bounded scrollable touch voice selection, wrapped names, horizontal orb presets, native button ripples and 52dp minimum touch controls. The Device Readiness panel shows usable screen dp and actual physical pixel dimensions. Reduce Motion is available. To avoid an unintended online speech recognizer, main-screen generic note dictation now opens the on-device-only Voice Lab; typed SD note entry is preserved. The UI is designed for **720 × 720 physical pixels** with varying display density, not a fixed 720dp assumption. Automated CI and layout policies are not visual device testing. [Touchscreen QA and first-day hardware checklist](docs/SQUARE_TOUCHSCREEN_TESTS.md).


## v0.1.9 Premium UI + Memory Library

Midnight-gradient native Android styling, softly glass-shaded cards, shared three-layer orb headers, restrained violet actions and teal labels, and consistent swipe/scroll navigation across the dashboard, Voice Lab and new Memory Library. No expensive real-time blur or background animation. The Memory Library provides search, per-game filters, edit, pin/unpin, delete confirmation and an explicit JSON backup/restore preview and confirmation flow. Backups remain **plaintext on the selected microSD**, not an external disaster-recovery copy. The generic screen-capture and cloud AI functions are still not present. [Design and first-day QA](docs/PREMIUM_UI_SPEC.md). UI fidelity requires emulator/device visual inspection; compilation alone is insufficient.


## v0.1.9 microSD Game Profiles

Choose **Manage Game Profiles** from the main dashboard to add custom game titles for PSP, PS1, PS2, GameCube or other games. No ROM scanning or automatic game identification. Only a manual list of titles is stored in two alternating **game-profiles-a/b.json** snapshots in the same SAF-selected removable microSD folder as your notes. The original three game names and General remain available. Existing memories' game names reappear even without a profile index. The game picker, Voice Lab context and Memory Library filters use these titles. Empty custom profiles can be removed with confirmation; profiles with notes and the four starter entries cannot be removed. **The legacy note-only export covers notes only; the newer COMPLETE JSON export contains both notes and custom game profiles.** No hidden cloud sync, screenshots, generative AI, app monitoring, or subscription.

See [game profile testing and future readiness plan](docs/GAME_PROFILES_TESTS.md). The pretty simulated concept screens in chat show visual design intent, not verified UI screenshots or proof of AI abilities.


## v0.1.9 • First-day Setup + COMPLETE backup

The dashboard now has a prominent **Start Here** card leading to a device setup guide. It reports the real screen px/dp and Android version, reads the selected microSD without fallback, offers a user-triggered write/read/delete test, explains overlay permission, and links into offline voice setup. Manual touch/swivel/PPSSPP checks are *user-reported*, not falsely marked verified by the application. A **Copy diagnostic report** button provides screen/permission/test metadata without notes, screenshots or folder paths. The app does not silently record audio, request privileges or install voice packs.

In Memory Library, **Create COMPLETE backup** combines saved notes AND custom game profiles into a single, plaintext JSON file on the user-approved microSD, protected with a deterministic SHA-256 integrity digest. Import shows a non-mutating preview, then a second confirmation before replacing either section. The app creates and verifies an emergency export of the prior dataset before restoring and attempts rollback on errors. The legacy notes-only backup/restore remains for previous exports. These backups do **not** include Android-managed app preferences, TTS/voice packs, or emulator save files; they are not encrypted and must also be copied *off* the card for protection against card failure.

See [pre-hardware QA plan](docs/FIRST_DAY_SETUP_TESTS.md). Emulator screenshot CI and JVM tests are not retail RG Rotate physical validation.


## v0.1.9 • compact premium dashboard

The main landing screen now leads with four compact, adaptive premium tiles (Voice Lab, Memory Library, Game Profiles and First-day Setup) instead of a large repeated setup card plus broad shortcut list. A swipeable secondary rail jumps straight to Orb, microSD and Device sections. Tile groups form two columns when usable width is at least 320 dp, one column for very narrow/high-density display scales. Touch ripples, gentle press animation, reduced motion, accessible labels and minimum touch sizes remain supported. The header orb/logo is more compact to free room for real controls on the **720×720 physical pixel** display.

Voice Lab now keeps diagnostics collapsed by default behind a tappable summary; tap to expand and see full installed TTS engine and on-device recognition details. This responds to actual emulator captures where verbose diagnostics occupied too much of the initial viewport. The same offline-only policy, data schemas and no-subscription approach remain unchanged. **CI can prove layout structure/touch navigation but not retail hardware fidelity or the exact concept artwork.**


## v0.2.0 RC • Pre-hardware hardening

- Two-slot SD memory and custom profile snapshots now include SHA-256 checksums on new writes. Legacy JSON snapshots without checksums remain readable; corrupted new snapshots are rejected in favor of the other valid slot when possible. Readback compares full content rather than merely the record count.
- Reviewing memory compaction requires confirmation and saves/verifies a recovery export before dropping any unpinned notes. No silent history deletion; older snapshots may retain deleted records until overwritten.
- First-day Setup checkmarks persist across page visits; **user-reported PASS** remains separate from automated system checks. You can reset test checkmarks safely. Diagnostic reports now include the actual installed app version.
- When Gaming AI itself has focus, controller **B** goes back and **L1/R1** page long screens. Firmware mappings still need physical testing. The non-focusable game overlay does **not** intercept in-game inputs.
- Reset the orb to its top-right position, and clamp its coordinates when Android display dimensions change.
- CI adds a release-specific privacy/permission/packaging audit, plus 720×720 emulator screenshots at 130%, 150%, **200% font scale**, and basic simulated controller input.
- All new APKs remain held until the candidate build and emulator smoke tests pass. These tests cannot verify microphone quality, PPSSPP overhead, swivel mapping or battery on the actual handheld.

Read [the consolidated release-candidate / first-day plan](docs/PRE_DEVICE_RELEASE_RC.md) before installing. **No raw screenshot capture, automatic game detection, generative offline AI, continuous listening or pay-per-minute cloud speech is implemented by v0.2.0.**


## IMPORTANT: upgrade-safe signing for the actual handheld

GitHub's ephemeral runner debug keystore is **not guaranteed stable between builds**. The user should configure a private one-time signing key as four GitHub Actions secrets before their *first permanent* device installation. On a main-branch workflow, the additional `gaming-ai-rotate-v0.2.0-signed` artifact is produced only when the secrets are installed and Android's `apksigner` verifies the result. Keep the keystore private and backed up off-device. [One-click Windows signing automation](docs/WINDOWS_SIGNING_AUTOMATION.md) or [manual signing instructions](docs/UPGRADE_SAFE_SIGNING.md). Do not mistake a successful debug APK for an upgrade-safe release.


## One-click Windows signing setup (no keystore in source)

After downloading the `gaming-ai-windows-signing-setup` artifact from a successful GitHub Actions run, extract and run `START-GAMING-AI-SIGNING.cmd` on your trusted Windows PC. You must authorize GitHub, choose and securely back up a private signing password, and confirm reuse if signing secrets already exist. The script generates/reuses a private signing keystore and sends the four encrypted GitHub Actions values without exposing them in public source. It now triggers a **manual** signed workflow on `main`, which was previously restricted to push events.

[Details and one-time security checklist](docs/WINDOWS_SIGNING_AUTOMATION.md). A stable signing key is required for the first permanent device install, but it is not generated in the public repository. Do not upload your private keystore into chat.
