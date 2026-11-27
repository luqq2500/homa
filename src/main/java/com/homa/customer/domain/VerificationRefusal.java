package com.homa.customer.domain;

/**
 * Why Verification refused the identity a customer verified with, for the refusals that tell the customer why they
 * can't reserve (CUS-10). Verification never stores a person it refused, so Customer learns it from the refusal and
 * not from a verified identity. A document that wasn't accepted, or a check that didn't pass, isn't one of these:
 * the customer simply verifies again.
 */
public enum VerificationRefusal {

    /** CUS-10: the person isn't aged 18 or above, like Aisyah's 16-year-old son. */
    UNDER_MINIMUM_AGE(ReservationBlocker.UNDER_MINIMUM_AGE),

    /** CUS-10, §14: the person's status isn't one the country policy allows for customers, like a foreign visitor. */
    NOT_LOCAL(ReservationBlocker.NOT_LOCAL),

    /** CUS-10, IDN-3: the identity already holds another customer account, like Daniel's second one. */
    IDENTITY_HELD_BY_ANOTHER_ACCOUNT(ReservationBlocker.IDENTITY_HELD_BY_ANOTHER_ACCOUNT);

    private final ReservationBlocker blocker;

    VerificationRefusal(ReservationBlocker blocker) {
        this.blocker = blocker;
    }

    /** What the customer is told stops them from reserving. */
    ReservationBlocker blocker() {
        return blocker;
    }
}
