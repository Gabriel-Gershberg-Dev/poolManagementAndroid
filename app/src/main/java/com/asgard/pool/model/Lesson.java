package com.asgard.pool.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Lesson implements Serializable {
    private Instructor instructor;
    private SwimStyle style;
    private int dayOfWeek; // Calendar.MONDAY etc.
    private int startHour;
    private int startMinute;
    private int durationMinutes;
    private boolean isGroup;
    private List<Student> students = new ArrayList<>();

    public Lesson() {}

    public Lesson(Instructor instructor, SwimStyle style, int dayOfWeek, int startHour, int startMinute, int durationMinutes, boolean isGroup) {
        this.instructor = instructor;
        this.style = style;
        this.dayOfWeek = dayOfWeek;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.durationMinutes = durationMinutes;
        this.isGroup = isGroup;
    }

    public void addStudent(Student s) { students.add(s); }
    public List<Student> getStudents() {
        if (students == null) students = new ArrayList<>();
        return Collections.unmodifiableList(students);
    }
    public Instructor getInstructor() { return instructor; }
    public SwimStyle getStyle() { return style; }
    public int getDayOfWeek() { return dayOfWeek; }
    public int getStartHour() { return startHour; }
    public int getStartMinute() { return startMinute; }
    public int getDurationMinutes() { return durationMinutes; }
    public boolean isGroup() { return isGroup; }

    public int getEndHour() {
        int endMin = startHour * 60 + startMinute + durationMinutes;
        return endMin / 60;
    }
    public int getEndMinute() {
        int endMin = startHour * 60 + startMinute + durationMinutes;
        return endMin % 60;
    }
}
