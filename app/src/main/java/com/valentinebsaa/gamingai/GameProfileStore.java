package com.valentinebsaa.gamingai;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.documentfile.provider.DocumentFile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Two alternating custom-profile snapshots; only SAF-selected removable SD is used. */
public final class GameProfileStore {
    private static final String SLOT_A = "game-profiles-a.json";
    private static final String SLOT_B = "game-profiles-b.json";
    private static final int MAX_FILE_BYTES = 16384;
    private final Context context;
    private final DocumentFile directory;

    private static final class Snapshot {
        final long revision;
        final List<String> names;
        Snapshot(long revision, List<String> names) {
            this.revision = revision;
            this.names = names;
        }
    }

    public GameProfileStore(Context context, Uri selectedTree) throws IOException {
        if (selectedTree == null || !StorageRules.isRemovableSdSubfolder(
                selectedTree.getAuthority(), DocumentsContract.getTreeDocumentId(selectedTree))) {
            throw new IOException("Choose a removable microSD subfolder first.");
        }
        this.context = context.getApplicationContext();
        this.directory = DocumentFile.fromTreeUri(context, selectedTree);
        if (directory == null || !directory.exists() || !directory.isDirectory()
                || !directory.canRead() || !directory.canWrite()) {
            throw new IOException("microSD missing, read-only or permission revoked.");
        }
    }

    private static byte[] bounded(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int count;
        while ((count = in.read(buf)) != -1) {
            if (out.size() + count > MAX_FILE_BYTES)
                throw new IOException("Game profile index exceeds safe limit.");
            out.write(buf, 0, count);
        }
        return out.toByteArray();
    }

    private Snapshot read(DocumentFile file) {
        if (file == null) return null;
        try (InputStream stream = context.getContentResolver().openInputStream(file.getUri())) {
            if (stream == null) return null;
            JSONObject root = new JSONObject(
                new String(bounded(stream), StandardCharsets.UTF_8));
            long revision = root.getLong("revision");
            if (revision < 1) return null;
            JSONArray array = root.getJSONArray("customGames");
            if (array.length() > GameProfilePolicy.MAX_CUSTOM) return null;
            ArrayList<String> titles = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) titles.add(array.getString(i));
            List<String> validated = GameProfilePolicy.validateCustom(titles);
            // Compatibility: previously created snapshots did not have a checksum.
            String checksum = CombinedBackupRules.sha256(new ArrayList<>(), validated);
            if (root.has("sha256") && !checksum.equals(root.getString("sha256"))) return null;
            return new Snapshot(revision, validated);
        } catch (Exception invalid) {
            return null; // Recover from the other valid snapshot; do not erase either.
        }
    }

    private Snapshot latest() throws IOException {
        DocumentFile a = directory.findFile(SLOT_A), b = directory.findFile(SLOT_B);
        Snapshot first = read(a), second = read(b);
        if (first == null && second == null) {
            if (a != null || b != null)
                throw new IOException("Both game profile snapshots are unreadable.");
            return new Snapshot(0, new ArrayList<>());
        }
        if (first == null) return second;
        if (second == null) return first;
        return first.revision >= second.revision ? first : second;
    }

    public List<String> load() throws IOException {
        return new ArrayList<>(latest().names);
    }

    public void save(List<String> custom) throws IOException {
        final List<String> validated;
        try {
            validated = GameProfilePolicy.validateCustom(custom);
        } catch (IllegalArgumentException badName) {
            throw new IOException(badName.getMessage(), badName);
        }
        Snapshot old = latest();
        long next = old.revision + 1;
        try {
            JSONObject root = new JSONObject();
            root.put("revision", next);
            root.put("sha256", CombinedBackupRules.sha256(new ArrayList<>(), validated));
            JSONArray names = new JSONArray();
            for (String title : validated) names.put(title);
            root.put("customGames", names);
            byte[] encoded = root.toString().getBytes(StandardCharsets.UTF_8);
            if (encoded.length > MAX_FILE_BYTES) throw new IOException("Profile index too large.");
            String target = (next % 2 == 1) ? SLOT_A : SLOT_B;
            DocumentFile file = directory.findFile(target);
            if (file == null) file = directory.createFile("application/json", target);
            if (file == null) throw new IOException("Cannot create microSD profile index.");
            try (OutputStream stream = context.getContentResolver()
                    .openOutputStream(file.getUri(), "wt")) {
                if (stream == null) throw new IOException("Cannot write microSD game profiles.");
                stream.write(encoded);
                stream.flush();
            }
            Snapshot verified = read(file);
            if (verified == null || verified.revision != next
                    || !verified.names.equals(validated)) {
                throw new IOException("Game profile verification failed; earlier snapshot retained.");
            }
        } catch (org.json.JSONException error) {
            throw new IOException("Unable to encode game profiles.", error);
        }
    }
}
