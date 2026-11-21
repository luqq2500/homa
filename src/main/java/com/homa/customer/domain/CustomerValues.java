package com.homa.customer.domain;

/**
 * The Homa-set values Customer owns (§11). They are account values: they apply at once to everyone, so they have
 * one current version and no dated ones (P3). Every other place takes the number from here, and other contexts get
 * the computed {@code endsAt} instead of the number.
 */
public final class CustomerValues {

    private CustomerValues() {
    }

    /** CUS-4, CUS-6: the shorter set suspension, in calendar days on the Kuala Lumpur calendar. */
    public static final int SHORT_SUSPENSION_DAYS = 7;

    /** CUS-4, CUS-6: the longer set suspension, in calendar days on the Kuala Lumpur calendar. */
    public static final int LONG_SUSPENSION_DAYS = 30;
}
