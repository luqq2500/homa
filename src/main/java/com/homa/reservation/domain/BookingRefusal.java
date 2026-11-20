package com.homa.reservation.domain;

/** Why a booking was refused at checkout, or when a payment landed after its checkout hold expired (RES-12). */
public enum BookingRefusal {
    /** CUS-3, CUS-5: only a registered customer in good standing can reserve. */
    CUSTOMER_CANNOT_RESERVE,
    /** PRV-6: the provider can't be booked, for example with no active helpers. */
    PROVIDER_NOT_BOOKABLE,
    /** RES-1. */
    CITY_NOT_COVERED,
    /** RES-2. */
    JOB_NOT_OFFERED,
    /** RES-5 with the provider's hour bounds (OFR-5). */
    HOURS_OUT_OF_BOUNDS,
    /** The provider's helper bounds and active helpers (OFR-5). */
    HELPERS_OUT_OF_BOUNDS,
    /** RES-4. */
    NOTICE_TOO_SHORT,
    /** ACC-1 (Instant: before the Check-in Period) and ACC-3 (Request: at least 1 hour before it). */
    TOO_LATE_FOR_ACCEPTANCE_POLICY,
    /** RES-3: the job time must fall within the provider's working hours. */
    OUTSIDE_WORKING_HOURS,
    /** RES-3, RES-9, RES-11: the provider's time is taken. */
    PROVIDER_BUSY,
    /** RES-10: the customer already has a job at that time. */
    CUSTOMER_BUSY
}
