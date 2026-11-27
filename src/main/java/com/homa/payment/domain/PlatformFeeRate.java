package com.homa.payment.domain;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import java.time.Instant;
import java.util.Objects;

/**
 * Homa's platform fee on every payout to a provider, including cancellation, last-minute and no-show fees (PAY-3).
 * The rate is a reservation value (§13): it comes from {@link PaymentValues}, in the version in force when the
 * reservation was Requested (P3).
 */
public record PlatformFeeRate(Percentage percentage) {

    public PlatformFeeRate {
        Objects.requireNonNull(percentage, "percentage");
    }

    /** The rate for a reservation Requested at {@code requestedAt}. */
    public static PlatformFeeRate asOf(Instant requestedAt) {
        return of(PaymentValues.asOf(requestedAt));
    }

    static PlatformFeeRate of(PaymentValues.Version version) {
        return new PlatformFeeRate(version.platformFeeRate());
    }

    /** The platform fee on a payout, a money line rounded once to the sen, half up (PAY-8): RM45 gives RM6.98. */
    public Money feeOn(Money payout) {
        return percentage.of(payout).roundedToSen();
    }
}
