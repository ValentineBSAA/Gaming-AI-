package com.valentinebsaa.gamingai;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public final class GameProfilePolicyTest {
    @Test public void defaultsAreStableAndOrdered() {
        assertEquals("General", GameProfilePolicy.defaults().get(0));
        assertTrue(GameProfilePolicy.defaults().contains("The 3rd Birthday"));
    }
    @Test public void customTitlesAreNormalizedAndDoNotDuplicateBuiltins() {
        List<String> custom = GameProfilePolicy.add(Collections.emptyList(),
            "  God    of War:   Chains of Olympus  ");
        assertEquals("God of War: Chains of Olympus", custom.get(0));
        try {
            GameProfilePolicy.add(custom, "god of war: chains of olympus");
            fail("Duplicate must fail");
        } catch (IllegalArgumentException expected) {}
        try {
            GameProfilePolicy.add(custom, "The 3rd Birthday");
            fail("Starter must not be duplicated");
        } catch (IllegalArgumentException expected) {}
        try {
            GameProfilePolicy.add(custom, "All games");
            fail("Filter label reserved");
        } catch (IllegalArgumentException expected) {}
    }
    @Test public void savedNoteGamesArePreservedWithoutProfileIndex() {
        List<MemoryRecord> notes = Arrays.asList(
            new MemoryRecord("1", 10, "Gran Turismo", "Challenge", true),
            new MemoryRecord("2", 11, "The 3rd Birthday", "Boss", false));
        List<String> games = GameProfilePolicy.allGames(
            Collections.singletonList("God of War"), notes);
        assertTrue(games.contains("God of War"));
        assertTrue(games.contains("Gran Turismo"));
        assertEquals(1, Collections.frequency(games, "The 3rd Birthday"));
    }
    @Test public void removeOnlyCustomAndNeverDeleteSavedNotes() {
        List<String> initial = GameProfilePolicy.add(Collections.emptyList(), "Persona 3 Portable");
        assertTrue(GameProfilePolicy.remove(initial, "Persona 3 Portable").isEmpty());
        try {
            GameProfilePolicy.remove(initial, "General");
            fail("Built-in cannot be removed");
        } catch (IllegalArgumentException expected) {}
    }
    @Test public void rejectsControlCharactersOversizeAndExcessiveProfiles() {
        for (String bad : new String[]{"", "   ", "bad\nname", "x".repeat(65)}) {
            try {
                GameProfilePolicy.normalize(bad);
                fail("Unexpected accepted name: " + bad);
            } catch (IllegalArgumentException expected) {}
        }
        List<String> names = new java.util.ArrayList<>();
        for (int i = 0; i < 81; i++) names.add("Test Game " + i);
        try {
            GameProfilePolicy.validateCustom(names);
            fail("Should reject >80");
        } catch (IllegalArgumentException expected) {}
    }
}
