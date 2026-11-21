package com.homa.reservation.domain.terms;

import com.homa.kernel.Percentage;
import com.homa.reservation.domain.JobDuration;
import java.time.Duration;
import java.util.Objects;

/**
 * OFR-4: a discount rate that applies when a reservation has at least a minimum number of hours and helpers.
 * {@link #NONE} is the default: no discount.
 */
public record DiscountRule(Percentage rate, Duration minimumHours, int minimumHelpers) {

    public static final DiscountRule NONE = new DiscountRule(Percentage.ZERO, Duration.ZERO, 0);

    public DiscountRule {
        Objects.requireNonNull(rate, "rate");
        Objects.requireNonNull(minimumHours, "minimumHours");
    }

    /** The discount rate for these hours and helpers: the rule's rate when both minimums are met, else 0%. */
    public Percentage rateFor(JobDuration hours, int helpers) {
        return qualifies(hours, helpers) ? rate : Percentage.ZERO;
    }

    private boolean qualifies(JobDuration hours, int helpers) {
        return hours.length().compareTo(minimumHours) >= 0 && helpers >= minimumHelpers;
    }
}
