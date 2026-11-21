package com.homa.reservation.domain.terms;

/**
 * The provider's Customer Cancellation Tier (§6.2), kept in the reservation's terms (RES-8). Its free cut-offs
 * before the Check-in Period (CCX-3 to CCX-5) are Homa's reservation values, in
 * {@code ReservationValues.Version#freeCancellationBeforeCheckIn()}; the cancellation slice applies them.
 */
public enum CancellationTier {
    FLEXIBLE,
    STANDARD,
    FIRM
}
