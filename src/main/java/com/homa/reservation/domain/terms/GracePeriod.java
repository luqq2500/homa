package com.homa.reservation.domain.terms;

import com.homa.reservation.domain.ReservationValues.Version;
import java.time.Instant;

/**
 * CCX-1: a reservation Reserved at least 24 hours before its Check-in Period can be cancelled for free for 4 hours
 * after it is Reserved, whatever the tier. Under the reservation values in force when it was Requested (P3). The
 * 4 hours end at the moment they are up: from then on the tier's cut-off decides.
 */
public record GracePeriod(Instant reservedAt, Instant checkInStart, Version values) {

    /** Whether cancelling at this moment is within the Grace Period. */
    public boolean covers(Instant now) {
        boolean reservedInTime = !reservedAt.isAfter(checkInStart.minus(values.graceNeedsReservedBeforeCheckIn()));
        return reservedInTime && now.isBefore(reservedAt.plus(values.gracePeriod()));
    }
}
