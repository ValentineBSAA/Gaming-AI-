package com.valentinebsaa.gamingai;

/** Companion orb color policy. Automatic colors only reflect observed app status. */
public final class OrbPalette {
    public static final String PREFS = "orb_colors";
    public static final String COLOR_KEY = "manual_argb";
    public static final String AUTO_KEY = "automatic_status_colors";
    public static final int DEFAULT_COLOR = 0xffb997e8;
    private OrbPalette() {}

    public static int resolve(boolean automatic, int manualColor, String status) {
        if (!automatic) return 0xff000000 | (manualColor & 0x00ffffff);
        if ("READY".equals(status)) return 0xff71dece;
        if ("SAVED".equals(status)) return 0xff91caff;
        if ("NO SD".equals(status)) return 0xffffb15e;
        if ("ERROR".equals(status)) return 0xffff6f88;
        return DEFAULT_COLOR;
    }
}
