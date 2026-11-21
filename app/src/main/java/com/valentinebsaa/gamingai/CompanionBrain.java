package com.valentinebsaa.gamingai;

import java.util.List;
import java.util.Locale;

/**
 * Transparent zero-credit response rehearsal. This is not an LLM and it does
 * not observe the screen, infer game progress, or continuously learn.
 * The same speaking style is used across every game tag.
 */
public final class CompanionBrain {
    private CompanionBrain() {}

    public static String reply(String message, String game, List<String> recentGameMemories) {
        if (message == null || message.trim().isEmpty()) {
            return "I didn't catch anything. Try a short question.";
        }
        String input = message.toLowerCase(Locale.ROOT).trim();
        String title = game == null || game.trim().isEmpty() ? "your game" : game.trim();

        if (containsAny(input, "remember", "memory", "last time", "what did i say")) {
            if (recentGameMemories != null && !recentGameMemories.isEmpty()) {
                String last = recentGameMemories.get(0);
                if (last != null && !last.trim().isEmpty()) {
                    last = last.trim().replaceAll("[\\r\\n]+", " ");
                    if (last.length() > 180) last = last.substring(0, 180) + "...";
                    return "For " + title + ", you saved this: " + last
                        + ". That's from your microSD notes, not something I saw happening.";
                }
            }
            return "No saved notes for " + title
                + " are available here yet. You can add one from the main screen.";
        }

        if (containsAny(input, "can you see", "look at the screen", "what is on my screen",
                "watch me", "what am i doing")) {
            return "Not yet. I can't see your game screen. Tell me what's happening in "
                + title + ", and I'll keep you company.";
        }

        if (containsAny(input, "help", "stuck", "what should i do", "how do i", "any tips")) {
            return "Tell me the mission, race, or exact problem in " + title
                + ". This offline demo has no live game guide or visual recognition yet.";
        }

        if (containsAny(input, "who are you", "what can you do", "are you an ai")) {
            return "I'm your early Gaming AI voice prototype. I can speak with an installed "
                + "offline voice, read saved notes when you ask, and stay quiet otherwise. "
                + "Free-form AI conversation comes later.";
        }

        if (containsAny(input, "hello", "hey", "hi ", "good morning", "good evening")) {
            return "Hey! Ready for " + title + "? I'm here if you feel like talking.";
        }

        if (containsAny(input, "stop", "quiet", "mute")) {
            return "Got it. Tap Mute or Stop in Voice Lab whenever you need silence.";
        }

        return "I'm with you in " + title + ". Tell me what just happened or ask about "
            + "a saved note. My offline conversations are deliberately simple for now.";
    }

    private static boolean containsAny(String input, String... phrases) {
        for (String phrase : phrases) if (input.contains(phrase)) return true;
        return false;
    }
}
