package com.homa.listing.usecase;

import com.homa.listing.domain.WeeklyHours;
import com.homa.listing.domain.WorkingHours;
import com.homa.listing.usecase.exception.InvalidWeeklyHoursException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Weekly hours as requests and {@code AvailabilityChangedIE} write them: one "DAY HH:MM-HH:MM" entry per working
 * day, such as "SATURDAY 08:00-18:00", in Kuala Lumpur local time. Working hours are wall-clock times, so no zone is
 * applied here; Reservation reads them in Asia/Kuala_Lumpur.
 */
final class WeeklyHoursFormat {

    private static final String TIME = "((?:[01]\\d|2[0-3]):[0-5]\\d)";
    private static final Pattern ENTRY = Pattern.compile(
            "(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY) " + TIME + "-" + TIME);
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private WeeklyHoursFormat() {
    }

    static List<String> format(WeeklyHours weeklyHours) {
        return weeklyHours.days().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(day -> day.getKey().name() + " " + HH_MM.format(day.getValue().start()) + "-"
                        + HH_MM.format(day.getValue().end()))
                .toList();
    }

    static WeeklyHours parse(List<String> entries) {
        Map<DayOfWeek, WorkingHours> days = new EnumMap<>(DayOfWeek.class);
        for (String entry : entries) {
            Matcher matcher = matched(entry);
            DayOfWeek day = DayOfWeek.valueOf(matcher.group(1));
            WorkingHours hours = new WorkingHours(LocalTime.parse(matcher.group(2)), LocalTime.parse(matcher.group(3)));
            if (days.put(day, hours) != null) {
                throw new InvalidWeeklyHoursException("One entry per working day; " + day + " is given twice");
            }
        }
        return new WeeklyHours(days);
    }

    private static Matcher matched(String entry) {
        Matcher matcher = ENTRY.matcher(entry);
        if (!matcher.matches()) {
            throw new InvalidWeeklyHoursException("Expected \"DAY HH:MM-HH:MM\", such as \"SATURDAY 08:00-18:00\", was \""
                    + entry + "\"");
        }
        return matcher;
    }
}
