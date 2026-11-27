package com.homa.customer.domain;

/**
 * A customer's standing at a given moment, read with {@link Customer#statusAt}. SUSPENDED is not stored: it holds
 * while the {@link Suspension} Support decided is in force, so a 7- or 30-day suspension ends on its own (CUS-6).
 * Identity verification is not a standing: an unverified customer is ACTIVE, and only reserving needs verification
 * (CUS-3), read with {@link Customer#reservationBlockerAt}. The account states that CUS-8 and CUS-9 add in slice 6,
 * DEACTIVATED and ERASED, will be stored on the customer and read before the suspension, which stays a separate
 * value: an erased customer's suspension still blocks their sign-in (DAT-7).
 */
public enum CustomerStatus {
    ACTIVE,
    SUSPENDED
}
