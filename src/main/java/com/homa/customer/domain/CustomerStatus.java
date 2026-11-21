package com.homa.customer.domain;

/**
 * A customer's standing at a given moment, read with {@link Customer#statusAt}. SUSPENDED is not stored: it holds
 * while a {@link Suspension} is in force, so a suspension for a set period ends on its own (CUS-6). The account
 * states that CUS-8 and CUS-9 add in slice 6, DEACTIVATED and ERASED, are stored on the customer and read before the
 * suspension, which stays a separate value: an erased customer's suspension still blocks their sign-in (CUS-9).
 */
public enum CustomerStatus {
    ACTIVE,
    SUSPENDED
}
