package com.asgard.pool;

import android.content.Context;
import android.content.SharedPreferences;

import com.asgard.pool.model.Lesson;
import com.asgard.pool.scheduler.PoolScheduler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class ScheduleHistoryHelper {
    private static final String PREFS_NAME = "schedule_history";
    private static final String KEY_ENTRIES = "history_entries";
    private static final int MAX_ENTRIES = 50;

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private static final Type LIST_STRING = new TypeToken<List<String>>() {}.getType();

    public ScheduleHistoryHelper(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static final class HistoryEntry {
        public long timestamp;
        public List<Lesson> lessons;
        public List<String> conflicts;
        public List<String> gaps;

        public HistoryEntry() {}

        public HistoryEntry(long timestamp, List<Lesson> lessons, List<String> conflicts, List<String> gaps) {
            this.timestamp = timestamp;
            this.lessons = lessons != null ? new ArrayList<>(lessons) : new ArrayList<>();
            this.conflicts = conflicts != null ? new ArrayList<>(conflicts) : new ArrayList<>();
            this.gaps = gaps != null ? new ArrayList<>(gaps) : new ArrayList<>();
        }
    }

    public void save(PoolScheduler.ScheduleResult result) {
        if (result == null) return;
        List<String> jsonList = loadRawEntries();
        HistoryEntry entry = new HistoryEntry(
                System.currentTimeMillis(),
                result.lessons,
                result.conflicts,
                result.gaps
        );
        String json = gson.toJson(entry);
        jsonList.add(0, json);
        while (jsonList.size() > MAX_ENTRIES) jsonList.remove(jsonList.size() - 1);
        prefs.edit().putString(KEY_ENTRIES, gson.toJson(jsonList)).apply();
    }

    public List<HistoryEntry> loadAll() {
        List<String> raw = loadRawEntries();
        List<HistoryEntry> out = new ArrayList<>();
        for (String json : raw) {
            try {
                HistoryEntry e = gson.fromJson(json, HistoryEntry.class);
                if (e != null) {
                    if (e.lessons == null) e.lessons = new ArrayList<>();
                    if (e.conflicts == null) e.conflicts = new ArrayList<>();
                    if (e.gaps == null) e.gaps = new ArrayList<>();
                    out.add(e);
                }
            } catch (Exception ignore) { }
        }
        return out;
    }

    private List<String> loadRawEntries() {
        String raw = prefs.getString(KEY_ENTRIES, "[]");
        try {
            List<String> list = gson.fromJson(raw, LIST_STRING);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void clear() {
        prefs.edit().remove(KEY_ENTRIES).apply();
    }
}
