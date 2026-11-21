# v0.1.2 draggable orb: hands-on test checklist

**Prerequisite:** successful GitHub APK build and install on the physical Anbernic RG Rotate. Source commit is not a hardware PASS.

1. Launch app. Before permission, there must be **no overlay**. Tap **Show draggable orb over games**; Android's overlay permission Settings must open. Deny it and confirm nothing appears.
2. Grant permission, return, tap **Show** again. Purple 3-layer PNG orb and `IDLE` indicator should appear; notification should provide **Hide orb**.
3. Tap orb: Gaming AI returns to foreground. Drag it to each corner. Ensure it doesn't steal game D-pad, face-button, or virtual touch control inputs except within its own small footprint.
4. Select actual removable SD subfolder and run self-test: label should become `READY`. Save a memory: `SAVED`. Remove SD and try a write: `NO SD`. No new files may be saved internally.
5. Run PPSSPP and the three target games, including full screen, landscape if supported, opening/closing swivel, sleep/resume. Record whether overlay persists, moves correctly, causes stutter, or is hidden by firmware/emulator.
6. Hide orb using app button. Re-enable, hide using notification. Check foreground notification removal, absence of stale overlay, and no microphone or screenshots running.
7. Test permission revocation and force-stop; app must never turn overlay back on without permission. After app process kill, overlay may stop; this baseline does not promise automatic resurrection.
8. Compare battery impact and emulator FPS with orb OFF vs ON for 30 minutes. Record hardware version, Android version, CI run, and whether requested APK version matches.
9. Status labels are **last reported operations**. They are not an active watcher or proof that AI is listening. This is a deliberately restricted preview.

## Color personalization regression

10. Verify each PNG is truly grayscale with transparent alpha, without baked-in checkerboard.
11. Move each RGB slider through 0 and 255 and at least two mixed combinations; verify preview and overlay are tinted while highlights and transparent edges remain intact.
12. Tap Violet, Rose, Teal, White presets; verify immediate preview and overlay updates and correct hex label.
13. Restart app and overlay: manual RGB selections and automatic mode persist.
14. Enable **Automatic status colors**; run SD test / save note / remove SD and confirm READY mint, SAVED ice, NO SD amber. The dot remains the true status indicator.
15. Disable Automatic, change a custom hue, then trigger failures/successes: hue remains fixed while status label/dot can still change.
16. Change colors with the overlay visible over PPSSPP; ensure no duplicated orb, stuck foreground service, or new permissions.
17. Do not claim the AI chose a color until the later AI companion milestone actually exists.
