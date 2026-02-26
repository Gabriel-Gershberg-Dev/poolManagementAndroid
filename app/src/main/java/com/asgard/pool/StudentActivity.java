package com.asgard.pool;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asgard.pool.model.Lesson;
import com.asgard.pool.model.LessonRequest;
import com.asgard.pool.model.LessonType;
import com.asgard.pool.model.Student;
import com.asgard.pool.model.SwimStyle;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.asgard.pool.scheduler.PoolScheduler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StudentActivity extends AppCompatActivity {

    private FirebaseHelper firebaseHelper = new FirebaseHelper();
    private List<String> myLessonLines = new ArrayList<>();
    private List<LessonRequest> pendingRequests = new ArrayList<>();
    private List<LessonRequest> declinedRequests = new ArrayList<>();
    private RecyclerView recyclerMyLessons;
    private RecyclerView recyclerPendingStudent;
    private RecyclerView recyclerDeclined;
    private TextView tvNoLessons;
    private TextView tvNoPendingStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        tvNoLessons = findViewById(R.id.tvNoLessons);
        tvNoPendingStudent = findViewById(R.id.tvNoPendingStudent);
        recyclerMyLessons = findViewById(R.id.recyclerMyLessons);
        recyclerPendingStudent = findViewById(R.id.recyclerPendingStudent);
        recyclerDeclined = findViewById(R.id.recyclerDeclined);

        recyclerMyLessons.setLayoutManager(new LinearLayoutManager(this));
        recyclerMyLessons.setAdapter(new LessonLineAdapter(myLessonLines));

        recyclerPendingStudent.setLayoutManager(new LinearLayoutManager(this));
        recyclerPendingStudent.setAdapter(new PendingRequestStudentAdapter(pendingRequests));

        recyclerDeclined.setLayoutManager(new LinearLayoutManager(this));
        recyclerDeclined.setAdapter(new DeclinedAdapter(declinedRequests));

        findViewById(R.id.btnAccount).setOnClickListener(v -> openAccountDialog());
        findViewById(R.id.btnRequestLesson).setOnClickListener(v -> requestLesson());

        loadData();
    }

    private void loadData() {
        firebaseHelper.loadStudents(students -> runOnUiThread(() -> {
            String myUid = getCurrentUid();
            if (myUid == null) return;
            PoolScheduler.ScheduleResult result = PoolScheduler.schedule(students);
            myLessonLines.clear();
            for (Lesson lesson : result.lessons) {
                for (Student s : lesson.getStudents()) {
                    if (myUid.equals(s.getUserId())) {
                        String line = formatLesson(lesson);
                        myLessonLines.add(line);
                        break;
                    }
                }
            }
            ((LessonLineAdapter) recyclerMyLessons.getAdapter()).notifyDataSetChanged();
            tvNoLessons.setVisibility(myLessonLines.isEmpty() ? View.VISIBLE : View.GONE);
        }));

        firebaseHelper.loadLessonRequests(getCurrentUid(), requests -> runOnUiThread(() -> {
            pendingRequests.clear();
            declinedRequests.clear();
            if (requests != null) {
                for (LessonRequest r : requests) {
                    if (LessonRequest.STATUS_PENDING.equals(r.getStatus())) {
                        pendingRequests.add(r);
                    } else if (LessonRequest.STATUS_DECLINED.equals(r.getStatus())) {
                        declinedRequests.add(r);
                    }
                }
            }
            ((PendingRequestStudentAdapter) recyclerPendingStudent.getAdapter()).notifyDataSetChanged();
            ((DeclinedAdapter) recyclerDeclined.getAdapter()).notifyDataSetChanged();
            tvNoPendingStudent.setVisibility(pendingRequests.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerPendingStudent.setVisibility(pendingRequests.isEmpty() ? View.GONE : View.VISIBLE);
        }));
    }

    private String formatLesson(Lesson lesson) {
        String[] dayNames = getResources().getStringArray(R.array.weekday_names);
        int dayIndex = lesson.getDayOfWeek() - Calendar.MONDAY;
        String day = (dayIndex >= 0 && dayIndex < dayNames.length) ? dayNames[dayIndex] : "";
        String time = lesson.getStartHour() + ":" + String.format("%02d", lesson.getStartMinute());
        String inst = lesson.getInstructor() != null ? lesson.getInstructor().getName() : "";
        return day + " " + time + " – " + inst;
    }

    private void requestLesson() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_student, null);
        String[] styles = {getString(R.string.crawl), getString(R.string.breaststroke), getString(R.string.butterfly), getString(R.string.backstroke)};
        String[] types = {getString(R.string.private_only), getString(R.string.group_only), getString(R.string.private_or_group)};

        MaterialAutoCompleteTextView dropdownStyle = dialogView.findViewById(R.id.dropdownStyle);
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
                .setTitle(R.string.request_lesson)
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

            String fullName = (first + " " + last).trim();
            LessonRequest request = new LessonRequest(user.getUid(), fullName);
            request.setFirstName(first);
            request.setLastName(last);
            request.setSwimStyle(style.name());
            request.setLessonType(lessonType.name());

            firebaseHelper.addLessonRequest(request, success -> runOnUiThread(() -> {
                dialog.dismiss();
                if (success) {
                    Toast.makeText(this, R.string.request_sent, Toast.LENGTH_SHORT).show();
                    loadData();
                } else {
                    Toast.makeText(this, R.string.sync_failed, Toast.LENGTH_SHORT).show();
                }
            }));
        });
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String getCurrentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private void openAccountDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user != null && user.getEmail() != null ? user.getEmail() : "";
        String message = email.isEmpty() ? getString(R.string.signed_in_anonymously) : getString(R.string.signed_in_as, email);
        new androidx.appcompat.app.AlertDialog.Builder(this)
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

    static class PendingRequestStudentAdapter extends RecyclerView.Adapter<PendingRequestStudentAdapter.VH> {
        private final List<LessonRequest> list;

        PendingRequestStudentAdapter(List<LessonRequest> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_request_student, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            LessonRequest r = list.get(position);
            holder.tvName.setText(r.getUserName() != null ? r.getUserName() : "");
            String styleStr = formatSwimStyleName(r.getSwimStyle());
            String typeStr = formatLessonTypeName(r.getLessonType());
            holder.tvDetails.setText(styleStr + " · " + typeStr);
        }

        @Override
        public int getItemCount() { return list.size(); }

        private static String formatSwimStyleName(String name) {
            if (name == null) return "Crawl";
            switch (name) {
                case "CRAWL": return "Crawl";
                case "BREASTSTROKE": return "Breaststroke";
                case "BUTTERFLY": return "Butterfly";
                case "BACKSTROKE": return "Backstroke";
                default: return name;
            }
        }

        private static String formatLessonTypeName(String name) {
            if (name == null) return "Private only";
            switch (name) {
                case "PRIVATE_ONLY": return "Private only";
                case "GROUP_ONLY": return "Group only";
                case "PRIVATE_OR_GROUP": return "Private or group";
                default: return name;
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDetails;
            VH(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvDetails = itemView.findViewById(R.id.tvDetails);
            }
        }
    }

    static class LessonLineAdapter extends RecyclerView.Adapter<LessonLineAdapter.VH> {
        private final List<String> lines;

        LessonLineAdapter(List<String> lines) { this.lines = lines; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_lesson, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.tv.setText(lines.get(position));
        }

        @Override
        public int getItemCount() { return lines.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.tvLesson);
            }
        }
    }

    static class DeclinedAdapter extends RecyclerView.Adapter<DeclinedAdapter.VH> {
        private final List<LessonRequest> list;

        DeclinedAdapter(List<LessonRequest> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_declined_request, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            LessonRequest r = list.get(position);
            holder.tvTitle.setText(R.string.request_declined);
            if (r.getDeclineReason() != null && !r.getDeclineReason().isEmpty()) {
                holder.tvReason.setText(r.getDeclineReason());
                holder.tvReason.setVisibility(View.VISIBLE);
            } else {
                holder.tvReason.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvReason;
            VH(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvDeclinedTitle);
                tvReason = itemView.findViewById(R.id.tvDeclinedReason);
            }
        }
    }
}
