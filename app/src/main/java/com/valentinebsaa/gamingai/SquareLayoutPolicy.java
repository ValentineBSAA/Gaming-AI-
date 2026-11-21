package com.valentinebsaa.gamingai;

/** Physical 720x720 pixels can be much less than 720dp at device density. */
public final class SquareLayoutPolicy {
    private SquareLayoutPolicy() {}

    public static int paddingDp(int widthDp) {
        if (widthDp < 320) return 10;
        if (widthDp < 400) return 14;
        return 18;
    }

    public static int cardPaddingDp(int widthDp) {
        return widthDp < 320 ? 10 : 14;
    }

    public static int voiceListHeightDp(int widthDp) {
        return widthDp < 320 ? 180 : widthDp < 480 ? 216 : 248;
    }

    public static int quickActionColumns(int widthDp) {
        return widthDp < 320 ? 1 : 2;
    }

    public static int presetWidthDp(int widthDp) {
        return widthDp < 400 ? 96 : 112;
    }
}
