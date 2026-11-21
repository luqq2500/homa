package com.homa.reservation.domain.replica;

import com.homa.kernel.BusinessCalendar;
import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.JobDuration;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A provider's weekly working hours as Reservation sees them (OFR-8), on Kuala Lumpur wall-clock time. A day that
 * is missing is not worked.
 */
public record WorkingHours(Map<DayOfWeek, DailyHours> days) {

    public WorkingHours {
        days = Map.copyOf(days);
    }

    /**
     * RES-3: the job time must fall within the working hours of its day. The Check-in Period and buffer may fall
     * outside them. A job that runs past midnight is never covered.
     */
    public boolean covers(TimeInterval jobTime) {
        LocalDateTime start = LocalDateTime.ofInstant(jobTime.start(), BusinessCalendar.ZONE);
        LocalDateTime end = LocalDateTime.ofInstant(jobTime.end(), BusinessCalendar.ZONE);
        return start.toLocalDate().equals(end.toLocalDate())
                && hoursOn(start.getDayOfWeek())
                        .map(hours -> hours.covers(start.toLocalTime(), end.toLocalTime()))
                        .orElse(false);
    }

    /** The job start times on that date, in the given steps from opening, at which a job of this length fits. */
    public List<Instant> startsOn(LocalDate date, JobDuration duration, Duration step) {
        return hoursOn(date.getDayOfWeek()).map(hours -> hours.startsFor(duration, step)).orElse(List.of()).stream()
                .map(time -> date.atTime(time).atZone(BusinessCalendar.ZONE).toInstant())
                .toList();
    }

    private Optional<DailyHours> hoursOn(DayOfWeek day) {
        return Optional.ofNullable(days.get(day));
    }
}
