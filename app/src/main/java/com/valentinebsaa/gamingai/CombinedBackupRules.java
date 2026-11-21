package com.valentinebsaa.gamingai;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable data validation and deterministic checksum for a unified export. */
public final class CombinedBackupRules {
    public static final String FORMAT = "gaming-ai-unified";
    public static final int VERSION = 1;
    private CombinedBackupRules() {}

    public static void validate(List<MemoryRecord> notes, List<String> custom) {
        if (notes == null || custom == null
                || notes.size() > MemoryPolicy.MAX_ENTRIES
                || MemoryPolicy.approxBytes(notes) > MemoryPolicy.MAX_BYTES) {
            throw new IllegalArgumentException("Memory backup exceeds the safe storage budget.");
        }
        GameProfilePolicy.validateCustom(custom);
        Set<String> ids = new HashSet<>();
        for (MemoryRecord item : notes) {
            if (item == null || item.id == null || item.id.trim().isEmpty()
                    || item.id.length() > 128 || !ids.add(item.id)
                    || item.when < 0 || item.game == null
                    || item.game.trim().isEmpty() || item.game.length() > 128
                    || item.note == null || item.note.trim().isEmpty()
                    || item.note.length() > 500) {
                throw new IllegalArgumentException("Backup contains invalid or duplicated memories.");
            }
        }
    }

    private static void add(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
        digest.update(bytes);
    }

    public static String sha256(List<MemoryRecord> notes, List<String> custom) {
        validate(notes, custom);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            add(digest, FORMAT);
            add(digest, Integer.toString(VERSION));
            add(digest, Integer.toString(notes.size()));
            for (MemoryRecord item : notes) {
                add(digest, item.id);
                add(digest, Long.toString(item.when));
                add(digest, item.game);
                add(digest, item.note);
                add(digest, item.pinned ? "1" : "0");
            }
            add(digest, Integer.toString(custom.size()));
            for (String name : custom) add(digest, name);
            StringBuilder result = new StringBuilder();
            for (byte b : digest.digest()) {
                result.append(Character.forDigit((b >>> 4) & 0xF, 16));
                result.append(Character.forDigit(b & 0xF, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable on this Android installation.", impossible);
        }
    }
}
