package com.valentinebsaa.gamingai;

import java.util.Locale;

/** Deliberately conservative. Deny ambiguous providers instead of writing user data to internal storage. */
public final class StorageRules {
    private StorageRules() {}

    public static boolean isRemovableSdSubfolder(String authority, String treeDocumentId) {
        if (!"com.android.externalstorage.documents".equals(authority) || treeDocumentId == null) {
            return false;
        }
        int colon = treeDocumentId.indexOf(':');
        if (colon < 1 || colon == treeDocumentId.length() - 1) return false;
        String volume = treeDocumentId.substring(0, colon).toLowerCase(Locale.ROOT);
        String folder = treeDocumentId.substring(colon + 1).trim();
        if (volume.equals("primary") || volume.equals("home") || folder.isEmpty()) return false;
        // FAT/exFAT removable microSD is typically exposed as XXXX-XXXX. Unknown IDs fail closed.
        return volume.matches("[0-9a-f]{4}-[0-9a-f]{4}") && !folder.equals("/");
    }
}
