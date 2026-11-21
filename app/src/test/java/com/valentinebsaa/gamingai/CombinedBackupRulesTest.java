package com.valentinebsaa.gamingai;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class CombinedBackupRulesTest {
    private List<MemoryRecord> samples() {
        return Arrays.asList(
            new MemoryRecord("id1", 100, "The 3rd Birthday", "Mission note", true),
            new MemoryRecord("id2", 200, "Persona 3 Portable", "A boss strategy", false));
    }
    @Test public void checksumIsStableAndSensitiveToAllFields() {
        List<String> games = Arrays.asList("Persona 3 Portable", "God of War");
        String first = CombinedBackupRules.sha256(samples(), games);
        assertEquals(64, first.length());
        assertEquals(first, CombinedBackupRules.sha256(samples(), games));
        assertNotEquals(first, CombinedBackupRules.sha256(samples(),
            Arrays.asList("God of War", "Persona 3 Portable")));
        List<MemoryRecord> edited = new ArrayList<>(samples());
        edited.set(1, new MemoryRecord("id2", 200, "Persona 3 Portable",
            "A different boss strategy", false));
        assertNotEquals(first, CombinedBackupRules.sha256(edited, games));
        edited.set(1, new MemoryRecord("id2", 200, "Persona 3 Portable",
            "A boss strategy", true));
        assertNotEquals(first, CombinedBackupRules.sha256(edited, games));
    }
    @Test public void emptyPayloadStillHasValidChecksum() {
        String sum = CombinedBackupRules.sha256(new ArrayList<>(), new ArrayList<>());
        assertTrue(sum.matches("[0-9a-f]{64}"));
    }
    @Test public void duplicateIdsAndInvalidProfileNamesFailClosed() {
        List<MemoryRecord> broken = new ArrayList<>(samples());
        broken.add(new MemoryRecord("id1", 300, "General", "Duplicate", false));
        try { CombinedBackupRules.validate(broken, new ArrayList<>()); fail(); }
        catch (IllegalArgumentException expected) {}
        try { CombinedBackupRules.validate(samples(), Arrays.asList("General")); fail(); }
        catch (IllegalArgumentException expected) {}
    }
    @Test public void max500EntriesEnforcedAndMalformedContentRejected() {
        List<MemoryRecord> notes = new ArrayList<>();
        for (int i = 0; i < MemoryPolicy.MAX_ENTRIES + 1; i++)
            notes.add(new MemoryRecord("n" + i, i, "General", "test note", false));
        try { CombinedBackupRules.validate(notes, new ArrayList<>()); fail(); }
        catch (IllegalArgumentException expected) {}
        try { CombinedBackupRules.validate(Arrays.asList(
            new MemoryRecord("id", 1, "General", " ", false)), new ArrayList<>()); fail(); }
        catch (IllegalArgumentException expected) {}
    }
}
