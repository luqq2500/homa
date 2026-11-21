package com.homa.listing.domain;

import com.homa.kernel.Percentage;
import com.homa.listing.domain.exception.InvalidDiscountException;
import java.time.Duration;
import java.util.Objects;

/**
 * The provider's discount (OFR-4): a rate above zero and up to 99% that applies when a reservation meets a minimum
 * number of hours and of helpers. No discount by default. The rate after the discount must still respect Homa's
 * minimum rate (OFR-11), which the listing checks against its hourly rate.
 */
public sealed interface DiscountRule {

    /** The discount rate a reservation of these hours and helpers gets; zero when it doesn't qualify. */
    Percentage rateFor(Duration hours, int helpers);

    /** The rate a qualifying reservation gets; zero for no discount. The minimum rate is checked against it. */
    Percentage rate();

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

        @Override
        public Percentage rate() {
            return Percentage.ZERO;
        }
    }

    /** Kemas Home's 10% for reservations of at least 3 hours and 2 helpers. */
    record Threshold(Percentage rate, Duration minHours, int minHelpers) implements DiscountRule {

        public Threshold {
            requireValidRate(rate);
            requireValidMinimums(minHours, minHelpers);
        }

        @Override
        public Percentage rateFor(Duration hours, int helpers) {
            boolean qualifies = hours.compareTo(minHours) >= 0 && helpers >= minHelpers;
            return qualifies ? rate : Percentage.ZERO;
        }

        private static void requireValidRate(Percentage rate) {
            Objects.requireNonNull(rate, "rate");
            if (rate.isZero()) {
                throw new InvalidDiscountException("A discount rate must be above zero; choose no discount instead");
            }
            if (rate.compareTo(ListingValues.MAXIMUM_DISCOUNT) > 0) {
                throw new InvalidDiscountException(
                        "A discount rate can't be over " + ListingValues.MAXIMUM_DISCOUNT + ", was " + rate);
            }
        }

        private static void requireValidMinimums(Duration minHours, int minHelpers) {
            Objects.requireNonNull(minHours, "minHours");
            if (minHours.isNegative() || minHours.isZero()) {
                throw new InvalidDiscountException("A discount needs a minimum number of hours, was " + minHours);
            }
            if (minHelpers < 1) {
                throw new InvalidDiscountException("A discount needs at least 1 helper as its minimum, was " + minHelpers);
            }
        }
    }
}
