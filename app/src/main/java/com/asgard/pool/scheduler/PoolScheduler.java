package com.asgard.pool.scheduler;

import com.asgard.pool.model.Instructor;
import com.asgard.pool.model.Lesson;
import com.asgard.pool.model.LessonType;
import com.asgard.pool.model.Student;
import com.asgard.pool.model.SwimStyle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PoolScheduler {
    private static final int PRIVATE_DURATION = 45;
    private static final int GROUP_DURATION = 60;
    /** Slot interval in minutes (15 = try 8:00, 8:15, 8:30, ... to fill gaps) */
    private static final int SLOT_INTERVAL_MINUTES = 15;
    private static final int[] WORK_DAYS = {Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY};

    public static class ScheduleResult {
        public List<Lesson> lessons = new ArrayList<>();
        public List<String> conflicts = new ArrayList<>();
        public List<String> gaps = new ArrayList<>();
    }

    public static ScheduleResult schedule(List<Student> students) {
        ScheduleResult result = new ScheduleResult();
        if (students == null || students.isEmpty()) {
            result.gaps.add("No students entered");
            return result;
        }
        if (students.size() > 30) {
            result.conflicts.add("Maximum 30 students");
            return result;
        }

        List<Student> privateOnly = new ArrayList<>();
        List<Student> groupOnly = new ArrayList<>();
        List<Student> privateOrGroup = new ArrayList<>();

        for (Student s : students) {
            if (s.getLessonType() == LessonType.PRIVATE_ONLY) privateOnly.add(s);
            else if (s.getLessonType() == LessonType.GROUP_ONLY) groupOnly.add(s);
            else privateOrGroup.add(s);
        }

        // 1. Schedule group-only students into group lessons
        scheduleGroupLessons(groupOnly, result);

        // 2. PRIVATE_OR_GROUP: try joining existing group, then open new group, fallback to private
        List<Student> noGroupYet = new ArrayList<>();
        for (Student s : privateOrGroup) {
            if (!tryJoinExistingGroup(s, result)) {
                noGroupYet.add(s);
            }
        }
        // Try to create new group lessons for remaining private-or-group students
        scheduleGroupLessons(noGroupYet, result);
        // Anyone still unscheduled gets a private lesson
        for (Student s : new ArrayList<>(noGroupYet)) {
            boolean alreadyScheduled = false;
            for (Lesson l : result.lessons) {
                if (l.getStudents().contains(s)) { alreadyScheduled = true; break; }
            }
            if (!alreadyScheduled) {
                scheduleOnePrivate(s, result);
            }
        }

        // 3. Schedule private-only students
        for (Student s : privateOnly) scheduleOnePrivate(s, result);

        detectConflicts(result);
        return result;
    }

    private static final int MAX_GROUP_SIZE = 4;

    private static boolean tryJoinExistingGroup(Student s, ScheduleResult result) {
        for (Lesson lesson : result.lessons) {
            if (!lesson.isGroup()) continue;
            if (lesson.getStyle() != s.getSwimStyle()) continue;
            if (lesson.getStudents().size() >= MAX_GROUP_SIZE) continue;
            lesson.addStudent(s);
            return true;
        }
        return false;
    }

    private static void scheduleOnePrivate(Student s, ScheduleResult result) {
        for (int day : WORK_DAYS) {
            for (Instructor inst : Instructor.values()) {
                if (!inst.canTeach(s.getSwimStyle()) || !inst.isAvailable(day, inst.getStartHour())) continue;
                int dayStartMin = inst.getStartHour() * 60;
                int dayEndMin = inst.getEndHour() * 60;
                for (int startMin = dayStartMin; startMin + PRIVATE_DURATION <= dayEndMin; startMin += SLOT_INTERVAL_MINUTES) {
                    int startH = startMin / 60;
                    int startM = startMin % 60;
                    if (overlaps(result.lessons, inst, day, startH, startM, PRIVATE_DURATION)) continue;
                    Lesson lesson = new Lesson(inst, s.getSwimStyle(), day, startH, startM, PRIVATE_DURATION, false);
                    lesson.addStudent(s);
                    result.lessons.add(lesson);
                    return;
                }
            }
        }
        result.gaps.add("No slot for student: " + s.getFullName() + " (private lesson, " + styleName(s.getSwimStyle()) + ")");
    }

    private static void scheduleGroupLessons(List<Student> forGroup, ScheduleResult result) {
        if (forGroup.isEmpty()) return;
        for (SwimStyle style : SwimStyle.values()) {
            List<Student> need = new ArrayList<>();
            for (Student s : forGroup) if (s.getSwimStyle() == style) need.add(s);
            if (need.isEmpty()) continue;
            for (int day : WORK_DAYS) {
                for (Instructor inst : Instructor.values()) {
                    if (!inst.canTeach(style) || !inst.isAvailable(day, inst.getStartHour())) continue;
                    int dayStartMin = inst.getStartHour() * 60;
                    int dayEndMin = inst.getEndHour() * 60;
                    for (int startMin = dayStartMin; startMin + GROUP_DURATION <= dayEndMin; startMin += SLOT_INTERVAL_MINUTES) {
                        int startH = startMin / 60;
                        int startM = startMin % 60;
                        if (overlaps(result.lessons, inst, day, startH, startM, GROUP_DURATION)) continue;
                        Lesson lesson = new Lesson(inst, style, day, startH, startM, GROUP_DURATION, true);
                        for (Student s : need) lesson.addStudent(s);
                        result.lessons.add(lesson);
                        need.clear();
                        break;
                    }
                    if (need.isEmpty()) break;
                }
                if (need.isEmpty()) break;
            }
            for (Student s : need) result.gaps.add("No group lesson for: " + s.getFullName() + " (" + styleName(style) + ")");
        }
    }

    private static boolean overlaps(List<Lesson> lessons, Instructor inst, int day, int startH, int startM, int duration) {
        int startMin = startH * 60 + startM;
        int endMin = startMin + duration;
        for (Lesson L : lessons) {
            if (L.getInstructor() != inst || L.getDayOfWeek() != day) continue;
            int Lstart = L.getStartHour() * 60 + L.getStartMinute();
            int Lend = Lstart + L.getDurationMinutes();
            if (startMin < Lend && endMin > Lstart) return true;
        }
        return false;
    }

    private static void detectConflicts(ScheduleResult result) {
        for (int i = 0; i < result.lessons.size(); i++) {
            Lesson a = result.lessons.get(i);
            for (int j = i + 1; j < result.lessons.size(); j++) {
                Lesson b = result.lessons.get(j);
                if (a.getInstructor() == b.getInstructor() && a.getDayOfWeek() == b.getDayOfWeek()) {
                    int aStart = a.getStartHour() * 60 + a.getStartMinute();
                    int aEnd = aStart + a.getDurationMinutes();
                    int bStart = b.getStartHour() * 60 + b.getStartMinute();
                    int bEnd = bStart + b.getDurationMinutes();
                    if (aStart < bEnd && bStart < aEnd)
                        result.conflicts.add("Conflict: " + a.getInstructor().getName() + " " + dayName(a.getDayOfWeek()) + " " + timeStr(a.getStartHour(), a.getStartMinute()));
                }
            }
        }
    }

    private static String timeStr(int hour, int minute) {
        if (minute == 0) return hour + ":00";
        return hour + ":" + (minute < 10 ? "0" : "") + minute;
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

    public static String dayName(int day) {
        switch (day) {
            case Calendar.SUNDAY: return "Sunday";
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
            default: return "?";
        }
    }
}
