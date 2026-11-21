package com.valentinebsaa.gamingai;

import static org.junit.Assert.*;
import org.junit.Test;

public final class StorageRulesTest {
    @Test public void acceptsRemovableSdSubfolder() {
        assertTrue(StorageRules.isRemovableSdSubfolder(
            "com.android.externalstorage.documents", "1234-ABCD:GamingAI"));
    }

    @Test public void rejectsInternalStorageAndRoot() {
        assertFalse(StorageRules.isRemovableSdSubfolder(
            "com.android.externalstorage.documents", "primary:GamingAI"));
        assertFalse(StorageRules.isRemovableSdSubfolder(
            "com.android.externalstorage.documents", "1234-ABCD:"));
    }

    @Test public void rejectsCloudAndUnrecognizedProvider() {
        assertFalse(StorageRules.isRemovableSdSubfolder(
            "com.google.android.apps.docs.storage", "1234-ABCD:GamingAI"));
        assertFalse(StorageRules.isRemovableSdSubfolder(
            "com.android.externalstorage.documents", "mysterious:Folder"));
    }
}
