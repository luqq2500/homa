package com.homa.reservation.domain.terms;

/**
 * The provider's Customer Cancellation Tier (§6.2), kept in the reservation's terms (RES-8). Its free cut-offs
 * (Flexible 4 h, Standard 12 h, Firm 24 h before the Check-in Period, CCX-3 to CCX-5) arrive with the
 * cancellation slice.
 */
public enum CancellationTier {
    FLEXIBLE,
    STANDARD,
    FIRM
}
