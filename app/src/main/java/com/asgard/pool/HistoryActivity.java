package com.asgard.pool;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asgard.pool.ScheduleHistoryHelper.HistoryEntry;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private ScheduleHistoryHelper historyHelper;
    private HistoryAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        historyHelper = new ScheduleHistoryHelper(this);
        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView recycler = findViewById(R.id.recyclerHistory);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        adapter.setOnItemClickListener(entry -> openScheduleFromHistory(entry));
        recycler.setAdapter(adapter);

        loadHistory();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        List<HistoryEntry> entries = historyHelper.loadAll();
        adapter.setEntries(entries);
        tvEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openScheduleFromHistory(HistoryEntry entry) {
        String json = new com.google.gson.Gson().toJson(entry);
        Intent i = new Intent(this, ScheduleActivity.class);
        i.putExtra(ScheduleActivity.EXTRA_HISTORY_ENTRY_JSON, json);
        startActivity(i);
    }
}
