# RG Rotate 720 × 720 touchscreen QA • v0.1.5

## Engineering choices

- **720 × 720 physical pixels is not 720 dp.** Scale and Android's status/navigation bars shrink usable layout space. Responsive card margins are based on current screenWidthDp, not raw pixels.
- Main screen uses scrollable cards for Voice, microSD, Orb, Memories and Device. The horizontal finger-swiped shortcuts jump to each section without requiring long scrolls.
- Voice Lab uses scrollable cards for Setup, Voices, Talk and Privacy. Voice browser is a bounded native ListView with touch selection highlight, scrollbar, filter and favorites, instead of an oversized spinner/dropdown.
- Orb color presets occupy a horizontal scroll strip, not four equal constrained columns. Text and buttons can wrap at high font scales; primary touch buttons have 52 dp minimum height, tactile press feedback and subtle ripple.
- Optional **Reduce interface animations** setting persists. Animations also honor Android's system-wide animation-off preference. Nothing animates continuously behind a PSP game.
- A new Device Readiness card reports actual display px, available dp, Android version, overlay grant and last selected microSD folder.
- Main screen's former generic Android recognizer shortcut was intentionally replaced with **on-device-only Voice Lab**. That older shortcut could choose a network speech recognizer even when the app itself had no INTERNET permission. To save a memory, type it on the main screen and confirm Save; Voice Lab still supports optional on-device-only push-to-talk for the scripted demo.

## CI gates
- [ ] Layout policy unit tests for 720px at densities 1.0/1.5/2.0/2.5/3.0 verify bounded padding and fixed-height voice list.
- [ ] Existing memory, orb and voice tests PASS; grayscale PNG asset integrity PASS; v0.1.5 debug APK compiles on clean GitHub checkout.
- [ ] Build run and artifact recorded before announcing build ready.

## Real-device QA (NOT RUN until actual handheld)
- [ ] Report actual Android model, firmware, physical display resolution, available dp and font/display size settings.
- [ ] Screen 720×720: main screen cards, header, bottom privacy, status, all buttons reachable by touch; no cropped text with system navigation visible.
- [ ] Repeat with Android 100%, 130%, 150%, 200% font settings and display-size changes. Check 240–480dp available widths with keyboard visible.
- [ ] Swipe horizontal section shortcuts; select every card. Swipe orb color presets, choose first and last, verify preview/overlay.
- [ ] Voice catalog: scroll 50 voices, filter US/UK/Other/Favorites, select and star, return and preview; no wrong voice or sticky nested scroll.
- [ ] Tap, hold, rapid tap and use external gamepad to ensure no duplicate commands, jank or stolen input. Reduced Motion off/on and Android animations disabled.
- [ ] Swivel open/close, screen off/on, microphone/headphones, PPSSPP running, app background/resume; no constant animation or microphone usage in the game.
- [ ] Upgrade from v0.1.4 with saved SD URI, pinned notes and chosen orb color; no corrupt data.
- [ ] Real battery, GPU/CPU, touch gesture responsiveness and overlay frame rate measured on retail RG Rotate.

**Do not mark visual QA passed merely from Java layout tests or an APK compile.** Hardware and emulator screenshots are a separate evidence gate.
