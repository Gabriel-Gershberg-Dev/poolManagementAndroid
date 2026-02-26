package com.asgard.pool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asgard.pool.model.LessonType;
import com.asgard.pool.model.Student;
import com.asgard.pool.model.SwimStyle;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {
    private final List<Student> list = new ArrayList<>();
    private OnDeleteListener onDeleteListener;

    public interface OnDeleteListener { void onDelete(Student s, int position); }

    public void setOnDeleteListener(OnDeleteListener l) { onDeleteListener = l; }

    public void setStudents(List<Student> students) {
        list.clear();
        if (students != null) list.addAll(students);
        notifyDataSetChanged();
    }

    public void addStudent(Student s) {
        if (list.size() >= 30) return;
        list.add(s);
        notifyItemInserted(list.size() - 1);
    }

    public void removeAt(int position) {
        if (position >= 0 && position < list.size()) {
            list.remove(position);
            notifyItemRemoved(position);
        }
    }

    /** Remove a student by reference (e.g. from a sheet that has a copy of the list). */
    public boolean removeStudent(Student s) {
        for (int i = 0; i < list.size(); i++) {
            Student item = list.get(i);
            if (item == s || (item.getFirstName().equals(s.getFirstName()) && item.getLastName().equals(s.getLastName()))) {
                list.remove(i);
                notifyItemRemoved(i);
                return true;
            }
        }
        return false;
    }

    public List<Student> getStudents() { return new ArrayList<>(list); }
    public int getCount() { return list.size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student s = list.get(position);
        holder.tvName.setText(s.getFullName());
        holder.tvStyle.setText(styleStr(s.getSwimStyle()));
        holder.tvLessonType.setText(lessonTypeStr(s.getLessonType()));
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || onDeleteListener == null) return;
            onDeleteListener.onDelete(s, pos);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    private static String styleStr(SwimStyle style) {
        switch (style) {
            case CRAWL: return "Crawl";
            case BREASTSTROKE: return "Breaststroke";
            case BUTTERFLY: return "Butterfly";
            case BACKSTROKE: return "Backstroke";
            default: return style.name();
        }
    }

    private static String lessonTypeStr(LessonType t) {
        switch (t) {
            case PRIVATE_ONLY: return "Private only";
            case GROUP_ONLY: return "Group only";
            case PRIVATE_OR_GROUP: return "Private or group";
            default: return t.name();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStyle, tvLessonType;
        Button btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvStyle = itemView.findViewById(R.id.tvStyle);
            tvLessonType = itemView.findViewById(R.id.tvLessonType);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
