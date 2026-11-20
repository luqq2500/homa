package com.homa.ie.events;

import com.homa.ie.port.PaymentIE;
import com.homa.kernel.Money;
import java.time.Instant;

/** The gateway confirmed the customer's payment; it is now held (PAY-1). Reservation makes it Requested (RES-7). */
public record PaymentHeldIE(String checkoutId, Money amount, Instant paidAt, Instant occurredAt) implements PaymentIE {
}
