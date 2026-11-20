package com.homa.payment.domain;

import com.homa.kernel.Money;
import java.time.Instant;
import java.util.Objects;

/**
 * The money lines of a released payout: the gross the Settlement gave the provider, Homa's platform fee taken from it
 * (PAY-3), and what remains after the platform fee. Payout RM200: fee RM31, RM169 after the fee.
 *
 * <p>A fine deduction (PAY-5, slice 3) slots in after the platform fee: it takes at most 20% of
 * {@link #afterPlatformFee()}, and becomes a further line here with the amount sent to the provider.
 */
public record PayoutRelease(PayoutId payoutId, ProviderId providerId, PayoutKind kind, Money gross,
        Money platformFee, Instant releasedAt) {

    public PayoutRelease {
        Objects.requireNonNull(payoutId, "payoutId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(gross, "gross");
        Objects.requireNonNull(platformFee, "platformFee");
        Objects.requireNonNull(releasedAt, "releasedAt");
    }

    /** Gross minus the rounded platform fee, so the lines always add up (PAY-8). The base for a fine deduction (PAY-5). */
    public Money afterPlatformFee() {
        return gross.minus(platformFee);
    }
}
