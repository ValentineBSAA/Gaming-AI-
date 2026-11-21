# Grayscale orb color customization (v0.1.2)

The orb is three independent **transparent grayscale PNG layers**: orb_core, orb_petals, and orb_ring. Android multiplies the color into these at runtime, retaining the original light/shadow/alpha. The small dot reflects true app status separately.

Users can select any RGB color with sliders (0–255 for each channel) or use one-tap **Violet, Rose, Teal, White** presets. The manual color and automatic-mode preference persist in small Android UI preferences; actual game memories stay on the selected microSD subfolder.

Optional **Automatic status colors**: IDLE violet, READY mint, SAVED ice blue, NO SD amber, ERROR coral. This is rule-based and not yet AI mood recognition. Later we can add AI-selected colors to match game context and conversational tone, while keeping an override and user control. See [overlay testing](OVERLAY_TESTS.md).
