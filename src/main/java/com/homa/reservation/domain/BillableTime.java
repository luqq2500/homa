package com.homa.reservation.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Worked time as it is charged: every started step counts in full, so the time is rounded up to a whole number of
 * steps (LMC-2, §13 partial-time charging). Billing counts from the job start (§1).
 */
public record BillableTime(Duration duration) {

    /** The first hour is paid for by the Base Cancellation Fee itself (§1). */
    private static final Duration FIRST_HOUR = Duration.ofHours(1);
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    public BillableTime {
        Objects.requireNonNull(duration, "duration");
    }

    /**
     * LMC-2: the time worked after the first hour, from the job start until {@code now}, rounded up to whole
     * {@code step}s. Nothing before the job starts, and nothing in the first hour.
     */
    public static BillableTime afterFirstHour(Instant jobStart, Instant now, Duration step) {
        Duration afterFirstHour = Duration.between(jobStart, now).minus(FIRST_HOUR);
        if (!afterFirstHour.isPositive()) {
            return new BillableTime(Duration.ZERO);
        }
        long started = (afterFirstHour.toNanos() + step.toNanos() - 1) / step.toNanos();
        return new BillableTime(step.multipliedBy(started));
    }

    /** The charged time in hours, exact: 30 minutes is 0.5. */
    public BigDecimal inHours() {
        return BigDecimal.valueOf(duration.toMinutes()).divide(MINUTES_PER_HOUR);
    }
}
