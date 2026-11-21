package com.valentinebsaa.gamingai;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

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
 * Manual unified export/restore. User data never falls back to app-internal files.
 * The two legacy snapshot formats remain unchanged and independently recoverable.
 */
public final class CombinedBackupStore {
    private static final int MAX_EXPORT_BYTES = MemoryPolicy.MAX_BYTES + 65536;
    private final Context context;
    private final Uri folderUri;
    private final DocumentFile folder;
    private final MemoryStore memory;
    private final GameProfileStore profiles;

    public static final class Preview {
        public final List<MemoryRecord> memories;
        public final List<String> customGames;
        public final String checksum;
        public final long exportedAt;

        Preview(List<MemoryRecord> memories, List<String> customGames, String checksum,
                long exportedAt) {
            this.memories = java.util.Collections.unmodifiableList(new ArrayList<>(memories));
            this.customGames = java.util.Collections.unmodifiableList(new ArrayList<>(customGames));
            this.checksum = checksum;
            this.exportedAt = exportedAt;
        }

        public int pinnedCount() {
            int count = 0;
            for (MemoryRecord record : memories) if (record.pinned) count++;
            return count;
        }
    }

    public CombinedBackupStore(Context context, Uri treeUri) throws IOException {
        if (treeUri == null || !StorageRules.isRemovableSdSubfolder(treeUri.getAuthority(),
                DocumentsContract.getTreeDocumentId(treeUri))) {
            throw new IOException("Select a verified removable microSD subfolder first.");
        }
        this.context = context.getApplicationContext();
        this.folderUri = treeUri;
        this.folder = DocumentFile.fromTreeUri(context, treeUri);
        if (folder == null || !folder.exists() || !folder.isDirectory()
                || !folder.canRead() || !folder.canWrite()) {
            throw new IOException("microSD unavailable or folder permission revoked.");
        }
        this.memory = new MemoryStore(context, treeUri);
        this.profiles = new GameProfileStore(context, treeUri);
    }

