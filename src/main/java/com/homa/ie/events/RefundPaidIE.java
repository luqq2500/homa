package com.homa.ie.events;

import com.homa.ie.port.PaymentIE;
import com.homa.kernel.Money;
import java.time.Instant;

/**
 * Money Payment sent back to a customer the moment it was decided (PAY-7): a refund from the held payment (PAY-4), or
 * the compensation Homa pays on top of one. "Paid" means sent, never arrived: arrival depends on the banks (EXT-3).
 * Notification tells the customer.
 *
 * @param reservationId the reservation the money is about
 * @param customerId    who it was sent to
 * @param amount        what was sent
 * @param reason        WITHDRAWN, DECLINED or EXPIRED (a Requested reservation that ended, REQ-2 to REQ-4);
 *                      CANCELLED (the refund line of a cancellation settlement); SMALLER_ROSTER (the difference when
 *                      the customer accepted a smaller roster, ROS-8); COMPENSATION (what Homa pays on top of a full
 *                      refund, PAY-4)
 * @param sentAt        when Payment sent it; this is also {@link #occurredAt()}
 */
public record RefundPaidIE(
        String eventId,
        String reservationId,
        String customerId,
        Money amount,
        String reason,
        Instant sentAt) implements PaymentIE {

    @Override
    public Instant occurredAt() {
        return sentAt;
    }
}
