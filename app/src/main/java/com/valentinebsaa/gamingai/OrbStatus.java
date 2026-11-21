package com.valentinebsaa.gamingai;

/** UI status comes from observed app actions; no imaginary AI listening state. */
public final class OrbStatus {
    private OrbStatus() {}

    public static String normalize(String value) {
        if ("READY".equals(value) || "SAVED".equals(value) ||
                "NO SD".equals(value) || "ERROR".equals(value)) return value;
        return "IDLE";
    }

    public static String fromMessage(String message) {
        if (message == null) return "IDLE";
        if (message.startsWith("PASS:") || message.startsWith("Verified removable")) return "READY";
        if (message.startsWith("Saved to removable") || message.startsWith("Compacted")) return "SAVED";
        if (message.startsWith("NOT SAVED:") || message.startsWith("FAIL:")
                || message.startsWith("Folder rejected:")) return "NO SD";
        if (message.startsWith("Compaction not applied:")) return "ERROR";
        return "IDLE";
    }
}
