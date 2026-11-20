package com.homa.reservation.domain.replica;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Homa's calendar: every date and wall-clock time is in Asia/Kuala_Lumpur. A local stand-in with the same
 * semantics as the kernel's {@code BusinessCalendar}, to be replaced by it on merge.
 */
final class BusinessCalendar {

    static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");

    private BusinessCalendar() {
    }

    /** The Kuala Lumpur calendar date of the instant. */
    static LocalDate dateOf(Instant instant) {
        return LocalDate.ofInstant(instant, ZONE);
    }
}