    private static byte[] boundedRead(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = input.read(chunk)) != -1) {
            if (buffer.size() + n > MAX_EXPORT_BYTES)
                throw new IOException("Backup exceeds the 320 KiB safety limit.");
            buffer.write(chunk, 0, n);
        }
        return buffer.toByteArray();
    }

    public Preview inspect(Uri document) throws IOException {
        if (document == null) throw new IOException("No JSON backup selected.");
        try (InputStream input = context.getContentResolver().openInputStream(document)) {
            if (input == null) throw new IOException("Unable to read selected backup.");
            JSONObject root = new JSONObject(new String(boundedRead(input),
                StandardCharsets.UTF_8));
            if (!CombinedBackupRules.FORMAT.equals(root.getString("format"))
                    || root.getInt("version") != CombinedBackupRules.VERSION) {
                throw new IOException("This is not a supported unified Gaming AI backup.");
            }
            JSONArray notesJson = root.getJSONArray("memories");
            JSONArray gamesJson = root.getJSONArray("customGames");
            if (notesJson.length() > MemoryPolicy.MAX_ENTRIES
                    || gamesJson.length() > GameProfilePolicy.MAX_CUSTOM)
                throw new IOException("Backup exceeds entry limits.");
            List<MemoryRecord> notes = new ArrayList<>();
            List<String> games = new ArrayList<>();
            for (int i = 0; i < notesJson.length(); i++) {
                notes.add(MemoryRecord.fromJson(notesJson.getJSONObject(i)));
            }
            for (int i = 0; i < gamesJson.length(); i++) {
                games.add(gamesJson.getString(i));
            }
            try { CombinedBackupRules.validate(notes, games); }
            catch (IllegalArgumentException unsafe) {
                throw new IOException("Backup contains invalid data: " + unsafe.getMessage(), unsafe);
            }
            String checksum = root.getString("sha256");
            String calculated = CombinedBackupRules.sha256(notes, games);
            if (!calculated.equals(checksum)) {
                throw new IOException("Backup failed SHA-256 integrity verification.");
            }
            return new Preview(notes, games, checksum, root.getLong("exportedAt"));
        } catch (JSONException malformed) {
            throw new IOException("Invalid or incomplete unified backup JSON.", malformed);
        } catch (SecurityException denied) {
            throw new IOException("Permission to read that backup was denied.", denied);
        }
    }

    /**
     * Export current data, then read back/verify the new file before claiming success.
     * Existing user files and note snapshots are not changed.
     */
    public String create() throws IOException {
        List<MemoryRecord> notes = memory.load();
        List<String> names = profiles.load();
        try { CombinedBackupRules.validate(notes, names); }
        catch (IllegalArgumentException unsafe) {
            throw new IOException("Existing data failed validation: " + unsafe.getMessage(), unsafe);
        }
        String checksum = CombinedBackupRules.sha256(notes, names);
        byte[] bytes;
        try {
            JSONObject root = new JSONObject();
            root.put("format", CombinedBackupRules.FORMAT);
            root.put("version", CombinedBackupRules.VERSION);
            root.put("exportedAt", System.currentTimeMillis());
            root.put("sha256", checksum);
            JSONArray memories = new JSONArray();
            for (MemoryRecord note : notes) memories.put(note.toJson());
            JSONArray games = new JSONArray();
            for (String game : names) games.put(game);
            root.put("memories", memories);
            root.put("customGames", games);
            bytes = root.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JSONException error) {
            throw new IOException("Cannot encode unified backup.", error);
        }
        if (bytes.length > MAX_EXPORT_BYTES)
            throw new IOException("Unified backup exceeds allowed export size.");

        String filename = "gaming-ai-complete-" + System.currentTimeMillis() + ".json";
        DocumentFile file = folder.createFile("application/json", filename);
        if (file == null) throw new IOException("Cannot create unified backup on microSD.");
        try {
            try (OutputStream output = context.getContentResolver()
                    .openOutputStream(file.getUri(), "wt")) {
                if (output == null) throw new IOException("Cannot open microSD backup output.");
                output.write(bytes);
                output.flush();
            }
            Preview check = inspect(file.getUri());
            if (!check.checksum.equals(checksum)
                    || check.memories.size() != notes.size()
                    || check.customGames.size() != names.size()) {
                throw new IOException("Unified backup write/read verification failed.");
            }
            return file.getName() == null ? filename : file.getName();
        } catch (Exception failed) {
            // Delete only our newly-created failed export, never other user files.
            file.delete();
            if (failed instanceof IOException) throw (IOException) failed;
            throw new IOException("Backup verification failed.", failed);
        }
    }

    /**
     * Call ONLY after the user previews and confirms the checksum-verified data.
     * A recovery export is made before any mutation. Restore is best-effort
     * transactional across two independent microSD snapshot families.
     */
    public String restore(Preview approved) throws IOException {
        if (approved == null) throw new IOException("Preview and approve a backup first.");
        try { CombinedBackupRules.validate(approved.memories, approved.customGames); }
        catch (IllegalArgumentException invalid) {
            throw new IOException("Backup content is invalid.", invalid);
        }
        if (!CombinedBackupRules.sha256(approved.memories, approved.customGames)
                .equals(approved.checksum)) {
            throw new IOException("Restore preview checksum no longer matches its content.");
        }
        List<MemoryRecord> previousNotes = memory.load();
        List<String> previousGames = profiles.load();
        String rescueName = create(); // Must succeed before we touch either live snapshot.
        try {
            profiles.save(approved.customGames);
            memory.save(approved.memories);
            String verified = CombinedBackupRules.sha256(
                memory.load(), profiles.load());
            if (!verified.equals(approved.checksum))
                throw new IOException("Restored data failed final verification.");
            return "Restored " + approved.memories.size() + " notes and "
                + approved.customGames.size() + " custom games. "
                + "Emergency previous-data export: " + rescueName;
        } catch (Exception failure) {
            boolean rollbackOK = false;
            try {
                // Re-snapshot both old datasets. Even if one write failed,
                // leave an on-card emergency JSON file for manual restoration.
                profiles.save(previousGames);
                memory.save(previousNotes);
                rollbackOK = CombinedBackupRules.sha256(memory.load(), profiles.load())
                    .equals(CombinedBackupRules.sha256(previousNotes, previousGames));
            } catch (Exception ignored) {
                rollbackOK = false;
            }
            throw new IOException(rollbackOK
                ? "Restore failed; prior data was restored and verified. "
                    + "Recovery file: " + rescueName
                : "Restore failed and rollback could not be verified. "
                    + "DO NOT modify this card; recover using: " + rescueName, failure);
        }
    }
}
