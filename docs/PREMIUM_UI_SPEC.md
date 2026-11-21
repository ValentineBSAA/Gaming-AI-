# Gaming AI Premium Interface • RG Rotate 720 × 720

## Visual direction: midnight glass, violet × teal

One coherent, native Android interface throughout the Dashboard, Voice Lab and Memory Library. A subtle gradient midnight background; softly shaded translucent-looking cards with rounded 20dp corners, modest elevation, understated lavender outlines and restrained violet actions; sea-glass teal for metadata and navigation; soft white readable text. This is simulated glass styling, **not** expensive live blur or GPU effects. The three real grayscale transparent orb PNG layers appear in the shared hero header. The companion's user-selected orb color remains independent of the application accent.

### Interaction rules
- 720×720 **physical pixels**, adaptive available dp and font scale; all content vertical-scrollable.
- Headers share exact spacing and orb mark; sections follow common alignment, uppercase small section headings, and one primary action per group.
- Touch controls: 52dp minimum height, wrapped labels, native ripple and a short 95/135ms press/release scale. Respect the Reduced Motion setting and Android global animator scale. No continuous background animations while PPSSPP is running.
- Horizontal swipeable navigation and orb presets, explicit selected voice/memory row highlights, keyboard-aware scrolling; no cramped four-column buttons or long walls of undifferentiated text.
- Accessibility: native text sizing, touch controls >=48dp, high-contrast primary text and readable captions. Support TalkBack semantics on existing native widgets. Verify font scales and screen-reader traversal on device.
- Information architecture: main dashboard (Voice / microSD / Orb / Memories / Device); Voice Lab (Setup / Voices / Talk / Privacy); Memory Library (Search / Selected Memory / Backup). No duplicate competing settings.

## v0.1.6 Memory Library safety
- Search existing microSD notes and filter by game without changing source files.
- Edit, pin/unpin and delete one selected memory at a time. Destructive delete requires a separate confirmation, especially for pinned notes.
- A plaintext JSON export is created only inside the approved microSD folder; verify its readback and warn that same-card backup is not disaster protection.
- Explicit restore path: Android file picker → bounded/validated record preview → second confirmation → verified snapshot write. Back and Cancel never modify live notes.
- **Do not touch emulator screenshots, PSP games, save states or other files.** User memory remains bounded by 500 records / 256 KiB; no continuous capture, no cloud, no Piper.
- Android retains app install/settings and URI permission metadata internally; only user note content and backup JSON are placed in microSD folder.

## CI and device acceptance
- [ ] Clean checkout Android 12-compatible debug APK and JVM memory mutation, existing storage, voice, orb, square layout tests PASS.
- [ ] Original memory formats load and new library edits preserve IDs/timestamps/game and pinned flags. Corrupt/invalid JSON and missing microSD fail closed.
- [ ] Confirm restore cannot proceed without validating file and explicit second confirmation; export is verified and warns against same-card-only backups.
- [ ] Inspect emulator screenshots 720×720 at 320/400dpi and 130%/150% fonts: glass cards, header mark, button labels, selected row, color contrast, no clipping.
- [ ] Real RG Rotate: test with firmware/nav bars, square touchscreen, 150–200% font scale, swipe/scroll, swivel open/close, keyboard, favorites, orb visibility.
- [ ] Profile CPU/RAM and battery with app in background and PPSSPP running: no continuous UI animations or unexpected microphone/capture.
- [ ] User reviews visual style and actual offline voice quality on retail device before claiming hardware-ready.

**Sign-off:** CI may prove compile/tests, not the actual aesthetic or square-screen UI correctness. No hardware claim until screenshots and test evidence are recorded.
