# Gaming AI · RG Rotate companion

**Prototype v0.1.1 source** for the Anbernic RG Rotate (Android 12, 720×720 display). This is the start of a single-personality voice-first gaming companion, not yet the complete AI. See [the gated roadmap](docs/ROADMAP.md) for acceptance tests and later features.

## Implemented in v0.1.1

- Square-screen touch interface for tagged notes about **Midnight Club 3: DUB Edition**, **Midnight Club: LA Remix**, **The 3rd Birthday**, and general conversations.
- SAF picker for an **existing subfolder on a removable microSD**; persist the folder permission. Reject internal/cloud selections and fail closed if SD is unavailable.
- Notes stored on SD as two alternate, revisioned JSON snapshots (for basic corrupt-write recovery). No user conversation contents stored in app preferences.
- Add/review/pin memories. Explicit compact action deduplicates unpinned notes, preferentially removes older unpinned notes if over the **256 KiB / 500-record** prototype limit. Pinned memories are protected; when the budget cannot be met the operation errors rather than dropping them.
- Optional **three-layer PNG floating companion orb** with real status indication (IDLE / READY / SAVED / NO SD / ERROR); draggable and tap-to-open. Requires explicit Android “display over other apps” approval and displays a foreground notification with **Hide orb**.
- SD write/read/delete self-test; native Android speech-recognition intent can **dictate** note text when a provider is installed. This is NOT yet a conversational AI or background voice service.
- Automated policy tests and GitHub Actions APK build pipeline. No in-app microphone capture, screen recording, cloud data transfer or service keys in this build.

## Build

The CI workflow downloads Android SDK 34 and Gradle 8.7 and runs:

```sh
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

The unsigned-for-distribution debug APK is uploaded to the **Android debug APK** workflow artifact after CI succeeds. Debug APK is Android-debug signed, suitable for sideloaded testing. It is not a release-signed artifact. No CI run has been verified merely by committing this code.

## On the device

1. Put a **512 GB microSD** in the RG Rotate. In Android's Files picker, create/open an ordinary folder on the card, e.g. `GamingAI`, **not the card root**.
2. Install the CI-built debug APK when it exists, open **Gaming AI Rotate**, choose the SD subfolder, grant access.
3. Use **Test SD write/read/delete**. Add a tagged memory, close/reopen app, inspect the saved note.
4. Optional: **Dictate note** opens Android's installed recognizer, if present. This is a note-entry shortcut only.
5. Optional: choose **Show draggable orb over games**, allow the Android overlay permission, return to the app, and tap Show again. Drag the orb, tap it to reopen the app, or use Hide from the notification. Its status reflects the last operation; it is not claiming to listen or view gameplay.\n6. Test card removal, reinsert, permission revoke, screen orientation and PPSSPP coexistence as specified in [docs/ROADMAP.md](docs/ROADMAP.md).

## Important limitations

- Android normally installs APKs and keeps OS permissions/preferences internally. **Persistent note contents go on the microSD**, but not literally *every byte* of this Android app.
- Android 11+ commonly blocks the root of an SD volume in the folder picker; select a dedicated subfolder.
- v0.1 does **not** capture gameplay, delete screenshots from other apps, know what game is running, launch games, converse using AI, or speak. Those are separately gated features, not claims of an existing app.
- Some SD providers may expose unusual document IDs, in which case the conservative removable-card check rejects them. That case must be investigated on actual RG Rotate hardware before relaxing protection.
- Not verified on physical hardware or compiled locally. CI and hardware tests are required before declaring any gate passed.

**Privacy:** an overlay foreground service is used only after you explicitly enable it; no account, no network permission, no telemetry, no screenshots, no hardcoded credentials. Do not commit real gaming session data.
