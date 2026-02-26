package com.asgard.pool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asgard.pool.model.LessonRequest;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/** Adapter for manager's pending lesson requests (Assign / Decline). */
public class PendingRequestAdapter extends RecyclerView.Adapter<PendingRequestAdapter.ViewHolder> {

    private final List<LessonRequest> list = new ArrayList<>();
    private OnAssignListener onAssignListener;
    private OnDeclineListener onDeclineListener;

    public interface OnAssignListener { void onAssign(LessonRequest request); }
    public interface OnDeclineListener { void onDecline(LessonRequest request); }

    public void setOnAssignListener(OnAssignListener l) { onAssignListener = l; }
    public void setOnDeclineListener(OnDeclineListener l) { onDeclineListener = l; }

    public void setRequests(List<LessonRequest> requests) {
        list.clear();
        if (requests != null) list.addAll(requests);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LessonRequest r = list.get(position);
        String name = r.getUserName() != null ? r.getUserName() : "";
        if (name.isEmpty() && r.getUserId() != null) name = "User";
        holder.tvRequestName.setText(name);
        holder.btnAssign.setOnClickListener(v -> {
            if (onAssignListener != null) onAssignListener.onAssign(r);
        });
        holder.btnDecline.setOnClickListener(v -> {
            if (onDeclineListener != null) onDeclineListener.onDecline(r);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRequestName;
        MaterialButton btnAssign, btnDecline;

        ViewHolder(View itemView) {
            super(itemView);
            tvRequestName = itemView.findViewById(R.id.tvRequestName);
            btnAssign = itemView.findViewById(R.id.btnAssign);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}
