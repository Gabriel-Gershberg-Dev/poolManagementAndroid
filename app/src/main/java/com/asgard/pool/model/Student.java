package com.asgard.pool.model;

import java.io.Serializable;

public class Student implements Serializable {
    private String firstName;
    private String lastName;
    private SwimStyle swimStyle;
    private LessonType lessonType;
    /** Firestore document id; null if not from Firestore. */
    private String firestoreId;
    /** Firebase Auth uid when this student can log in and see their lessons. */
    private String userId;

    public Student() {}

    public Student(String firstName, String lastName, SwimStyle swimStyle, LessonType lessonType) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.swimStyle = swimStyle;
        this.lessonType = lessonType;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public SwimStyle getSwimStyle() { return swimStyle; }
    public void setSwimStyle(SwimStyle swimStyle) { this.swimStyle = swimStyle; }
    public LessonType getLessonType() { return lessonType; }
    public void setLessonType(LessonType lessonType) { this.lessonType = lessonType; }
    public String getFirestoreId() { return firestoreId; }
    public void setFirestoreId(String firestoreId) { this.firestoreId = firestoreId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() {
        String first = firstName != null ? firstName : "";
        String last = lastName != null ? lastName : "";
        return (first + " " + last).trim();
    }
}
