package com.valentinebsaa.gamingai;

import static org.junit.Assert.*;
import org.junit.Test;

public final class VoiceCatalogTest {
    @Test public void filtersByEnglishRegion() {
        assertTrue(VoiceCatalog.matches(0, "en-US", false));
        assertTrue(VoiceCatalog.matches(1, "en-US", false));
        assertTrue(VoiceCatalog.matches(2, "en-GB", false));
        assertTrue(VoiceCatalog.matches(3, "en-AU", false));
        assertFalse(VoiceCatalog.matches(1, "en-GB", true));
        assertFalse(VoiceCatalog.matches(0, "fr-FR", true));
        assertFalse(VoiceCatalog.matches(4, "en-US", false));
        assertTrue(VoiceCatalog.matches(4, "en-GB", true));
        assertFalse(VoiceCatalog.matches(25, "en-US", true));
    }
    @Test public void sortsByRegionThenEngineQualityThenName() {
        assertTrue(VoiceCatalog.compare("en-US", 300, "z", "en-GB", 500, "a") < 0);
        assertTrue(VoiceCatalog.compare("en-GB", 500, "a", "en-GB", 300, "b") < 0);
        assertTrue(VoiceCatalog.compare("en-US", 300, "a", "en-US", 300, "z") < 0);
        assertEquals(0, VoiceCatalog.compare("en-US", 300, "a", "en-US", 300, "a"));
    }
    @Test public void qualityDescriptionsAreNotGenderClaims() {
        assertTrue(VoiceCatalog.qualityLabel(500).contains("engine claim"));
        assertFalse(VoiceCatalog.qualityLabel(500).contains("female"));
        assertEquals("Other language", VoiceCatalog.section(null));
    }
}
