package com.homa.payment.domain;

import com.homa.kernel.Percentage;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The Homa-set values Payment owns (§13): the platform fee (PAY-3) and the fine deduction cap (PAY-5). Both are
 * reservation values, fixed when a reservation is Requested (P3, RES-8). A change never edits a number in place: it
 * adds a new dated {@link Version}, and every payout reads the version in force at its reservation's
 * {@code requestedAt} through {@link #asOf(Instant)}. A business constant, not configuration: changing it is a change
 * to the Homa Business Rule.
 */
public final class PaymentValues {

    /**
     * One dated set of Payment's values: in force for reservations Requested from {@code effectiveFrom} until the next
     * version takes over.
     */
    public record Version(Instant effectiveFrom, Percentage platformFeeRate, Percentage fineDeductionCap) {

        public Version {
            Objects.requireNonNull(effectiveFrom, "effectiveFrom");
            Objects.requireNonNull(platformFeeRate, "platformFeeRate");
            Objects.requireNonNull(fineDeductionCap, "fineDeductionCap");
        }
    }

    private static final PaymentValues HOMA = new PaymentValues(List.of(
            new Version(Instant.EPOCH, Percentage.of("15.5"), Percentage.of(20))));

    private final List<Version> versions;

    /** Versions oldest first: the first is in force from {@link Instant#EPOCH}, each later one strictly after. */
    PaymentValues(List<Version> versions) {
        this.versions = List.copyOf(versions);
        if (this.versions.isEmpty() || !this.versions.get(0).effectiveFrom().equals(Instant.EPOCH)) {
            throw new IllegalArgumentException("Payment's values must start with a version in force from the epoch");
        }
        for (int i = 1; i < this.versions.size(); i++) {
            requireAfter(this.versions.get(i - 1), this.versions.get(i));
        }
    }

    private static void requireAfter(Version earlier, Version later) {
        if (!later.effectiveFrom().isAfter(earlier.effectiveFrom())) {
            throw new IllegalArgumentException("Each version of Payment's values must start after the one before, "
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
    static PaymentValues homa() {
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
