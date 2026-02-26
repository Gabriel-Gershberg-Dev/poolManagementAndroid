package com.asgard.pool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asgard.pool.ScheduleHistoryHelper.HistoryEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final List<HistoryEntry> list = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener { void onItemClick(HistoryEntry entry); }

    public void setOnItemClickListener(OnItemClickListener l) { listener = l; }

    public void setEntries(List<HistoryEntry> entries) {
        list.clear();
        if (entries != null) list.addAll(entries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryEntry e = list.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(e.timestamp)));
        int lessons = e.lessons != null ? e.lessons.size() : 0;
        int issues = (e.conflicts != null ? e.conflicts.size() : 0) + (e.gaps != null ? e.gaps.size() : 0);
        String summary = holder.itemView.getContext().getString(R.string.history_summary, lessons, issues > 0 ? issues + " issues" : "OK");
        holder.tvSummary.setText(summary);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(e);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvSummary;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            tvSummary = itemView.findViewById(R.id.tvHistorySummary);
        }
    }
}
