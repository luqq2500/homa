package com.homa.reservation.domain;

/**
 * Why a reservation became Cancelled (plan, key decision 5). Settlement (§6, §7) and reliability counts (RTG-10)
 * read the cause. A withdrawal, a decline and an expiry are not cancellations and have no cause. Whether a customer
 * cancellation cost a fee is in the settlement, not the cause: a free one is not counted, a fee-paying one is.
 */
public enum CancellationCause {
    /** CCX-1 to CCX-6: the customer cancels before the Check-in Period, free or with the Base Cancellation Fee. */
    CUSTOMER_CANCELLED,
    /** PCX-1, PCX-2: the provider cancels before the Check-in Period. */
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
