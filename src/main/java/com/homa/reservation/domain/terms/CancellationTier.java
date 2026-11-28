package com.homa.reservation.domain.terms;

import com.homa.reservation.domain.ReservationValues.Version;
import java.time.Instant;

/**
 * The provider's Customer Cancellation Tier (§6.2), kept in the reservation's terms (RES-8). Its free cut-offs
 * before the Check-in Period (CCX-3 to CCX-5) are Homa's reservation values, in
 * {@code ReservationValues.Version#freeCancellationBeforeCheckIn()}, read as they were when the reservation was
 * Requested (P3).
 */
public enum CancellationTier {
    FLEXIBLE,
    STANDARD,
    FIRM;

    /**
     * CCX-3 to CCX-5: cancelling is free until the tier's cut-off before the Check-in Period: Flexible 4 hours,
     * Standard 12 hours, Firm 24 hours. The cut-off moment itself is the first moment that is no longer free, so
     * under Standard, with the Check-in Period at Saturday 9:45, Friday 21:44 is free and 21:45 is not (CCX-6).
     */
    public boolean isFreeAt(Instant now, Instant checkInStart, Version values) {
        return now.isBefore(checkInStart.minus(values.freeCancellationBeforeCheckIn().get(this)));
    }
}
