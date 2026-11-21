# Voice Lab v0.1.3 • zero-credit Android TTS experiment

**Not a full AI or a live voice companion.** This is a small text/speech audition and a deterministic scripted companion, used to choose a voice *before* any paid provider. It has no internet permission, API credentials, Piper, background listening, screenshot capture, or game controls.

## What it actually does

- Offers Android's **installed English voices that declare network NOT required**. If zero are present, text conversation still works but voice fails closed.
- Lets a user preview each installed voice, choose one, adjust pitch and rate (0.7–1.5×), mute and stop immediately. Persists voice settings locally via Android preferences; saved user notes remain in the selected removable microSD folder.
- Offers optional push-to-talk **only** if Android 12's on-device `SpeechRecognizer` service is available and the user grants microphone access. No network recognizer fallback; type instead when missing.
- Allows manually asking for a scripted response, and reading **one existing note for the selected game** from the configured microSD. Does not record conversation, perform semantic inference, or claim to see gameplay.
- Closes/cancels microphone and stops speech on leaving Voice Lab. Does not speak while PPSSPP is in foreground.

## Required device acceptance checks (all NOT RUN until actual hardware)

- [ ] V1 Fresh-install upgrade from v0.1.2 keeps existing microSD URI/memory/orb preferences. Version 0.1.3/4 appears.
- [ ] V2 Launch Voice Lab on Android 12, stock and any alternative firmware. Record installed English offline TTS voice count; voice names may not encode gender. No promise of a feminine voice without audition.
- [ ] V3 Select each candidate offline voice and preview 10 identical lines. Note naturalness, timbre, articulation, punctuation, harshness, repetition, cutoffs. Choose a preferred candidate only after listening.
- [ ] V4 Change voice and rate/pitch. Stop playback mid-sentence; text-only mute; re-enter/reboot retains settings.
- [ ] V5 Turn on Airplane mode, verify no paid calls and selected offline TTS still speaks. If not, investigate OS voice packs and default engine.
- [ ] V6 If on-device speech recognition available, grant mic and ask a question; observe transcript; stop and deny/revoke mic; must never select a cloud fallback. If absent, explicit unavailability instead of crash.
- [ ] V7 Selected game remembered note can be read; remove SD/revoke access and verify no guessed memory or storage fallback.
- [ ] V8 Enter background/lock screen/swivel; no microphone remains active; no audio continues in PPSSPP.
- [ ] V9 720×720 screen: no clipped voice dropdown, sliders or buttons; screen scrolls; tap targets stay usable.
- [ ] V10 Play PPSSPP with Voice Lab not active; no measurable regression from uninitialized voice engine.
- [ ] V11 Inspect app permissions: RECORD_AUDIO requested only on demand. No INTERNET permission, no raw audio/transcript files in app storage or microSD.
- [ ] V12 Verify on-device speech services are optional and TTS initialization failure still leaves notes/orb unaffected.

## Honest scope/decision criteria

This approach has **$0 third-party API usage**, but may require installing or downloading an offline voice package via the OS's TTS engine and requires sufficient device performance. Naturalness depends on installed engine and voice. No gender metadata is guaranteed. It is not a local generative AI model, and on-device ASR is *not guaranteed to be present*. The Android OS may cache data internally and voice packages may require storage outside of microSD; the memory policy applies to user notes.

**Stop/go:** If the installed free voices sound robotic, do not quietly ship them as the final companion. Keep the Voice Lab for comparing voices, and investigate optimized licensed local neural voices and optional user-approved cloud fallback. No paid calls without explicit consent, cost ceiling and secure token broker.


## v0.1.4 guided offline voice catalog regression (device tests pending)
- [ ] V13 Verify catalog groups **All English / US / UK / Other / Favorites** and each group accurately filters real TTS voices, with same-group higher claimed quality ordered first. Unknown gender must never be labeled female/male without verified metadata.
- [ ] V14 Star/unstar voice, select Favorites, preview and restart app; favorite and chosen voice persist and no broken selection indices occur after filtering.
- [ ] V15 No English offline voices: empty list is explicit, text replies remain available, no crash when previewing empty selection.
- [ ] V16 Check installed voice data via active engine; PASS/non-PASS/unsupported handled without invented results or automatic download.
- [ ] V17 Manage/download voice data offers engine's official UI if supported. Engine missing installer: no crash, Android TTS/Accessibility settings shortcut fallback with manual guidance.
- [ ] V18 Return from external settings after changing engine/voice pack, refresh/reinitialize TTS and ensure new list reflects actual engine. Device may require app restart. Never claim download completed unless TTS data check or actual playback confirms it.
- [ ] V19 Try Airplane mode before/after installing official offline voice pack. Selected non-network voice must still speak with game sound muted; when absent, text fallback. No INTERNET permission in our APK; external installer may require network for downloads.
- [ ] V20 Filter/favorite controls work on 720×720 with touch and swivel; PPSSPP, memory and orb regressions still passed. 0.1.3→0.1.4 upgrade retains selected voice, rate/pitch, memory and orb preferences.
- [ ] V21 SDK 34 APK compiles and unit tests pass on clean main branch; install on RG Rotate not claimed until performed.


## v0.1.5 square-screen update
The voice options now use a fixed-height finger-scrollable ListView with tap-to-select highlight and native scrollbar rather than one large Android voice dropdown. Verify scrolling the voice list and surrounding page independently at 720×720 and 150% font scale, all five filters, favorites and correct preview. See [square touchscreen QA](SQUARE_TOUCHSCREEN_TESTS.md).
