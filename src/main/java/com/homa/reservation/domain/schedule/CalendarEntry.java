package com.homa.reservation.domain.schedule;

import com.homa.kernel.TimeInterval;
import java.time.Instant;

/**
 * One taken interval of a schedule: a checkout hold (held by a {@code CheckoutId}, active for 15 minutes) or a
 * reservation's block (held by a {@code ReservationId}, active until released).
 */
record CalendarEntry(Object holder, TimeInterval interval, Instant activeUntil) {

    boolean isActiveAt(Instant now) {
        return now.isBefore(activeUntil);
    }

    boolean isHeldBy(Object someone) {
        return holder.equals(someone);
    }

    boolean overlaps(TimeInterval other) {
        return interval.overlaps(other);
    }
}
