package com.homa.customer.domain;

/**
 * Why a customer can't reserve at a given moment, read with {@link Customer#reservationBlockerAt}. Only the first
 * one that applies is given, in this order, so a suspended customer is told so before being asked to verify.
 */
public enum ReservationBlocker {

    /** CUS-5: a suspended customer can't reserve. */
    SUSPENDED,

    /**
     * CUS-3: the customer hasn't verified their identity yet. They verify at their first reservation, before paying,
     * so the checkout asks them to verify instead of opening a payment.
     */
    IDENTITY_NOT_VERIFIED,

    /**
     * CUS-10, §14: the identity isn't local, and the country policy allows only locals as customers. Customer learns
     * it from a verified identity, or from Verification's refusal.
     */
    NOT_LOCAL,

    /** CUS-10: the verified identity didn't pass the age check; a customer must be aged 18 or above. */
    UNDER_MINIMUM_AGE,

    /**
     * CUS-10, IDN-3: Verification refused the identity because another person already holds it, so one verified
     * identity holds one customer account.
     */
    IDENTITY_HELD_BY_ANOTHER_ACCOUNT
}
