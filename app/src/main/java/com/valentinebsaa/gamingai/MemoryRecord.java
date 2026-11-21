package com.valentinebsaa.gamingai;

import org.json.JSONException;
import org.json.JSONObject;

public final class MemoryRecord {
    public final String id;
    public final long when;
    public final String game;
    public final String note;
    public final boolean pinned;

    public MemoryRecord(String id, long when, String game, String note, boolean pinned) {
        this.id = id;
        this.when = when;
        this.game = game;
        this.note = note;
        this.pinned = pinned;
    }

    JSONObject toJson() throws JSONException {
        JSONObject out = new JSONObject();
        out.put("id", id);
        out.put("when", when);
        out.put("game", game);
        out.put("note", note);
        out.put("pinned", pinned);
        return out;
    }

    static MemoryRecord fromJson(JSONObject source) throws JSONException {
        return new MemoryRecord(
            source.getString("id"),
            source.getLong("when"),
            source.getString("game"),
            source.getString("note"),
            source.optBoolean("pinned", false)
        );
    }
}
