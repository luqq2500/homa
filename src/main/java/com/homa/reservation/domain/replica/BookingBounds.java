package com.homa.reservation.domain.replica;

import java.time.Duration;
import java.util.Objects;

/**
 * The provider's minimum and maximum hours and helpers, and the step durations increase in, as Listing publishes
 * them (OFR-5, RES-5). Listing alone defines Homa's 1-hour floor and 15-minute steps; Reservation receives them here.
 * Customers can book at most the smaller of the maximum helpers and the provider's active helpers.
 */
public record BookingBounds(
        Duration minimumHours, Duration maximumHours, Duration step, int minimumHelpers, int maximumHelpers) {

    public BookingBounds {
        Objects.requireNonNull(minimumHours, "minimumHours");
        Objects.requireNonNull(maximumHours, "maximumHours");
        Objects.requireNonNull(step, "step");
        if (!step.isPositive()) {
            throw new IllegalArgumentException("A booking step must be positive, was " + step);
        }
    }

    boolean admitsHelpers(int helpers, int activeHelpers) {
        return helpers >= minimumHelpers && helpers <= Math.min(maximumHelpers, activeHelpers);
    }
}
