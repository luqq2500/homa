package com.homa.listing.domain;

/**
 * The provider's Customer Cancellation Tier (§6.2, OFR-9). Listing only records the provider's choice; what each
 * tier's free cut-off means (CCX-3 to CCX-5) is Reservation's rule, read from its terms snapshot.
 */
public enum CancellationTier {
    FLEXIBLE,
    STANDARD,
    FIRM
}
