package com.homa.offering.domain;

import com.homa.kernel.Money;
import com.homa.offering.domain.exception.HourlyRateNotPositiveException;

/** The flat rate a provider charges per helper per hour. It is above zero. */
public record HourlyRate(Money perHelperPerHour) {

    public HourlyRate {
        if (perHelperPerHour.isNegative() || perHelperPerHour.isZero()) {
            throw new HourlyRateNotPositiveException(perHelperPerHour);
        }
    }

    public static HourlyRate of(Money perHelperPerHour) {
        return new HourlyRate(perHelperPerHour);
    }
}
