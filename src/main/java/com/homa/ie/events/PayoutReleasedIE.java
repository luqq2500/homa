package com.homa.ie.events;

import com.homa.ie.port.PaymentIE;
import com.homa.kernel.Money;
import java.time.Instant;

/**
 * A payout was released to the provider (PAY-2, PAY-3, PAY-5). The lines always add up:
 * {@code gross = platformFee + fineDeduction + net}. "Released" means sent, never arrived (EXT-3).
 *
 * @param kind          JOB or FEE
 * @param gross         what the provider was owed before any deduction
 * @param platformFee   Homa's platform fee, taken from every payout (PAY-3)
 * @param fineDeduction the part of the provider's fines cleared from this payout, after the platform fee; zero when
 *                      the provider owes nothing (PAY-5)
 * @param net           what was sent to the provider
 */
public record PayoutReleasedIE(
        String eventId,
        String payoutId, String reservationId, String providerId, String kind,
        Money gross, Money platformFee, Money fineDeduction, Money net, Instant occurredAt) implements PaymentIE {
}
