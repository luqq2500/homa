package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import com.homa.kernel.Money;
import java.time.Instant;

/**
 * A reservation's price came down after it was Reserved, without a cancellation: Payment refunds the difference at
 * once (PAY-7) and lowers the provider's payout by the same amount. Reservation worked the amounts out; Payment
 * never re-derives them (plan decision 1). Each money call is keyed by {@code adjustmentId} (idempotency).
 *
 * @param adjustmentId unique per adjustment, assigned by Reservation
 * @param reason SMALLER_ROSTER (ROS-8). Later slices add MEMBER_LEFT (RPT-9), JOB_STOPPED (SUS-3) and
 *               SUPPORT_REFUND (RPT-5)
 * @param customerRefund what goes back to the customer from the held payment, paid at once
 * @param newTotal what the reservation costs after the adjustment: the payout's gross now
 */
public record SettlementAdjustedIE(
        String eventId,
        String reservationId,
        String customerId,
        String providerId,
        String adjustmentId,
        String reason,
        Money customerRefund,
        Money newTotal,
        Instant adjustedAt,
        Instant occurredAt) implements ReservationIE {
}
