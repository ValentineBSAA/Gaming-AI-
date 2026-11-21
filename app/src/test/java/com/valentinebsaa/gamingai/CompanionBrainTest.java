package com.valentinebsaa.gamingai;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public final class CompanionBrainTest {
    @Test public void admitsItCannotWatchGames() {
        String answer = CompanionBrain.reply("Can you see the screen?",
            "The 3rd Birthday", Collections.emptyList());
        assertTrue(answer.contains("can't see"));
        assertTrue(answer.contains("The 3rd Birthday"));
    }
    @Test public void recallsOnlySuppliedSdMemoryAndDisclosesSource() {
        String answer = CompanionBrain.reply("What do you remember?",
            "Midnight Club 3: DUB Edition", Arrays.asList("I unlocked a new car"));
        assertTrue(answer.contains("I unlocked a new car"));
        assertTrue(answer.contains("microSD"));
        assertFalse(answer.contains("saw you"));
    }
    @Test public void missingMemoryIsNotInvented() {
        String answer = CompanionBrain.reply("What do you remember?",
            "Midnight Club: LA Remix", Collections.emptyList());
        assertTrue(answer.contains("No saved notes"));
    }
    @Test public void samePersonalityAcrossGamesWithoutInventedDetail() {
        String first = CompanionBrain.reply("hello", "The 3rd Birthday", null);
        String second = CompanionBrain.reply("hello", "Midnight Club 3", null);
        assertTrue(first.startsWith("Hey!"));
        assertTrue(second.startsWith("Hey!"));
    }
    @Test public void blankAndHelpHaveSafeFallbacks() {
        assertTrue(CompanionBrain.reply("  ", "General", null).contains("didn't catch"));
        assertTrue(CompanionBrain.reply("Help I'm stuck", "General", null)
            .contains("no live game guide"));
    }
}
