package com.homa.reservation.domain.replica;

import com.homa.reservation.domain.QuarterHourDuration;
import java.time.Duration;
import java.util.Objects;

/**
 * The provider's minimum and maximum hours and helpers (OFR-5). Customers can book at most the smaller of the
 * maximum helpers and the provider's active helpers.
 */
public record BookingBounds(Duration minimumHours, Duration maximumHours, int minimumHelpers, int maximumHelpers) {

    public BookingBounds {
        Objects.requireNonNull(minimumHours, "minimumHours");
        Objects.requireNonNull(maximumHours, "maximumHours");
    }

    boolean admitsHours(QuarterHourDuration hours) {
        return hours.length().compareTo(minimumHours) >= 0 && hours.length().compareTo(maximumHours) <= 0;
    }

    boolean admitsHelpers(int helpers, int activeHelpers) {
        return helpers >= minimumHelpers && helpers <= Math.min(maximumHelpers, activeHelpers);
    }
}
