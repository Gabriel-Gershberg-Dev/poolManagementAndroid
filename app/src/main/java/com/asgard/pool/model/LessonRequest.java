package com.asgard.pool.model;

import java.io.Serializable;

/** A lesson request from a student; manager can assign or decline. */
public class LessonRequest implements Serializable {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_DECLINED = "DECLINED";

    private String firestoreId;
    private String userId;       // Firebase Auth uid of the student
    private String userName;    // display name (first + " " + last)
    private String firstName;
    private String lastName;
    private String swimStyle;    // SwimStyle.name()
    private String lessonType;   // LessonType.name()
    private long requestedAt;
    private String status;       // PENDING, ASSIGNED, DECLINED
    private String declineReason;

    public LessonRequest() {}

    public LessonRequest(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.requestedAt = System.currentTimeMillis();
        this.status = STATUS_PENDING;
    }

    public String getFirestoreId() { return firestoreId; }
    public void setFirestoreId(String firestoreId) { this.firestoreId = firestoreId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getSwimStyle() { return swimStyle; }
    public void setSwimStyle(String swimStyle) { this.swimStyle = swimStyle; }
    public String getLessonType() { return lessonType; }
    public void setLessonType(String lessonType) { this.lessonType = lessonType; }
    public long getRequestedAt() { return requestedAt; }
    public void setRequestedAt(long requestedAt) { this.requestedAt = requestedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDeclineReason() { return declineReason; }
    public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }
}
