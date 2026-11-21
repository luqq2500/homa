package com.homa.kernel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Homa's business calendar: the fixed Asia/Kuala_Lumpur zone. Instants are what is stored; rules counted in
 * days or months (MON-1, HLP-3, ROS-4, RTG-2) count calendar days and months here, so a 90-day or 6-month rule
 * never drifts at a day edge.
 */
public final class BusinessCalendar {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");

    private BusinessCalendar() {
    }

    /** The Kuala Lumpur calendar date the instant falls on. */
    public static LocalDate dateOf(Instant instant) {
        return instant.atZone(ZONE).toLocalDate();
    }

    /** The same Kuala Lumpur wall-clock time the given number of calendar days later (earlier when negative). */
    public static Instant plusDays(Instant instant, long days) {
        return instant.atZone(ZONE).plusDays(days).toInstant();
    }

    /**
     * The same Kuala Lumpur wall-clock time the given number of calendar months later (earlier when negative).
     * A day that does not exist in the target month becomes that month's last day: 31 August plus 6 months is
     * 28 or 29 February.
     */
    public static Instant plusMonths(Instant instant, long months) {
        return instant.atZone(ZONE).plusMonths(months).toInstant();
    }

    /**
     * The last moment of the Kuala Lumpur calendar year the instant falls in: 31 December, one nanosecond before
     * midnight. A payment record is kept until the end of its transaction's year plus 7 years (DAT-2).
     */
    public static Instant endOfYear(Instant instant) {
        int year = dateOf(instant).getYear();
        return LocalDate.of(year + 1, 1, 1).atStartOfDay(ZONE).toInstant().minusNanos(1);
    }
}
