package com.homa.listing.domain;

import static com.homa.listing.domain.ListingValues.DEFAULT_MAX_HOURS;
import static com.homa.listing.domain.ListingValues.DEFAULT_MIN_HELPERS;
import static com.homa.listing.domain.ListingValues.DEFAULT_MIN_HOURS;
import static com.homa.listing.domain.ListingValues.DURATION_STEP;
import static com.homa.listing.domain.ListingValues.MINIMUM_DURATION;

import com.homa.listing.domain.exception.InvalidBookingBoundsException;
import java.time.Duration;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * The hours and helpers a customer may book (OFR-5). Every provider's bounds sit on Homa's duration floor of
 * 1 hour in 15-minute steps (RES-5); this is the only place that floor is applied, and Reservation checks a
 * booking's hours against the bounds in the terms snapshot. Hours default to 1 to 8; helpers default to 1 up to
 * the number of active helpers. A maximum of helpers left on default is empty: the active helper count is Provider
 * state, so it is passed in when needed. An explicit maximum never lets a customer book more helpers than are
 * active.
 */
public record BookingBounds(Duration minHours, Duration maxHours, int minHelpers, OptionalInt maxHelpers) {

    public BookingBounds {
        Objects.requireNonNull(minHours, "minHours");
        Objects.requireNonNull(maxHours, "maxHours");
        Objects.requireNonNull(maxHelpers, "maxHelpers");
        requireValidHours(minHours, maxHours);
        requireValidHelpers(minHelpers, maxHelpers);
    }

    /** 1 to 8 hours, and 1 helper up to the number of active helpers. */
    public static BookingBounds defaults() {
        return new BookingBounds(DEFAULT_MIN_HOURS, DEFAULT_MAX_HOURS, DEFAULT_MIN_HELPERS, OptionalInt.empty());
    }

    public BookingBounds withHours(Duration min, Duration max) {
        return new BookingBounds(min, max, minHelpers, maxHelpers);
    }

    public BookingBounds withHelpers(int min, int max) {
        return new BookingBounds(minHours, maxHours, min, OptionalInt.of(max));
    }

    /** Helpers from the minimum up to however many helpers are active, the default maximum. */
    public BookingBounds withHelpersUpToActiveTeam(int min) {
        return new BookingBounds(minHours, maxHours, min, OptionalInt.empty());
    }

    /** True when the hours are within the bounds and in 15-minute steps (RES-5). */
    public boolean allowsHours(Duration hours) {
        return hours.compareTo(minHours) >= 0 && hours.compareTo(maxHours) <= 0 && inDurationSteps(hours);
    }

    /** The most helpers a customer can book while this many helpers are active. */
    public int maxHelpersWith(int activeHelperCount) {
        return Math.min(maxHelpers.orElse(activeHelperCount), activeHelperCount);
    }

    /** True when the helpers are within the bounds while this many helpers are active. */
    public boolean allowsHelpers(int helpers, int activeHelperCount) {
        return helpers >= minHelpers && helpers <= maxHelpersWith(activeHelperCount);
    }

    private static void requireValidHours(Duration minHours, Duration maxHours) {
        if (minHours.compareTo(MINIMUM_DURATION) < 0) {
            throw new InvalidBookingBoundsException("Minimum hours can't be under " + MINIMUM_DURATION + ", was " + minHours);
        }
        if (!inDurationSteps(minHours) || !inDurationSteps(maxHours)) {
            throw new InvalidBookingBoundsException(
                    "Hours move in " + DURATION_STEP + " steps, was " + minHours + " to " + maxHours);
        }
        if (maxHours.compareTo(minHours) < 0) {
            throw new InvalidBookingBoundsException("Maximum hours " + maxHours + " are under minimum hours " + minHours);
        }
    }

    private static void requireValidHelpers(int minHelpers, OptionalInt maxHelpers) {
        if (minHelpers < 1) {
            throw new InvalidBookingBoundsException("Minimum helpers can't be under 1, was " + minHelpers);
        }
        if (maxHelpers.isPresent() && maxHelpers.getAsInt() < minHelpers) {
            throw new InvalidBookingBoundsException(
                    "Maximum helpers " + maxHelpers.getAsInt() + " are under minimum helpers " + minHelpers);
        }
    }

    private static boolean inDurationSteps(Duration hours) {
        return hours.toSeconds() % DURATION_STEP.toSeconds() == 0;
    }
}
