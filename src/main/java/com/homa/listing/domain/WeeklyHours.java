package com.homa.listing.domain;

import com.homa.kernel.TimeInterval;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The provider's working days and hours (OFR-8): Kemas Home works Monday to Saturday, 8:00–18:00. A day that isn't
 * listed is not a working day.
 */
public record WeeklyHours(Map<DayOfWeek, WorkingHours> days) {

    public WeeklyHours {
        days = Map.copyOf(days);
    }

    /** The same hours on each of the given days. */
    public static WeeklyHours on(Set<DayOfWeek> workingDays, WorkingHours hours) {
        Map<DayOfWeek, WorkingHours> days = new EnumMap<>(DayOfWeek.class);
        workingDays.forEach(day -> days.put(day, hours));
        return new WeeklyHours(days);
    }

    public Optional<WorkingHours> hoursOn(DayOfWeek day) {
        return Optional.ofNullable(days.get(day));
    }

    /**
     * True when the whole job time falls within one working day's hours, read in the given zone. Only the job time
     * counts: the Check-in Period and the buffer may fall outside working hours (RES-3).
     */
    public boolean covers(TimeInterval jobTime, ZoneId zone) {
        LocalDateTime start = LocalDateTime.ofInstant(jobTime.start(), zone);
        LocalDateTime end = LocalDateTime.ofInstant(jobTime.end(), zone);
        WorkingHours hours = days.get(start.getDayOfWeek());
        return hours != null
                && start.toLocalDate().equals(end.toLocalDate())
                && hours.contains(start.toLocalTime(), end.toLocalTime());
    }
}
