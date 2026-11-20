package com.homa.reservation.domain.terms;

import com.homa.reservation.domain.JobTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * ACC-3: 24 hours after the request, or 1 hour before the Check-in Period starts, whichever comes first. The
 * provider can accept before it (ACC-2); at the deadline the request is no longer open.
 */
public record AcceptanceDeadline(Instant at) {

    private static final Duration RESPONSE_TIME = Duration.ofHours(24);
    private static final Duration BEFORE_CHECK_IN = Duration.ofHours(1);

    public AcceptanceDeadline {
        Objects.requireNonNull(at, "at");
    }

    public static AcceptanceDeadline from(Instant requestedAt, JobTime jobTime) {
        Instant dayLater = requestedAt.plus(RESPONSE_TIME);
        Instant hourBeforeCheckIn = jobTime.checkInPeriod().start().minus(BEFORE_CHECK_IN);
        return new AcceptanceDeadline(dayLater.isBefore(hourBeforeCheckIn) ? dayLater : hourBeforeCheckIn);
    }

    /** True while the provider can still accept: strictly before the deadline. */
    public boolean isOpenAt(Instant now) {
        return now.isBefore(at);
    }
}
