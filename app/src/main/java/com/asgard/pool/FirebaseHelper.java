package com.asgard.pool;

import android.util.Log;

import com.asgard.pool.model.LessonRequest;
import com.asgard.pool.model.LessonType;
import com.asgard.pool.model.Student;
import com.asgard.pool.model.SwimStyle;
import com.asgard.pool.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore: shared pool (pool/default/students), lesson requests (pool/default/lesson_requests),
 * user profile (users/{uid}/profile) for role STUDENT/MANAGER.
 */
public final class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static final String POOL_ID = "default";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_STUDENTS = "students";
    private static final String COLLECTION_LESSON_REQUESTS = "lesson_requests";
    private static final String DOC_PROFILE = "profile";

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isSignedIn() {
        return getCurrentUser() != null;
    }

    private com.google.firebase.firestore.DocumentReference getPoolDocument() {
        return firestore.collection("pool").document(POOL_ID);
    }

    private com.google.firebase.firestore.CollectionReference getPoolStudentsCollection() {
        return getPoolDocument().collection(COLLECTION_STUDENTS);
    }

    private com.google.firebase.firestore.CollectionReference getLessonRequestsCollection() {
        return getPoolDocument().collection(COLLECTION_LESSON_REQUESTS);
    }

    /**
     * Creates the pool/default document in Firestore so the structure appears in the console.
     * Subcollections "students" and "lesson_requests" are created when the first doc is added.
     */
    public void ensurePoolExists(Runnable onDone) {
        getPoolDocument().set(Collections.singletonMap("initialized", true), SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Pool document ready");
                    if (onDone != null) onDone.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "ensurePoolExists failed", e);
                    if (onDone != null) onDone.run();
                });
    }

    private com.google.firebase.firestore.DocumentReference getUserProfileRef() {
        FirebaseUser user = getCurrentUser();
        if (user == null) return null;
        return firestore.collection(COLLECTION_USERS).document(user.getUid()).collection("profile").document("role");
    }

    private Map<String, Object> studentToMap(Student s) {
        Map<String, Object> map = new HashMap<>();
        map.put("firstName", s.getFirstName() != null ? s.getFirstName() : "");
        map.put("lastName", s.getLastName() != null ? s.getLastName() : "");
        map.put("swimStyle", s.getSwimStyle() != null ? s.getSwimStyle().name() : SwimStyle.CRAWL.name());
        map.put("lessonType", s.getLessonType() != null ? s.getLessonType().name() : LessonType.PRIVATE_ONLY.name());
        if (s.getUserId() != null) map.put("userId", s.getUserId());
        return map;
    }

    private Student docToStudent(QueryDocumentSnapshot doc) {
        Map<String, Object> data = doc.getData();
        String first = data.containsKey("firstName") ? String.valueOf(data.get("firstName")) : "";
        String last = data.containsKey("lastName") ? String.valueOf(data.get("lastName")) : "";
        String styleStr = data.containsKey("swimStyle") ? String.valueOf(data.get("swimStyle")) : SwimStyle.CRAWL.name();
        String typeStr = data.containsKey("lessonType") ? String.valueOf(data.get("lessonType")) : LessonType.PRIVATE_ONLY.name();
        String uid = data.containsKey("userId") ? String.valueOf(data.get("userId")) : null;
        SwimStyle style = parseSwimStyle(styleStr);
        LessonType type = parseLessonType(typeStr);
        Student s = new Student(first, last, style, type);
        s.setFirestoreId(doc.getId());
        s.setUserId(uid);
        return s;
    }

    private static SwimStyle parseSwimStyle(String name) {
        try { return SwimStyle.valueOf(name); } catch (Exception e) { return SwimStyle.CRAWL; }
    }

    private static LessonType parseLessonType(String name) {
        try { return LessonType.valueOf(name); } catch (Exception e) { return LessonType.PRIVATE_ONLY; }
    }

    public void loadUserProfile(OnProfileLoadedListener listener) {
        com.google.firebase.firestore.DocumentReference ref = getUserProfileRef();
        if (ref == null) {
            if (listener != null) listener.onLoaded(null);
            return;
        }
        ref.get()
                .addOnSuccessListener(snap -> {
                    UserProfile p = null;
                    if (snap != null && snap.exists() && snap.getData() != null) {
                        String role = snap.getString("role");
                        if (role != null) {
                            p = new UserProfile(role);
                        }
                    }
                    if (listener != null) listener.onLoaded(p);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadUserProfile failed", e);
                    if (listener != null) listener.onLoaded(null);
                });
    }

    public void saveUserProfile(UserProfile profile, OnWriteDoneListener onDone) {
        com.google.firebase.firestore.DocumentReference ref = getUserProfileRef();
        if (ref == null) {
            if (onDone != null) onDone.onDone(false);
            return;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("role", profile.getRole() != null ? profile.getRole() : UserProfile.ROLE_STUDENT);
        ref.set(map)
                .addOnSuccessListener(aVoid -> { if (onDone != null) onDone.onDone(true); })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveUserProfile failed", e);
                    if (onDone != null) onDone.onDone(false);
                });
    }

    /**
     * Load students from the shared pool (pool/default/students).
     */
    public void loadStudents(OnStudentsLoadedListener onLoaded) {
        getPoolStudentsCollection().get()
                .addOnSuccessListener(snapshots -> {
                    List<Student> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            list.add(docToStudent(doc));
                        }
                    }
                    if (onLoaded != null) onLoaded.onLoaded(list);
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                    Log.e(TAG, "loadStudents failed: " + msg, e);
                    if (onLoaded != null) {
                        onLoaded.onLoaded(new ArrayList<>());
                        onLoaded.onError(e);
                    }
                });
    }

    public void addStudent(Student student, OnWriteDoneListener onDone) {
        getPoolStudentsCollection().add(studentToMap(student))
                .addOnSuccessListener(ref -> {
                    student.setFirestoreId(ref.getId());
                    if (onDone != null) onDone.onDone(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addStudent failed", e);
                    if (onDone != null) onDone.onDone(false);
                });
    }

    public void removeStudent(String firestoreId, OnWriteDoneListener onDone) {
        if (firestoreId == null || firestoreId.isEmpty()) {
            if (onDone != null) onDone.onDone(true);
            return;
        }
        getPoolStudentsCollection().document(firestoreId).delete()
                .addOnSuccessListener(aVoid -> { if (onDone != null) onDone.onDone(true); })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "removeStudent failed", e);
                    if (onDone != null) onDone.onDone(false);
                });
    }

    public void loadLessonRequests(String forUserId, OnLessonRequestsLoadedListener listener) {
        getLessonRequestsCollection().get()
                .addOnSuccessListener(snapshots -> {
                    List<LessonRequest> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        LessonRequest r = docToLessonRequest(doc);
                        if (forUserId == null || forUserId.equals(r.getUserId())) {
                            list.add(r);
                        }
                    }
                    Collections.sort(list, (a, b) -> Long.compare(b.getRequestedAt(), a.getRequestedAt()));
                    if (listener != null) listener.onLoaded(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadLessonRequests failed: " + e.getMessage(), e);
                    if (listener != null) listener.onLoaded(new ArrayList<>());
                });
    }

    private LessonRequest docToLessonRequest(QueryDocumentSnapshot doc) {
        Map<String, Object> data = doc.getData();
        LessonRequest r = new LessonRequest();
        r.setFirestoreId(doc.getId());
        r.setUserId(data.containsKey("userId") ? String.valueOf(data.get("userId")) : null);
        r.setUserName(data.containsKey("userName") ? String.valueOf(data.get("userName")) : "");
        r.setFirstName(data.containsKey("firstName") ? String.valueOf(data.get("firstName")) : null);
        r.setLastName(data.containsKey("lastName") ? String.valueOf(data.get("lastName")) : null);
        r.setSwimStyle(data.containsKey("swimStyle") ? String.valueOf(data.get("swimStyle")) : null);
        r.setLessonType(data.containsKey("lessonType") ? String.valueOf(data.get("lessonType")) : null);
        r.setRequestedAt(data.containsKey("requestedAt") ? ((Number) data.get("requestedAt")).longValue() : 0L);
        r.setStatus(data.containsKey("status") ? String.valueOf(data.get("status")) : LessonRequest.STATUS_PENDING);
        r.setDeclineReason(data.containsKey("declineReason") ? String.valueOf(data.get("declineReason")) : null);
        return r;
    }

    public void addLessonRequest(LessonRequest request, OnWriteDoneListener onDone) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", request.getUserId() != null ? request.getUserId() : "");
        map.put("userName", request.getUserName() != null ? request.getUserName() : "");
        if (request.getFirstName() != null) map.put("firstName", request.getFirstName());
        if (request.getLastName() != null) map.put("lastName", request.getLastName());
        if (request.getSwimStyle() != null) map.put("swimStyle", request.getSwimStyle());
        if (request.getLessonType() != null) map.put("lessonType", request.getLessonType());
        map.put("requestedAt", request.getRequestedAt());
        map.put("status", request.getStatus() != null ? request.getStatus() : LessonRequest.STATUS_PENDING);
        getLessonRequestsCollection().add(map)
                .addOnSuccessListener(ref -> {
                    request.setFirestoreId(ref.getId());
                    if (onDone != null) onDone.onDone(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addLessonRequest failed", e);
                    if (onDone != null) onDone.onDone(false);
                });
    }

    public void updateLessonRequestStatus(String docId, String status, String declineReason, OnWriteDoneListener onDone) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        if (declineReason != null) map.put("declineReason", declineReason);
        getLessonRequestsCollection().document(docId).update(map)
                .addOnSuccessListener(aVoid -> { if (onDone != null) onDone.onDone(true); })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateLessonRequestStatus failed", e);
                    if (onDone != null) onDone.onDone(false);
                });
    }

    public interface OnProfileLoadedListener {
        void onLoaded(UserProfile profile);
    }

    public interface OnStudentsLoadedListener {
        void onLoaded(List<Student> students);
        default void onError(Throwable e) {}
    }

    public interface OnLessonRequestsLoadedListener {
        void onLoaded(List<LessonRequest> requests);
    }

    public interface OnWriteDoneListener {
        void onDone(boolean success);
    }
}
