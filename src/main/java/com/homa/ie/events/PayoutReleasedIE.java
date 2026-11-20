package com.homa.ie.events;

import com.homa.ie.port.PaymentIE;
import com.homa.kernel.Money;
import java.time.Instant;

/** A payout was released to the provider (PAY-2, PAY-3): gross, platform fee and the amount sent. */
public record PayoutReleasedIE(
        String payoutId, String reservationId, String providerId, String kind,
        Money gross, Money platformFee, Money net, Instant occurredAt) implements PaymentIE {
}
