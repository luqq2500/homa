package com.homa.reservation.domain;

import com.homa.reservation.domain.exception.InvalidJobDurationException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/** How long a job lasts: at least 1 hour, in 15-minute steps (RES-5). */
public record QuarterHourDuration(Duration length) {

    private static final Duration MINIMUM = Duration.ofHours(1);
    private static final Duration STEP = Duration.ofMinutes(15);
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    public QuarterHourDuration {
        Objects.requireNonNull(length, "length");
        if (length.compareTo(MINIMUM) < 0 || !isWholeSteps(length)) {
            throw new InvalidJobDurationException(length);
        }
    }

    public static QuarterHourDuration of(Duration length) {
        return new QuarterHourDuration(length);
    }

    public static QuarterHourDuration ofMinutes(long minutes) {
        return new QuarterHourDuration(Duration.ofMinutes(minutes));
    }

    /** The length in hours, exact: 1h 15m is 1.25. */
    public BigDecimal inHours() {
        return BigDecimal.valueOf(length.toMinutes()).divide(MINUTES_PER_HOUR);
    }

    private static boolean isWholeSteps(Duration length) {
        return length.toNanos() % STEP.toNanos() == 0;
    }
}
