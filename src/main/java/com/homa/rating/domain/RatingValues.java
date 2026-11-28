package com.homa.rating.domain;

import com.homa.kernel.BusinessCalendar;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The Homa-set values Rating owns (§13). The Rating Window (RTG-2) is a reservation value, fixed when a reservation
 * is Requested (P3, RES-8): a change never edits the number in place, it adds a new dated {@link Version}, and a
 * session reads the version in force at its reservation's {@code requestedAt} through {@link #asOf(Instant)}. The
 * stars, the categories per side and the reliability period are account values: one current value that applies at
 * once. A business constant, not configuration: changing one is a change to the Homa Business Rule.
 */
public final class RatingValues {

    /** A rating has 1 to 5 stars (RTG-4). */
    public static final int MINIMUM_STARS = 1;
    public static final int MAXIMUM_STARS = 5;

    /** Three categories per side: RTG-7 for customers, RTG-8 for providers. */
    public static final int CATEGORIES_PER_SIDE = 3;

    /** Reliability counts cover the last 12 months, on the Kuala Lumpur calendar (RTG-10, DAT-4). */
    public static final int RELIABILITY_PERIOD_MONTHS = 12;

    /**
     * One dated set of Rating's reservation values: in force for reservations Requested from {@code effectiveFrom}
     * until the next version takes over.
     *
     * @param ratingWindow how long after the Protection Window ends the Rating Window stays open (RTG-2); a whole
     *                     number of calendar days
     */
    public record Version(Instant effectiveFrom, Duration ratingWindow) {

        public Version {
            Objects.requireNonNull(effectiveFrom, "effectiveFrom");
            Objects.requireNonNull(ratingWindow, "ratingWindow");
            if (!ratingWindow.isPositive() || !ratingWindow.equals(Duration.ofDays(ratingWindow.toDays()))) {
                throw new IllegalArgumentException("The Rating Window must be a whole number of days, not "
                        + ratingWindow);
            }
        }

        /**
         * When the Rating Window closes: the window after the Protection Window ends, counted in Kuala Lumpur
         * calendar days, never as multiples of 24 hours (RTG-2).
         */
        public Instant closesAfter(Instant protectionWindowEnd) {
            return BusinessCalendar.plusDays(protectionWindowEnd, ratingWindow.toDays());
        }
    }

    private static final RatingValues HOMA = new RatingValues(List.of(
            new Version(Instant.EPOCH, Duration.ofDays(7))));

    private final List<Version> versions;

    /** Versions oldest first: the first is in force from {@link Instant#EPOCH}, each later one strictly after. */
    RatingValues(List<Version> versions) {
        this.versions = List.copyOf(versions);
        if (this.versions.isEmpty() || !this.versions.get(0).effectiveFrom().equals(Instant.EPOCH)) {
            throw new IllegalArgumentException("Rating's values must start with a version in force from the epoch");
        }
        for (int i = 1; i < this.versions.size(); i++) {
            requireAfter(this.versions.get(i - 1), this.versions.get(i));
        }
    }

    private static void requireAfter(Version earlier, Version later) {
        if (!later.effectiveFrom().isAfter(earlier.effectiveFrom())) {
            throw new IllegalArgumentException("Each version of Rating's values must start after the one before, "
                    + later.effectiveFrom() + " does not");
        }
    }

    /** The version in force for a reservation Requested at {@code requestedAt} (P3). */
    public static Version asOf(Instant requestedAt) {
        return HOMA.inForceAt(requestedAt);
    }

    /** The newest version: the one a reservation Requested from now on gets. */
    public static Version current() {
        return HOMA.newest();
    }

    /** Homa's own values, the ones {@link #asOf(Instant)} reads. */
    static RatingValues homa() {
        return HOMA;
    }

    /** The latest version with {@code effectiveFrom <= requestedAt}. */
    Version inForceAt(Instant requestedAt) {
        Objects.requireNonNull(requestedAt, "requestedAt");
        Version inForce = versions.get(0);
        for (Version version : versions) {
            if (!version.effectiveFrom().isAfter(requestedAt)) {
                inForce = version;
            }
        }
        return inForce;
    }

    Version newest() {
        return versions.get(versions.size() - 1);
    }
}
