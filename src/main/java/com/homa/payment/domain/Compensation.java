package com.homa.payment.domain;

import com.homa.kernel.Money;
import java.util.Objects;

/**
 * Money Homa pays the customer on top of a full refund (PAY-4), decided now and paid now (PAY-7). Unlike a
 * {@link Refund} it does not come out of the held payment: Homa pays it from its own money and recovers it through
 * the provider's fine. It names the payment that counted, so the gateway knows where to send it.
 */
public record Compensation(PaymentHoldId holdId, CustomerId customerId, PaymentAttemptId attemptId, Money amount) {

    public Compensation {
        Objects.requireNonNull(holdId, "holdId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(attemptId, "attemptId");
        Objects.requireNonNull(amount, "amount");
    }
}
