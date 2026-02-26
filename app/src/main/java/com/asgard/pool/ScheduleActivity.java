package com.asgard.pool;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asgard.pool.model.Instructor;
import com.asgard.pool.model.Lesson;
import com.asgard.pool.model.Student;
import com.asgard.pool.model.SwimStyle;
import com.asgard.pool.scheduler.PoolScheduler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ScheduleActivity extends AppCompatActivity {
    public static final String EXTRA_HISTORY_ENTRY_JSON = "history_entry_json";

    private static final int[] WORK_DAYS = {
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
    };

    private PoolScheduler.ScheduleResult scheduleResult;
    private LessonAdapter lessonAdapter;
    private TextView emptyLessons;
    private View recyclerLessons;
    private View calendarScroll;
    private LinearLayout calendarGridContainer;
    private boolean isListView = true;
    private boolean isFromHistory = false;
    private FirebaseHelper firebaseHelper = new FirebaseHelper();

    private static final int[] INSTRUCTOR_COLORS = {
            R.color.instructor_yotam,
            R.color.instructor_yoni,
            R.color.instructor_johnny
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        emptyLessons = findViewById(R.id.emptyLessons);
        recyclerLessons = findViewById(R.id.recyclerLessons);
        calendarScroll = findViewById(R.id.calendarScroll);
        calendarGridContainer = findViewById(R.id.calendarGridContainer);

        RecyclerView recycler = findViewById(R.id.recyclerLessons);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setItemAnimator(new DefaultItemAnimator());
        lessonAdapter = new LessonAdapter(INSTRUCTOR_COLORS);
        lessonAdapter.setOnDeleteListener((lesson, position) -> removeLessonAndPersist(lesson));
        recycler.setAdapter(lessonAdapter);

        String historyJson = getIntent().getStringExtra(EXTRA_HISTORY_ENTRY_JSON);
        if (historyJson != null && !historyJson.isEmpty()) {
            isFromHistory = true;
            try {
                ScheduleHistoryHelper.HistoryEntry entry = new Gson().fromJson(historyJson, ScheduleHistoryHelper.HistoryEntry.class);
                if (entry != null) {
                    scheduleResult = new PoolScheduler.ScheduleResult();
                    scheduleResult.lessons = entry.lessons != null ? new ArrayList<>(entry.lessons) : new ArrayList<>();
                    scheduleResult.conflicts = entry.conflicts != null ? new ArrayList<>(entry.conflicts) : new ArrayList<>();
                    scheduleResult.gaps = entry.gaps != null ? new ArrayList<>(entry.gaps) : new ArrayList<>();
                }
            } catch (Exception e) {
                scheduleResult = new PoolScheduler.ScheduleResult();
            }
        }
        if (scheduleResult == null) {
            @SuppressWarnings({"unchecked", "deprecation"})
            ArrayList<Student> students = (ArrayList<Student>) getIntent().getSerializableExtra("students");
            if (students == null) students = new ArrayList<>();
            scheduleResult = PoolScheduler.schedule(students);
        }

        List<Lesson> sorted = new ArrayList<>(scheduleResult.lessons);
        sorted.sort((a, b) -> {
            if (a.getDayOfWeek() != b.getDayOfWeek()) return a.getDayOfWeek() - b.getDayOfWeek();
            if (a.getStartHour() != b.getStartHour()) return a.getStartHour() - b.getStartHour();
            return a.getStartMinute() - b.getStartMinute();
        });
        lessonAdapter.setLessons(sorted);
        updateEmptyVisibility();

        findViewById(R.id.btnSaveToHistory).setOnClickListener(v -> saveToHistory());

        MaterialButton btnViewToggle = findViewById(R.id.btnViewToggle);
        btnViewToggle.setOnClickListener(v -> {
            if (isListView) switchToCalendarView();
            else switchToListView();
        });
        updateViewToggleIcon();
    }

    private void switchToListView() {
        isListView = true;
        recyclerLessons.setVisibility(View.VISIBLE);
        calendarScroll.setVisibility(View.GONE);
        updateViewToggleIcon();
    }

    private void switchToCalendarView() {
        isListView = false;
        recyclerLessons.setVisibility(View.GONE);
        calendarScroll.setVisibility(View.VISIBLE);
        buildCalendarGrid();
        updateViewToggleIcon();
    }

    private void updateViewToggleIcon() {
        MaterialButton btnViewToggle = findViewById(R.id.btnViewToggle);
        if (isListView) {
            btnViewToggle.setIconResource(R.drawable.ic_view_calendar);
            btnViewToggle.setContentDescription(getString(R.string.view_calendar));
        } else {
            btnViewToggle.setIconResource(R.drawable.ic_view_list);
            btnViewToggle.setContentDescription(getString(R.string.view_list));
        }
    }

    private void buildCalendarGrid() {
        calendarGridContainer.removeAllViews();
        if (scheduleResult == null || scheduleResult.lessons.isEmpty()) {
            return;
        }

        int timeColWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 44, getResources().getDisplayMetrics());
        int cellWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 96, getResources().getDisplayMetrics());
        int rowMinHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        int oneDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        int onSurface = getColor(R.color.md_theme_onSurface);
        int outlineVariant = getColor(R.color.md_theme_outlineVariant);

        // Header row: time label + day names (no block background, on card surface)
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setMinimumHeight(rowMinHeight);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView timeHeader = new TextView(this);
        timeHeader.setWidth(timeColWidth);
        timeHeader.setPadding(12, 12, 12, 12);
        timeHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        timeHeader.setTypeface(null, Typeface.BOLD);
        timeHeader.setTextColor(onSurface);
        headerRow.addView(timeHeader);
        addVerticalDivider(headerRow, oneDp, rowMinHeight, outlineVariant);
        for (int day : WORK_DAYS) {
            TextView dayHeader = new TextView(this);
            dayHeader.setLayoutParams(new LinearLayout.LayoutParams(cellWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            dayHeader.setPadding(12, 12, 12, 12);
            dayHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            dayHeader.setTypeface(null, Typeface.BOLD);
            dayHeader.setTextColor(onSurface);
            dayHeader.setText(PoolScheduler.dayName(day));
            headerRow.addView(dayHeader);
        }
        calendarGridContainer.addView(headerRow);
        addHorizontalDivider(calendarGridContainer, oneDp, outlineVariant);

        for (int hour = 8; hour <= 19; hour++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setMinimumHeight(rowMinHeight);
            row.setGravity(Gravity.TOP);

            TextView timeCell = new TextView(this);
            timeCell.setWidth(timeColWidth);
            timeCell.setPadding(12, 12, 12, 12);
            timeCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            timeCell.setTextColor(onSurface);
            timeCell.setText(hour + ":00");
            row.addView(timeCell);
            addVerticalDivider(row, oneDp, rowMinHeight, outlineVariant);

            for (int day : WORK_DAYS) {
                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setLayoutParams(new LinearLayout.LayoutParams(cellWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
                cell.setPadding(6, 6, 6, 6);
                cell.setMinimumHeight(rowMinHeight - 8);

                int hourStartMin = hour * 60;
                int hourEndMin = (hour + 1) * 60;
                for (Lesson lesson : scheduleResult.lessons) {
                    if (lesson.getDayOfWeek() != day) continue;
                    int lessonStart = lesson.getStartHour() * 60 + lesson.getStartMinute();
                    int lessonEnd = lessonStart + lesson.getDurationMinutes();
                    if (lessonStart < hourEndMin && lessonEnd > hourStartMin) {
                        View chip = createLessonChip(lesson, hour);
                        cell.addView(chip);
                    }
                }
                row.addView(cell);
            }
            calendarGridContainer.addView(row);
            addHorizontalDivider(calendarGridContainer, oneDp, outlineVariant);
        }
    }

    private void addHorizontalDivider(LinearLayout parent, int heightPx, int color) {
        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx));
        line.setBackgroundColor(color);
        parent.addView(line);
    }

    private void addVerticalDivider(LinearLayout row, int widthPx, int minHeight, int color) {
        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(widthPx, minHeight));
        line.setBackgroundColor(color);
        row.addView(line);
    }

    private View createLessonChip(Lesson lesson, int displayHour) {
        View chip = getLayoutInflater().inflate(R.layout.item_calendar_lesson_chip, calendarGridContainer, false);
        chip.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Instructor inst = lesson.getInstructor();

        String timeStr = lesson.getStartHour() + ":" + String.format("%02d", lesson.getStartMinute());
        String title = inst.getName() + " " + timeStr;
        StringBuilder students = new StringBuilder();
        for (Student s : lesson.getStudents()) {
            if (students.length() > 0) students.append(", ");
            students.append(s.getFirstName());
        }
        String studentStr = students.length() > 14 ? students.substring(0, 12) + "…" : students.toString();
        ((TextView) chip.findViewById(R.id.tvChipTitle)).setText(title);
        ((TextView) chip.findViewById(R.id.tvChipStudents)).setText(studentStr);

        chip.findViewById(R.id.btnChipDelete).setOnClickListener(v -> removeLessonAndPersist(lesson));

        chip.setOnClickListener(v -> showLessonDetailsDialog(lesson));
        return chip;
    }

    private void showLessonDetailsDialog(Lesson lesson) {
        Instructor inst = lesson.getInstructor();
        String timeStr = lesson.getStartHour() + ":" + String.format("%02d", lesson.getStartMinute());
        String endStr = lesson.getEndHour() + ":" + String.format("%02d", lesson.getEndMinute());
        String dayName = getDayName(lesson.getDayOfWeek());
        String styleStr = getStyleString(lesson.getStyle());
        String typeStr = lesson.isGroup() ? getString(R.string.group_lesson) : getString(R.string.private_lesson);
        StringBuilder sb = new StringBuilder();
        if (!dayName.isEmpty()) sb.append(dayName).append("\n");
        sb.append(inst != null ? inst.getName() : "").append(" ").append(timeStr).append(" – ").append(endStr)
                .append(" (").append(lesson.getDurationMinutes()).append(" ").append(getString(R.string.minutes_short)).append(")\n");
        sb.append(styleStr).append(" ").append(typeStr).append("\n\n");
        for (Student s : lesson.getStudents()) {
            sb.append(s.getFirstName()).append(" ").append(s.getLastName()).append("\n");
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lesson_details)
                .setMessage(sb.toString().trim())
                .setPositiveButton(R.string.close, null)
                .setNegativeButton(R.string.delete, (dialog, which) -> removeLessonAndPersist(lesson))
                .show();
    }

    private String getDayName(int dayOfWeek) {
        String[] names = getResources().getStringArray(R.array.weekday_names);
        int index = dayOfWeek - Calendar.SUNDAY;
        if (index >= 0 && index < names.length) return names[index];
        return "";
    }

    private String getStyleString(SwimStyle style) {
        if (style == null) return "";
        switch (style) {
            case CRAWL: return getString(R.string.crawl);
            case BREASTSTROKE: return getString(R.string.breaststroke);
            case BUTTERFLY: return getString(R.string.butterfly);
            case BACKSTROKE: return getString(R.string.backstroke);
            default: return style.name();
        }
    }

    private void updateEmptyVisibility() {
        boolean empty = lessonAdapter.getItemCount() == 0;
        emptyLessons.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    /**
     * Removes the lesson from the schedule and, when not viewing history,
     * removes its students from Firestore so the change persists.
     */
    private void removeLessonAndPersist(Lesson lesson) {
        scheduleResult.lessons.remove(lesson);
        List<Lesson> sorted = new ArrayList<>(scheduleResult.lessons);
        sorted.sort((a, b) -> {
            if (a.getDayOfWeek() != b.getDayOfWeek()) return a.getDayOfWeek() - b.getDayOfWeek();
            if (a.getStartHour() != b.getStartHour()) return a.getStartHour() - b.getStartHour();
            return a.getStartMinute() - b.getStartMinute();
        });
        lessonAdapter.setLessons(sorted);
        updateEmptyVisibility();
        if (!isListView) buildCalendarGrid();

        if (!isFromHistory && lesson.getStudents() != null) {
            for (Student s : lesson.getStudents()) {
                String docId = s.getFirestoreId();
                if (docId != null && !docId.isEmpty()) {
                    firebaseHelper.removeStudent(docId, success -> runOnUiThread(() -> {
                        if (!success) Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show();
                    }));
                }
            }
        }
        Toast.makeText(this, R.string.lesson_removed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void saveToHistory() {
        if (scheduleResult == null) return;
        new ScheduleHistoryHelper(this).save(scheduleResult);
        Toast.makeText(this, R.string.saved_to_history, Toast.LENGTH_SHORT).show();
    }
}
