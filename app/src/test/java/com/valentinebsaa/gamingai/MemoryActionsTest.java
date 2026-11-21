package com.valentinebsaa.gamingai;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class MemoryActionsTest {
    private List<MemoryRecord> sample() {
        return Arrays.asList(
            new MemoryRecord("one", 100, "The 3rd Birthday", "first secret", true),
            new MemoryRecord("two", 101, "Midnight Club 3: DUB Edition", "Favorite race", false),
            new MemoryRecord("three", 102, "The 3rd Birthday", "another FIRST", false)
        );
    }
    @Test public void filtersByGameAndPhraseNewestFirst() {
        List<MemoryRecord> filtered = MemoryActions.filter(
            sample(), "The 3rd Birthday", "first");
        assertEquals(2, filtered.size());
        assertEquals("three", filtered.get(0).id);
        assertEquals("one", filtered.get(1).id);
        assertEquals(3, MemoryActions.filter(sample(), "All games", "").size());
    }
    @Test public void editPreservesIdentityAndPinnedStatus() {
        List<MemoryRecord> original = sample();
        List<MemoryRecord> after = MemoryActions.replaceNote(original, "one", " fixed ");
        assertEquals("fixed", after.get(0).note);
        assertTrue(after.get(0).pinned);
        assertEquals(100, after.get(0).when);
        assertEquals("first secret", original.get(0).note);
        assertEquals(3, after.size());
    }
    @Test public void pinAndDeleteOnlySelectedNote() {
        List<MemoryRecord> original = sample();
        List<MemoryRecord> pinned = MemoryActions.setPinned(original, "two", true);
        assertTrue(pinned.get(1).pinned);
        assertFalse(original.get(1).pinned);
        List<MemoryRecord> removed = MemoryActions.delete(pinned, "two");
        assertEquals(2, removed.size());
        assertEquals("one", removed.get(0).id);
        assertEquals("three", removed.get(1).id);
    }
    @Test public void missingIdOrInvalidTextFailsClosed() {
        try { MemoryActions.delete(sample(), "missing"); fail(); }
        catch (IllegalArgumentException expected) {}
        try { MemoryActions.replaceNote(sample(), "one", " "); fail(); }
        catch (IllegalArgumentException expected) {}
        try { MemoryActions.setPinned(sample(), "missing", true); fail(); }
        catch (IllegalArgumentException expected) {}
    }
}
