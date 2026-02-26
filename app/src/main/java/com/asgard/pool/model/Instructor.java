package com.asgard.pool.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public enum Instructor {
    YOTAM("Yotam", 16, 20, new int[]{Calendar.MONDAY, Calendar.THURSDAY}, allStyles()),
    YONI("Yoni", 8, 15, new int[]{Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY}, new SwimStyle[]{SwimStyle.BREASTSTROKE, SwimStyle.BUTTERFLY}),
    JOHNNY("Johnny", 10, 19, new int[]{Calendar.SUNDAY, Calendar.TUESDAY, Calendar.THURSDAY}, allStyles());

    private static SwimStyle[] allStyles() {
        return new SwimStyle[]{SwimStyle.CRAWL, SwimStyle.BREASTSTROKE, SwimStyle.BUTTERFLY, SwimStyle.BACKSTROKE};
    }

    private final String name;
    private final int startHour;
    private final int endHour;
    private final int[] daysOfWeek; // Calendar.MONDAY etc. Pool closed weekend = we use only Mon-Fri in scheduler
    private final SwimStyle[] styles;

    Instructor(String name, int startHour, int endHour, int[] daysOfWeek, SwimStyle[] styles) {
        this.name = name;
        this.startHour = startHour;
        this.endHour = endHour;
        this.daysOfWeek = daysOfWeek;
        this.styles = styles;
    }

    public String getName() { return name; }
    public int getStartHour() { return startHour; }
    public int getEndHour() { return endHour; }
    public int[] getDaysOfWeek() { return daysOfWeek; }
    public SwimStyle[] getStyles() { return styles; }

    public boolean canTeach(SwimStyle style) {
        for (SwimStyle s : styles) if (s == style) return true;
        return false;
    }

    public boolean isAvailable(int dayOfWeek, int hour) {
        if (dayOfWeek == Calendar.SATURDAY) return false; // pool closed
        boolean dayOk = false;
        for (int d : daysOfWeek) if (d == dayOfWeek) { dayOk = true; break; }
        if (!dayOk) return false;
        return hour >= startHour && hour < endHour;
    }
}
