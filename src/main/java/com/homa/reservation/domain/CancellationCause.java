package com.homa.reservation.domain;

/**
 * Why a reservation became Cancelled (plan, key decision 5). Settlement (§6, §7) and reliability counts (RTG-10)
 * read the cause. No slice 1 rule cancels a reservation; the cancellation slices attach a cause to
 * {@link ReservationStatus#CANCELLED}.
 */
public enum CancellationCause {
    /** REQ-2, CCX-1 to CCX-6. */
    CUSTOMER_CANCELLED,
    /** PCX-1, PCX-2. */
    PROVIDER_CANCELLED,
    /** LMC-1, LMC-2. */
    CUSTOMER_LAST_MINUTE,
    /** LMC-3, RPT-11. */
    PROVIDER_LAST_MINUTE,
    /** NRS-1 to NRS-3. */
    NO_ROSTER,
    /** NSH-3, NSH-6. */
    PROVIDER_NO_SHOW,
    /** NSH-5. */
    CUSTOMER_NO_SHOW,
    /** CUS-5. */
    CUSTOMER_SUSPENDED,
    /** SUS-1 to SUS-5. */
    PROVIDER_SUSPENDED
}
