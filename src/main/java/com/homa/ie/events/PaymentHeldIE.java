package com.homa.ie.events;

import com.homa.ie.port.PaymentIE;
import com.homa.kernel.Money;
import java.time.Instant;

/**
 * The first confirmed payment for a checkout; it is now held (PAY-1). A later confirmation for the same checkout is
 * refunded by Payment and never published (RES-12). Reservation makes it Requested if the time is still free (RES-7,
 * RES-11).
 */
public record PaymentHeldIE(
        String eventId,
        String checkoutId, Money amount, Instant paidAt, Instant occurredAt) implements PaymentIE {
}
