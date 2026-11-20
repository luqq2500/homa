package com.homa.listing.domain;

import com.homa.kernel.Percentage;
import com.homa.listing.domain.exception.InvalidDiscountException;
import java.time.Duration;
import java.util.Objects;

/**
 * The provider's discount (OFR-4): a rate that applies when a reservation meets a minimum number of hours and of
 * helpers. No discount by default.
 */
public sealed interface DiscountRule {

    /** The discount rate a reservation of these hours and helpers gets; zero when it doesn't qualify. */
    Percentage rateFor(Duration hours, int helpers);

    static DiscountRule none() {
        return new None();
    }

    static DiscountRule of(Percentage rate, Duration minHours, int minHelpers) {
        return new Threshold(rate, minHours, minHelpers);
    }

    /** The default: no discount for any reservation. */
    record None() implements DiscountRule {

        @Override
        public Percentage rateFor(Duration hours, int helpers) {
            return Percentage.ZERO;
        }
    }

    /** Kemas Home's 10% for reservations of at least 3 hours and 2 helpers. */
    record Threshold(Percentage rate, Duration minHours, int minHelpers) implements DiscountRule {

        public Threshold {
            Objects.requireNonNull(rate, "rate");
            Objects.requireNonNull(minHours, "minHours");
            if (rate.isZero()) {
                throw new InvalidDiscountException("A discount rate must be above zero; choose no discount instead");
            }
            if (minHours.isNegative() || minHours.isZero()) {
                throw new InvalidDiscountException("A discount needs a minimum number of hours, was " + minHours);
            }
            if (minHelpers < 1) {
                throw new InvalidDiscountException("A discount needs at least 1 helper as its minimum, was " + minHelpers);
            }
        }

        @Override
        public Percentage rateFor(Duration hours, int helpers) {
            boolean qualifies = hours.compareTo(minHours) >= 0 && helpers >= minHelpers;
            return qualifies ? rate : Percentage.ZERO;
        }
    }
}
