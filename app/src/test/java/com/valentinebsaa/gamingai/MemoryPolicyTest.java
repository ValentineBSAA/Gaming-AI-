package com.valentinebsaa.gamingai;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

public final class MemoryPolicyTest {
    private static MemoryRecord entry(String id, String game, String note, boolean pin) {
        return new MemoryRecord(id, 1L, game, note, pin);
    }

    @Test public void mergesUnpinnedDuplicatesButPreservesPins() {
        List<MemoryRecord> records = new ArrayList<>();
        records.add(entry("a", "MC3", "Won a race", false));
        records.add(entry("b", "MC3", " won   a race ", false));
        records.add(entry("c", "MC3", "Won a race", true));
        List<MemoryRecord> result = MemoryPolicy.compact(records, 500, 262144);
        assertEquals(2, result.size());
        assertEquals("b", result.get(0).id);
        assertEquals("c", result.get(1).id);
    }

    @Test public void discardsOldUnpinnedBeforePinned() {
        List<MemoryRecord> records = new ArrayList<>();
        records.add(entry("a", "MC3", "old", true));
        records.add(entry("b", "MC3", "second", false));
        records.add(entry("c", "MC3", "new", false));
        List<MemoryRecord> result = MemoryPolicy.compact(records, 2, 262144);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0).id);
        assertEquals("c", result.get(1).id);
    }

    @Test(expected = IllegalStateException.class)
    public void refusesToDeletePinnedToMeetBudget() {
        List<MemoryRecord> records = new ArrayList<>();
        records.add(entry("a", "MC3", "one", true));
        records.add(entry("b", "MC3", "two", true));
        MemoryPolicy.compact(records, 1, 262144);
    }

    @Test public void gameContextsDoNotMerge() {
        List<MemoryRecord> records = new ArrayList<>();
        records.add(entry("a", "MC3", "race", false));
        records.add(entry("b", "LA Remix", "race", false));
        assertEquals(2, MemoryPolicy.compact(records, 500, 262144).size());
    }
}
