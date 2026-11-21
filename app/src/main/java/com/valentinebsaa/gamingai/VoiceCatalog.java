package com.valentinebsaa.gamingai;

import java.util.Locale;

/** Deterministic voice grouping for offline English Android text-to-speech. */
public final class VoiceCatalog {
    private VoiceCatalog() {}

    public static String section(String tag) {
        Locale locale = Locale.forLanguageTag(tag == null ? "" : tag);
        String language = locale.getLanguage();
        if (!"en".equalsIgnoreCase(language)) return "Other language";
        if ("US".equalsIgnoreCase(locale.getCountry())) return "US English";
        if ("GB".equalsIgnoreCase(locale.getCountry())) return "UK English";
        return "Other English";
    }

    /** 0=all English, 1=US, 2=UK, 3=other English, 4=starred. */
    public static boolean matches(int category, String tag, boolean starred) {
        String group = section(tag);
        if ("Other language".equals(group)) return false;
        switch (category) {
            case 0: return true;
            case 1: return "US English".equals(group);
            case 2: return "UK English".equals(group);
            case 3: return "Other English".equals(group);
            case 4: return starred;
            default: return false;
        }
    }

    private static int rank(String tag) {
        String group = section(tag);
        if ("US English".equals(group)) return 0;
        if ("UK English".equals(group)) return 1;
        if ("Other English".equals(group)) return 2;
        return 3;
    }

    public static String qualityLabel(int quality) {
        if (quality >= 500) return "Very high quality (engine claim)";
        if (quality >= 400) return "High quality (engine claim)";
        if (quality >= 300) return "Normal quality (engine claim)";
        return "Basic quality (engine claim)";
    }

    public static int compare(String tagA, int qualityA, String nameA,
            String tagB, int qualityB, String nameB) {
        int group = Integer.compare(rank(tagA), rank(tagB));
        if (group != 0) return group;
        int quality = Integer.compare(qualityB, qualityA);
        if (quality != 0) return quality;
        return (nameA == null ? "" : nameA).compareToIgnoreCase(
            nameB == null ? "" : nameB);
    }
}
