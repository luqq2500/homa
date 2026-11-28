package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import com.homa.kernel.Money;
import java.time.Instant;

/**
 * A Reserved reservation was cancelled, with the settlement Reservation worked out (§6, §7; plan decision 1).
 * Payment executes the lines and never re-derives them (PAY-4 to PAY-7); Rating reads {@code cause} for reliability
 * counts (RTG-10); Conversation turns the chat read-only at once only when {@code cancelledAt} is before
 * {@code checkInStart} (CHT-5).
 *
 * <p>The lines are the plan's Settlement: each is rounded once to the sen (PAY-8) and the fee and fine are each
 * capped at the Total Reservation Fee (CAN-2). {@code customerRefund + customerFee} equals what the customer paid.
 * Payment's idempotency key for each money call is {@code settlementId} plus the line name, so a replayed event
 * cannot pay twice.
 *
 * @param cause CUSTOMER_CANCELLED, PROVIDER_CANCELLED, CUSTOMER_LAST_MINUTE, PROVIDER_LAST_MINUTE, NO_ROSTER,
 *              PROVIDER_NO_SHOW, CUSTOMER_NO_SHOW, CUSTOMER_SUSPENDED or PROVIDER_SUSPENDED
 * @param settlementId unique per settlement, assigned by Reservation
 * @param customerFee what the customer pays the provider (CCX-6, LMC-1, NSH-5); zero for a free cancellation
 * @param customerRefund what goes back to the customer from the held payment, paid at once (PAY-7)
 * @param compensation the extra amount Homa pays the customer on top of a full refund (PAY-4); zero when none
 * @param providerFine the fine added to the provider's account (PAY-4, PAY-5); zero when none
 * @param providerPayoutGross what the provider is owed from this reservation: the customer fee, as a FEE payout held
 *                            until the protection window ends (PAY-6); zero when none
 */
public record ReservationCancelledIE(
        String eventId,
        String reservationId,
        String customerId,
        String providerId,
        String cause,
        Instant cancelledAt,
        Instant requestedAt,
        Instant checkInStart,
        Instant protectionWindowEnd,
        String settlementId,
        Money customerFee,
        Money customerRefund,
        Money compensation,
        Money providerFine,
        Money providerPayoutGross,
        Instant occurredAt) implements ReservationIE {
}
