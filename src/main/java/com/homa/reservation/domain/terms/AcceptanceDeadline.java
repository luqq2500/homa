package com.homa.reservation.domain.terms;

import com.homa.reservation.domain.JobTime;
import com.homa.reservation.domain.ReservationValues.Version;
import java.time.Instant;
import java.util.Objects;

/**
 * ACC-3: 24 hours after the request, or 1 hour before the Check-in Period starts, whichever comes first, under the
 * reservation values in force when it was Requested (P3). The provider can accept before it (ACC-2); at the deadline
 * the request is no longer open.
 */
public record AcceptanceDeadline(Instant at) {

    public AcceptanceDeadline {
        Objects.requireNonNull(at, "at");
    }

    public static AcceptanceDeadline from(Instant requestedAt, JobTime jobTime, Version values) {
        Instant dayLater = requestedAt.plus(values.acceptanceResponseTime());
        Instant hourBeforeCheckIn = jobTime.checkInStart(values).minus(values.acceptanceCutOffBeforeCheckIn());
        return new AcceptanceDeadline(dayLater.isBefore(hourBeforeCheckIn) ? dayLater : hourBeforeCheckIn);
    }

    /** True while the provider can still accept: strictly before the deadline. */
    public boolean isOpenAt(Instant now) {
        return now.isBefore(at);
    }
}
