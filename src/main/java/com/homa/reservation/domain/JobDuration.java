package com.homa.reservation.domain;

import com.homa.reservation.domain.exception.InvalidJobDurationException;
import com.homa.reservation.domain.replica.BookingBounds;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/**
 * How long a job lasts. Homa's 1-hour minimum and 15-minute steps (RES-5) are Listing's to define, in its booking
 * bounds; Reservation only checks that the length matches the bounds and step it received (§13, OFR-5).
 */
public record JobDuration(Duration length) {

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    public JobDuration {
        Objects.requireNonNull(length, "length");
        if (!length.isPositive()) {
            throw new InvalidJobDurationException(length);
        }
    }

    public static JobDuration of(Duration length) {
        return new JobDuration(length);
    }

    public static JobDuration ofMinutes(long minutes) {
        return new JobDuration(Duration.ofMinutes(minutes));
    }

    /** RES-5, OFR-5: from the provider's minimum to its maximum hours, in whole steps. */
    public boolean fits(BookingBounds bounds) {
        return length.compareTo(bounds.minimumHours()) >= 0
                && length.compareTo(bounds.maximumHours()) <= 0
                && length.toNanos() % bounds.step().toNanos() == 0;
    }

    /** The length in hours, exact: 1h 15m is 1.25. */
    public BigDecimal inHours() {
        return BigDecimal.valueOf(length.toMinutes()).divide(MINUTES_PER_HOUR);
    }
}
