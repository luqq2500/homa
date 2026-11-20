package com.homa.payment.domain;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;

/**
 * Homa's platform fee: 15.5% of the provider's released payout for a reservation (PAY-3). A business constant, not
 * configuration: changing it is a change to the Homa Business Rule.
 */
public final class PlatformFeeRate {

    /** The one rate Homa charges. */
    public static final PlatformFeeRate HOMA = new PlatformFeeRate(Percentage.of("15.5"));

    private final Percentage percentage;

    private PlatformFeeRate(Percentage percentage) {
        this.percentage = percentage;
    }

    public Percentage percentage() {
        return percentage;
    }

    /** The platform fee on a payout, a money line rounded once to the sen, half up (PAY-8): RM200 gives RM31. */
    public Money feeOn(Money payout) {
        return percentage.of(payout).roundedToSen();
    }
}
