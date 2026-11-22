package com.homa.customer.domain;

/**
 * How long Support suspends a customer for (CUS-4), as Support decided it. Support owns the lengths and works out
 * when a timed suspension ends, so Customer receives that end with the length and never counts the days itself.
 */
public enum SuspensionLength {

    /** Ends on its own at the end Support gave (CUS-6). */
    SEVEN_DAYS(true),

    /** Ends on its own at the end Support gave (CUS-6). */
    THIRTY_DAYS(true),

    /** Has no end of its own; it ends only when Support lifts it (CUS-6). */
    UNTIL_LIFTED(false);

    private final boolean endsOnItsOwn;

    SuspensionLength(boolean endsOnItsOwn) {
        this.endsOnItsOwn = endsOnItsOwn;
    }

    /** CUS-6: true for a 7- or 30-day suspension, which ends by itself; false for one until lifted. */
    public boolean endsOnItsOwn() {
        return endsOnItsOwn;
    }
}
