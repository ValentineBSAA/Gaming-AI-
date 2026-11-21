package com.valentinebsaa.gamingai;

import static org.junit.Assert.*;
import org.junit.Test;

public final class OrbPaletteTest {
    @Test public void manualColorOverridesAllStates() {
        assertEquals(0xff3366aa, OrbPalette.resolve(false, 0x003366aa, "ERROR"));
        assertEquals(0xff000000, OrbPalette.resolve(false, 0, "READY"));
        assertEquals(0xffffffff, OrbPalette.resolve(false, 0xffffffff, "IDLE"));
    }

    @Test public void automaticModeUsesActualStatus() {
        assertEquals(0xff71dece, OrbPalette.resolve(true, 0xff123456, "READY"));
        assertEquals(0xff91caff, OrbPalette.resolve(true, 0xff123456, "SAVED"));
        assertEquals(0xffffb15e, OrbPalette.resolve(true, 0xff123456, "NO SD"));
        assertEquals(0xffff6f88, OrbPalette.resolve(true, 0xff123456, "ERROR"));
        assertEquals(OrbPalette.DEFAULT_COLOR, OrbPalette.resolve(true, 0xff123456, "IDLE"));
    }

    @Test public void unknownAiMoodDoesNotFakeAwareness() {
        assertEquals(OrbPalette.DEFAULT_COLOR, OrbPalette.resolve(true, 0, "WATCHING GAME"));
    }
}
