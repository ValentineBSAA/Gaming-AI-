package com.valentinebsaa.gamingai;

import static org.junit.Assert.*;
import org.junit.Test;

public final class OrbStatusTest {
    @Test public void reportsOnlyRealImplementedStates() {
        assertEquals("IDLE", OrbStatus.normalize("LISTENING"));
        assertEquals("IDLE", OrbStatus.normalize("SEEING GAME"));
        assertEquals("READY", OrbStatus.fromMessage("PASS: SD test written, read, and deleted."));
        assertEquals("SAVED", OrbStatus.fromMessage("Saved to removable microSD."));
        assertEquals("NO SD", OrbStatus.fromMessage("NOT SAVED: microSD missing."));
        assertEquals("ERROR", OrbStatus.fromMessage("Compaction not applied: protected."));
    }
}
