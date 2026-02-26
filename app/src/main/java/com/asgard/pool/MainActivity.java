package com.asgard.pool;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asgard.pool.model.Instructor;
import com.asgard.pool.model.Lesson;
import com.asgard.pool.model.LessonRequest;
import com.asgard.pool.model.LessonType;
import com.asgard.pool.model.Student;
import com.asgard.pool.model.SwimStyle;
import com.asgard.pool.scheduler.PoolScheduler;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.text.TextUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "asgard_pool";
    private static final String KEY_STUDENTS = "students";
    private static final Type STUDENT_LIST_TYPE = new TypeToken<ArrayList<Student>>() {}.getType();

    private StudentAdapter adapter;
    private PendingRequestAdapter pendingAdapter;
    private TextView tvCapacity;
    private TextView tvYotam, tvYoni, tvJohnny;
    private TextView tvNoPending;
    private RecyclerView recyclerPendingRequests;
    private SharedPreferences prefs;
    private Gson gson = new Gson();
    private FirebaseHelper firebaseHelper = new FirebaseHelper();
    private static final int MAX_STUDENTS = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        tvCapacity = findViewById(R.id.tvCapacity);
        tvYotam = findViewById(R.id.tvYotam);
        tvYoni = findViewById(R.id.tvYoni);
        tvJohnny = findViewById(R.id.tvJohnny);
        tvNoPending = findViewById(R.id.tvNoPending);
        recyclerPendingRequests = findViewById(R.id.recyclerPendingRequests);

        adapter = new StudentAdapter();
        adapter.setOnDeleteListener((s, pos) -> removeStudentAt(pos));

        pendingAdapter = new PendingRequestAdapter();
        pendingAdapter.setOnAssignListener(this::onAssignRequest);
        pendingAdapter.setOnDeclineListener(this::onDeclineRequest);
        recyclerPendingRequests.setLayoutManager(new LinearLayoutManager(this));
        recyclerPendingRequests.setAdapter(pendingAdapter);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        firebaseHelper.ensurePoolExists(null);
        loadStudentsFromFirestore();
        loadPendingRequests();

        findViewById(R.id.btnHistory).setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.btnAccount).setOnClickListener(v -> openAccountDialog());
        findViewById(R.id.btnAddStudent).setOnClickListener(v -> openAddStudentDialog());
        findViewById(R.id.btnShowSchedule).setOnClickListener(v -> openSchedule());

        refreshStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            loadStudentsFromFirestore();
            loadPendingRequests();
        }
    }

    private void loadStudentsFromFirestore() {
        firebaseHelper.loadStudents(new FirebaseHelper.OnStudentsLoadedListener() {
            @Override
            public void onLoaded(List<Student> students) {
                runOnUiThread(() -> {
                    adapter.setStudents(students);
                    saveStudentsToPrefs();
                    refreshStats();
                });
            }
            @Override
            public void onError(Throwable e) {
                runOnUiThread(() -> {
                    String msg = e != null && e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    boolean permissionDenied = msg.contains("permission") || msg.contains("denied");
                    if (permissionDenied) {
                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle(R.string.load_pool_permission_denied_dialog_title)
                                .setMessage(R.string.load_pool_permission_denied_dialog_message)
                                .setPositiveButton(R.string.close, null)
                                .show();
                    } else {
                        Toast.makeText(MainActivity.this, R.string.load_pool_failed, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void loadPendingRequests() {
        firebaseHelper.loadLessonRequests(null, requests -> runOnUiThread(() -> {
            List<LessonRequest> pending = new ArrayList<>();
            if (requests != null) {
                for (LessonRequest r : requests) {
                    if (LessonRequest.STATUS_PENDING.equals(r.getStatus())) pending.add(r);
                }
            }
            pendingAdapter.setRequests(pending);
            boolean empty = pending.isEmpty();
            tvNoPending.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerPendingRequests.setVisibility(empty ? View.GONE : View.VISIBLE);
        }));
    }

    private void onAssignRequest(LessonRequest request) {
        if (adapter.getCount() >= MAX_STUDENTS) {
            Toast.makeText(this, R.string.max_students_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        String first = request.getFirstName();
        String last = request.getLastName();
        if (TextUtils.isEmpty(first) || TextUtils.isEmpty(last)) {
            String fullName = request.getUserName() != null ? request.getUserName().trim() : "";
            first = fullName;
            last = "";
            if (!fullName.isEmpty()) {
                int space = fullName.indexOf(' ');
                if (space > 0) {
                    first = fullName.substring(0, space).trim();
                    last = fullName.substring(space).trim();
                }
            }
            if (TextUtils.isEmpty(first)) first = "Student";
        }
        SwimStyle style = parseSwimStyle(request.getSwimStyle());
        LessonType lessonType = parseLessonType(request.getLessonType());
        Student newStudent = new Student(first, last, style, lessonType);
        newStudent.setUserId(request.getUserId());
        List<Student> wouldBeList = new ArrayList<>(adapter.getStudents());
        wouldBeList.add(newStudent);
        PoolScheduler.ScheduleResult result = PoolScheduler.schedule(wouldBeList);
        if (!result.conflicts.isEmpty() || !result.gaps.isEmpty()) {
            List<String> lines = new ArrayList<>();
            if (!result.conflicts.isEmpty()) {
                lines.add(getString(R.string.conflicts_header));
                lines.addAll(result.conflicts);
            }
            if (!result.gaps.isEmpty()) {
                if (!lines.isEmpty()) lines.add("");
                lines.add(getString(R.string.gaps_header));
                lines.addAll(result.gaps);
            }
            String message = getString(R.string.conflicts_alert_message) + "\n\n" + android.text.TextUtils.join("\n", lines);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.conflicts_alert_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.close, null)
                    .show();
            return;
        }
        firebaseHelper.addStudent(newStudent, success -> runOnUiThread(() -> {
            if (!success) {
                Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            adapter.addStudent(newStudent);
            saveStudentsToPrefs();
            refreshStats();
            firebaseHelper.updateLessonRequestStatus(request.getFirestoreId(), LessonRequest.STATUS_ASSIGNED, null, done -> runOnUiThread(() -> {
                if (done) Toast.makeText(this, R.string.request_assigned, Toast.LENGTH_SHORT).show();
                loadPendingRequests();
            }));
        }));
    }

    private static SwimStyle parseSwimStyle(String name) {
        if (name == null) return SwimStyle.CRAWL;
        try { return SwimStyle.valueOf(name); } catch (Exception e) { return SwimStyle.CRAWL; }
    }

    private static LessonType parseLessonType(String name) {
        if (name == null) return LessonType.PRIVATE_ONLY;
        try { return LessonType.valueOf(name); } catch (Exception e) { return LessonType.PRIVATE_ONLY; }
    }

    private void onDeclineRequest(LessonRequest request) {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(R.string.decline_reason);
        input.setSingleLine(false);
        input.setMinLines(2);
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(50, 40, 50, 10);
        container.addView(input);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.decline)
                .setView(container)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String reason = input.getText() != null ? input.getText().toString().trim() : null;
                    if (TextUtils.isEmpty(reason)) reason = null;
                    firebaseHelper.updateLessonRequestStatus(request.getFirestoreId(), LessonRequest.STATUS_DECLINED, reason, done -> runOnUiThread(() -> {
                        if (done) Toast.makeText(this, R.string.request_declined, Toast.LENGTH_SHORT).show();
                        loadPendingRequests();
                    }));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void loadStudentsFromPrefs() {
        List<Student> saved = loadStudentsFromPrefsList();
        if (!saved.isEmpty()) adapter.setStudents(saved);
        refreshStats();
    }

    private List<Student> loadStudentsFromPrefsList() {
        String json = prefs.getString(KEY_STUDENTS, "[]");
        try {
            List<Student> list = gson.fromJson(json, STUDENT_LIST_TYPE);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveStudentsToPrefs() {
        List<Student> list = adapter.getStudents();
        prefs.edit().putString(KEY_STUDENTS, gson.toJson(list)).apply();
    }

    private void removeStudentAt(int pos) {
        List<Student> list = adapter.getStudents();
        if (pos < 0 || pos >= list.size()) return;
        Student s = list.get(pos);
        String docId = s.getFirestoreId();
        adapter.removeAt(pos);
        saveStudentsToPrefs();
        refreshStats();
        if (docId != null && !docId.isEmpty()) {
            firebaseHelper.removeStudent(docId, success -> runOnUiThread(() -> {
                if (!success) Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show();
            }));
        }
    }

    private void refreshStats() {
        int n = adapter.getCount();
        tvCapacity.setText(getString(R.string.capacity_value, n));
        if (n == 0) {
            tvYotam.setText(getString(R.string.trainer_count, "Yotam", 0));
            tvYoni.setText(getString(R.string.trainer_count, "Yoni", 0));
            tvJohnny.setText(getString(R.string.trainer_count, "Johnny", 0));
            return;
        }
        PoolScheduler.ScheduleResult result = PoolScheduler.schedule(adapter.getStudents());
        int yotam = 0, yoni = 0, johnny = 0;
        for (Lesson lesson : result.lessons) {
            switch (lesson.getInstructor()) {
                case YOTAM: yotam++; break;
                case YONI: yoni++; break;
                case JOHNNY: johnny++; break;
            }
        }
        tvYotam.setText(getString(R.string.trainer_count, "Yotam", yotam));
        tvYoni.setText(getString(R.string.trainer_count, "Yoni", yoni));
        tvJohnny.setText(getString(R.string.trainer_count, "Johnny", johnny));
    }

    private void openAddStudentDialog() {
        if (adapter.getCount() >= MAX_STUDENTS) {
            Toast.makeText(this, R.string.max_students_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_student, null);

        String[] styles = {getString(R.string.crawl), getString(R.string.breaststroke), getString(R.string.butterfly), getString(R.string.backstroke)};
        String[] types = {getString(R.string.private_only), getString(R.string.group_only), getString(R.string.private_or_group)};

        MaterialAutoCompleteTextView dropdownStyle = dialogView.findViewById(R. id.dropdownStyle);
        MaterialAutoCompleteTextView dropdownLessonType = dialogView.findViewById(R.id.dropdownLessonType);

        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, styles);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, types);
        dropdownStyle.setAdapter(styleAdapter);
        dropdownLessonType.setAdapter(typeAdapter);

        dropdownStyle.setOnItemClickListener((parent, view, position, id) -> dropdownStyle.setTag(position));
        dropdownLessonType.setOnItemClickListener((parent, view, position, id) -> dropdownLessonType.setTag(position));
        dropdownStyle.setText(styles[0], false);
        dropdownLessonType.setText(types[0], false);
        dropdownStyle.setTag(0);
        dropdownLessonType.setTag(0);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String first = ((TextView) dialogView.findViewById(R.id.etFirstName)).getText().toString().trim();
            String last = ((TextView) dialogView.findViewById(R.id.etLastName)).getText().toString().trim();
            if (first.isEmpty() || last.isEmpty()) {
                Toast.makeText(this, R.string.enter_name_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            Object styleTag = dropdownStyle.getTag();
            Object typeTag = dropdownLessonType.getTag();
            int styleIndex = 0;
            int typeIndex = 0;
            if (styleTag instanceof Integer) styleIndex = (Integer) styleTag;
            if (typeTag instanceof Integer) typeIndex = (Integer) typeTag;
            styleIndex = Math.max(0, Math.min(styleIndex, SwimStyle.values().length - 1));
            typeIndex = Math.max(0, Math.min(typeIndex, LessonType.values().length - 1));
            SwimStyle style = SwimStyle.values()[styleIndex];
            LessonType lessonType = LessonType.values()[typeIndex];
            Student student = new Student(first, last, style, lessonType);
            adapter.addStudent(student);
            saveStudentsToPrefs();
            refreshStats();
            dialog.dismiss();
            Toast.makeText(this, getString(R.string.student_added_toast), Toast.LENGTH_SHORT).show();
            firebaseHelper.addStudent(student, success -> runOnUiThread(() -> {
                saveStudentsToPrefs();
                if (!success) Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show();
            }));
        });
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void openSchedule() {
        List<Student> students = adapter.getStudents();
        PoolScheduler.ScheduleResult result = PoolScheduler.schedule(students);
        boolean hasIssues = !result.conflicts.isEmpty() || !result.gaps.isEmpty();

        if (hasIssues) {
            List<String> lines = new ArrayList<>();
            if (!result.conflicts.isEmpty()) {
                lines.add(getString(R.string.conflicts_header));
                lines.addAll(result.conflicts);
            }
            if (!result.gaps.isEmpty()) {
                if (!lines.isEmpty()) lines.add("");
                lines.add(getString(R.string.gaps_header));
                lines.addAll(result.gaps);
            }
            String message = getString(R.string.conflicts_alert_message) + "\n\n" + android.text.TextUtils.join("\n", lines);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.conflicts_alert_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.close, null)
                    .show();
        } else {
            launchScheduleActivity(students);
        }
    }

    private void launchScheduleActivity(List<Student> students) {
        Intent i = new Intent(this, ScheduleActivity.class);
        i.putExtra("students", new ArrayList<>(students));
        startActivity(i);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void openAccountDialog() {
        FirebaseUser user = firebaseHelper.getCurrentUser();
        String email = user != null && user.getEmail() != null ? user.getEmail() : "";
        String message = email.isEmpty() ? getString(R.string.signed_in_anonymously) : getString(R.string.signed_in_as, email);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.account)
                .setMessage(message)
                .setPositiveButton(R.string.sign_out, (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                })
                .setNegativeButton(R.string.close, null)
                .show();
    }
}
