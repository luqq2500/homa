package com.homa.listing.domain;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import java.time.Duration;

/**
 * The Homa-set values Listing owns (§13), each in one place. They are business constants, not configuration: a
 * change is a one-line change here, reviewed like a contract change. A reservation keeps them because they are
 * frozen into the terms snapshot it was Requested with (P3, RES-8), so no dated versions are kept here.
 */
public final class ListingValues {

    private ListingValues() {
    }

    /** OFR-11: the lowest per-helper hourly rate after any discount. */
    public static final Money MINIMUM_RATE = Money.myr(20);

    /** OFR-4: the largest discount rate a provider can set. */
    public static final Percentage MAXIMUM_DISCOUNT = Percentage.of(99);

    /** OFR-5: the minimum hours a provider's bounds start at. */
    public static final Duration DEFAULT_MIN_HOURS = Duration.ofHours(1);

    /** OFR-5: the maximum hours a provider's bounds start at. */
    public static final Duration DEFAULT_MAX_HOURS = Duration.ofHours(8);

    /** OFR-5: the minimum helpers a provider's bounds start at; the default maximum is the active helper count. */
    public static final int DEFAULT_MIN_HELPERS = 1;

    /** RES-5: no reservation is shorter than this, whatever the provider's bounds. Applied only by {@link BookingBounds}. */
    public static final Duration MINIMUM_DURATION = Duration.ofHours(1);

    /** RES-5: a reservation's duration moves in steps of this. Applied only by {@link BookingBounds}. */
    public static final Duration DURATION_STEP = Duration.ofMinutes(15);

    /** OFR-6: the minimum notice a new listing starts with. */
    public static final Duration DEFAULT_MINIMUM_NOTICE = Duration.ofHours(2);

    /** OFR-6: the lowest minimum notice under the Instant policy. */
    public static final Duration INSTANT_NOTICE_FLOOR = Duration.ofHours(1);

    /** OFR-6: the lowest minimum notice under the Request policy, so the provider has at least 1 hour to respond. */
    public static final Duration REQUEST_NOTICE_FLOOR = Duration.ofHours(2);

    /** OFR-10: the buffer between jobs a new listing starts with. */
    public static final Duration DEFAULT_JOB_BUFFER = Duration.ofMinutes(30);
}
