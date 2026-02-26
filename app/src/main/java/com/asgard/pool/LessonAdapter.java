package com.asgard.pool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asgard.pool.model.Instructor;
import com.asgard.pool.model.Lesson;
import com.asgard.pool.model.Student;
import com.asgard.pool.model.SwimStyle;
import com.asgard.pool.scheduler.PoolScheduler;

import java.util.ArrayList;
import java.util.List;

public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.ViewHolder> {

    private final List<Lesson> lessons = new ArrayList<>();
    private final int[] instructorColors;
    private OnDeleteLessonListener onDeleteListener;

    public interface OnDeleteLessonListener {
        void onDelete(Lesson lesson, int position);
    }

    public void setOnDeleteListener(OnDeleteLessonListener l) {
        onDeleteListener = l;
    }

    public LessonAdapter(int[] instructorColorResIds) {
        this.instructorColors = instructorColorResIds;
    }

    public void setLessons(List<Lesson> newLessons) {
        lessons.clear();
        if (newLessons != null) lessons.addAll(newLessons);
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        if (position >= 0 && position < lessons.size()) {
            lessons.remove(position);
            notifyItemRemoved(position);
        }
    }

    public List<Lesson> getLessons() {
        return new ArrayList<>(lessons);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gantt_lesson, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Lesson lesson = lessons.get(position);
        Instructor inst = lesson.getInstructor();
        int colorRes = instructorColors[inst.ordinal() % instructorColors.length];
        holder.accentBar.setBackgroundColor(holder.itemView.getContext().getColor(colorRes));

        holder.tvTitle.setText(inst.getName() + " – " + styleName(lesson.getStyle())
                + (lesson.isGroup() ? " " + holder.itemView.getContext().getString(R.string.group_lesson)
                : " " + holder.itemView.getContext().getString(R.string.private_lesson)));
        holder.tvTime.setText(PoolScheduler.dayName(lesson.getDayOfWeek()) + " "
                + lesson.getStartHour() + ":" + String.format("%02d", lesson.getStartMinute())
                + " – " + lesson.getDurationMinutes() + " " + holder.itemView.getContext().getString(R.string.minutes_short));
        StringBuilder sb = new StringBuilder();
        for (Student s : lesson.getStudents()) sb.append(s.getFullName()).append(", ");
        if (sb.length() > 0) sb.setLength(sb.length() - 2);
        holder.tvStudents.setText(sb.toString());

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || onDeleteListener == null) return;
            onDeleteListener.onDelete(lesson, pos);
        });
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    private static String styleName(SwimStyle s) {
        switch (s) {
            case CRAWL: return "Crawl";
            case BREASTSTROKE: return "Breaststroke";
            case BUTTERFLY: return "Butterfly";
            case BACKSTROKE: return "Backstroke";
            default: return s.name();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View accentBar;
        TextView tvTitle, tvTime, tvStudents;
        com.google.android.material.button.MaterialButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            accentBar = itemView.findViewById(R.id.accentBar);
            tvTitle = itemView.findViewById(R.id.tvGanttTitle);
            tvTime = itemView.findViewById(R.id.tvGanttTime);
            tvStudents = itemView.findViewById(R.id.tvGanttStudents);
            btnDelete = itemView.findViewById(R.id.btnDeleteLesson);
        }
    }
}
