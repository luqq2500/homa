package com.homa.listing.domain;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.listing.domain.exception.InvalidHourlyRateException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.util.Objects;

/** The provider's per-helper hourly rate, more than zero. Pricing is per helper per hour only (OFR-2). */
public record HourlyRate(Money perHelperPerHour) {

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    public HourlyRate {
        Objects.requireNonNull(perHelperPerHour, "perHelperPerHour");
        if (!perHelperPerHour.isPositive()) {
            throw new InvalidHourlyRateException("The hourly rate must be more than zero, was " + perHelperPerHour);
        }
    }

    /** Rate × hours × helpers, before any discount or travel fee: RM25 for 2 hours and 2 helpers is RM100. */
    public Money priceFor(Duration hours, int helpers) {
        BigDecimal fractionalHours = BigDecimal.valueOf(hours.toMinutes()).divide(MINUTES_PER_HOUR, MathContext.DECIMAL64);
        return perHelperPerHour.times(fractionalHours).times(helpers);
    }

    /** The per-helper hourly rate after a discount, exact: RM25 after 10% is RM22.50 (OFR-11). */
    public Money afterDiscount(Percentage discount) {
        return discount.complement().of(perHelperPerHour);
    }
}
