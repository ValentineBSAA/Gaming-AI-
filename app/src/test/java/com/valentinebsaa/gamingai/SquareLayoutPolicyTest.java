package com.valentinebsaa.gamingai;

import static org.junit.Assert.*;
import org.junit.Test;

public final class SquareLayoutPolicyTest {
    @Test public void physical720SquareAtManyDensitiesHasSafeContentWidth() {
        float[] densities = {1f, 1.5f, 2f, 2.5f, 3f};
        for (float density : densities) {
            int widthDp = Math.round(720f / density);
            int available = widthDp - 2 * SquareLayoutPolicy.paddingDp(widthDp)
                - 2 * SquareLayoutPolicy.cardPaddingDp(widthDp);
            assertTrue("content width too small at density " + density, available >= 176);
            assertTrue(SquareLayoutPolicy.presetWidthDp(widthDp) <= 112);
            assertTrue(SquareLayoutPolicy.voiceListHeightDp(widthDp) <= 248);
        }
    }
    @Test public void voiceListHasBoundedHeight() {
        assertEquals(180, SquareLayoutPolicy.voiceListHeightDp(288));
        assertEquals(216, SquareLayoutPolicy.voiceListHeightDp(360));
        assertEquals(248, SquareLayoutPolicy.voiceListHeightDp(720));
    }
    @Test public void paddingIsBasedOnDpNotRawPixels() {
        assertEquals(10, SquareLayoutPolicy.paddingDp(280));
        assertEquals(14, SquareLayoutPolicy.paddingDp(360));
        assertEquals(18, SquareLayoutPolicy.paddingDp(500));
        assertEquals(10, SquareLayoutPolicy.cardPaddingDp(280));
        assertEquals(14, SquareLayoutPolicy.cardPaddingDp(360));
    }
}
