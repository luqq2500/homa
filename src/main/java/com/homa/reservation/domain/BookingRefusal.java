package com.homa.reservation.domain;

import static com.homa.reservation.domain.CheckoutRejection.RULE_FAILED;
import static com.homa.reservation.domain.CheckoutRejection.TIME_TAKEN;

/**
 * Why a booking was refused, at checkout or when its confirmed payment was to make it Requested. A refused payment is
 * refunded in full at once, for the {@link #rejection()} reason (P1, RES-11).
 */
public enum BookingRefusal {
    /** CUS-3, CUS-5: only a registered customer in good standing can reserve. */
    CUSTOMER_CANNOT_RESERVE(RULE_FAILED),
    /** PRV-6: the provider can't be booked, for example with no active helpers. */
    PROVIDER_NOT_BOOKABLE(RULE_FAILED),
    /** RES-1. */
    CITY_NOT_COVERED(RULE_FAILED),
    /** RES-2. */
    JOB_NOT_OFFERED(RULE_FAILED),
    /** RES-5 with the provider's hour bounds and steps (OFR-5). */
    HOURS_OUT_OF_BOUNDS(RULE_FAILED),
    /** The provider's helper bounds and active helpers (OFR-5). */
    HELPERS_OUT_OF_BOUNDS(RULE_FAILED),
    /** RES-4. */
    NOTICE_TOO_SHORT(RULE_FAILED),
    /** ACC-1 (Instant: before the Check-in Period) and ACC-3 (Request: at least 1 hour before it). */
    TOO_LATE_FOR_ACCEPTANCE_POLICY(RULE_FAILED),
    /** RES-3: the job time must fall within the provider's working hours. */
    OUTSIDE_WORKING_HOURS(RULE_FAILED),
    /** RES-3, OFR-8: a private appointment synced from the provider's external calendar. */
    PROVIDER_BUSY(RULE_FAILED),
    /** RES-9, RES-11: another reservation already blocks the provider's time; its payment was confirmed first. */
    TIME_TAKEN_BY_ANOTHER_RESERVATION(TIME_TAKEN),
    /** RES-10: the customer already has a job at that time. */
    CUSTOMER_BUSY(RULE_FAILED),
    /** RES-6, RES-8: the price under the terms in force when the payment confirmed is not the amount paid. */
    PRICE_CHANGED(RULE_FAILED);

    private final CheckoutRejection rejection;

    BookingRefusal(CheckoutRejection rejection) {
        this.rejection = rejection;
    }

    /** The reason a refused paid checkout is rejected with, for {@code ReservationCheckoutRejectedIE}. */
    public CheckoutRejection rejection() {
        return rejection;
    }
}
