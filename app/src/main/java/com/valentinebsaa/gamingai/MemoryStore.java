package com.valentinebsaa.gamingai;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * All content lives in the SAF-selected SD directory; never falls back to internal files.
 * Two alternated snapshots help recover from partial writes.
 */
public final class MemoryStore {
    private static final String SLOT_A = "memory-a.json";
    private static final String SLOT_B = "memory-b.json";
    private final Context context;
    private final DocumentFile folder;

    private static final class Snapshot {
        final long revision;
        final List<MemoryRecord> entries;
        Snapshot(long revision, List<MemoryRecord> entries) {
            this.revision = revision;
            this.entries = entries;
        }
    }

    public MemoryStore(Context context, Uri selectedTreeUri) throws IOException {
        this.context = context.getApplicationContext();
        if (!StorageRules.isRemovableSdSubfolder(selectedTreeUri.getAuthority(),
                android.provider.DocumentsContract.getTreeDocumentId(selectedTreeUri))) {
            throw new IOException("Folder is not a verified removable microSD subfolder.");
        }
        this.folder = DocumentFile.fromTreeUri(context, selectedTreeUri);
        if (folder == null || !folder.exists() || !folder.isDirectory() || !folder.canRead()
                || !folder.canWrite()) {
            throw new IOException("microSD missing, read-only, or permission revoked.");
        }
    }

    public List<MemoryRecord> load() throws IOException {
        return new ArrayList<>(bestSnapshot().entries);
    }

    private Snapshot bestSnapshot() throws IOException {
        Snapshot first = readSnapshot(folder.findFile(SLOT_A));
        Snapshot second = readSnapshot(folder.findFile(SLOT_B));
        if (first == null && second == null) {
            if (folder.findFile(SLOT_A) != null || folder.findFile(SLOT_B) != null) {
                throw new IOException("Memory snapshots are unreadable; backup recovery is required.");
            }
            return new Snapshot(0, new ArrayList<>());
        }
        if (first == null) return second;
        if (second == null) return first;
        return first.revision >= second.revision ? first : second;
    }

    private Snapshot readSnapshot(DocumentFile file) {
        if (file == null) return null;
        try (InputStream in = context.getContentResolver().openInputStream(file.getUri())) {
            if (in == null) return null;
            byte[] data = readLimited(in, MemoryPolicy.MAX_BYTES + 8192);
            JSONObject root = new JSONObject(new String(data, StandardCharsets.UTF_8));
            long revision = root.getLong("revision");
            if (revision <= 0) return null;
            JSONArray notes = root.getJSONArray("entries");
            List<MemoryRecord> result = new ArrayList<>();
            if (notes.length() > 2000) return null;
            for (int i = 0; i < notes.length(); i++) result.add(MemoryRecord.fromJson(notes.getJSONObject(i)));
            return new Snapshot(revision, result);
        } catch (Exception ignored) {
            // Invalid slot is ignored only if the other slot is valid.
            return null;
        }
    }

    private static byte[] readLimited(InputStream in, int max) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            if (bytes.size() + read > max) throw new IOException("Memory exceeds safe read budget");
            bytes.write(buffer, 0, read);
        }
        return bytes.toByteArray();
    }

    public void save(List<MemoryRecord> records) throws IOException {
        if (records.size() > MemoryPolicy.MAX_ENTRIES
                || MemoryPolicy.approxBytes(records) > MemoryPolicy.MAX_BYTES) {
            throw new IOException("Memory budget reached. Compact unpinned notes first.");
        }
        Snapshot current = bestSnapshot();
        long next = current.revision + 1;
        JSONObject root = new JSONObject();
        try {
            root.put("revision", next);
            JSONArray entries = new JSONArray();
            for (MemoryRecord memory : records) entries.put(memory.toJson());
            root.put("entries", entries);
        } catch (JSONException e) {
            throw new IOException("Failed to encode memory", e);
        }
        byte[] encoded = root.toString().getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MemoryPolicy.MAX_BYTES) throw new IOException("Serialized memory budget reached.");
        String name = (next % 2 == 1) ? SLOT_A : SLOT_B;
        DocumentFile output = folder.findFile(name);
        if (output == null) output = folder.createFile("application/json", name);
        if (output == null) throw new IOException("Unable to create memory snapshot on SD.");
        try (OutputStream stream = context.getContentResolver().openOutputStream(output.getUri(), "wt")) {
            if (stream == null) throw new IOException("Unable to write memory snapshot.");
            stream.write(encoded);
            stream.flush();
        }
        Snapshot verified = readSnapshot(output);
        if (verified == null || verified.revision != next || verified.entries.size() != records.size()) {
            throw new IOException("Memory write verification failed; older snapshot retained.");
        }
    }

    /** Returns only after a content round-trip and cleanup; never puts test data on internal storage. */
    public void selfTest() throws IOException {
        DocumentFile test = folder.createFile("application/octet-stream", "test-delete-me.bin");
        if (test == null) throw new IOException("Unable to create test document.");
        try {
            byte[] expected = "gaming-ai-sd-self-test".getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = context.getContentResolver().openOutputStream(test.getUri(), "wt")) {
                if (out == null) throw new IOException("No SD output stream.");
                out.write(expected);
            }
            byte[] actual;
            try (InputStream in = context.getContentResolver().openInputStream(test.getUri())) {
                if (in == null) throw new IOException("No SD input stream.");
                actual = readLimited(in, 128);
            }
            if (!java.util.Arrays.equals(actual, expected)) throw new IOException("SD round-trip mismatch.");
        } finally {
            if (!test.delete()) throw new IOException("Temporary test file could not be deleted.");
        }
    }
}
