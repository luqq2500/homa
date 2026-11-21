package com.homa.payment.domain;

import com.homa.kernel.Money;
import java.util.Objects;

/**
 * Money returned to the customer (PAY-4), decided now and paid now (PAY-7): a value to send through the gateway
 * straight away, never queued. It names the payment attempt it goes back to: the held payment, or a later payment
 * for the same checkout that never counted (RES-12).
 */
public record Refund(PaymentHoldId holdId, CustomerId customerId, PaymentAttemptId attemptId, Money amount) {

    public Refund {
        Objects.requireNonNull(holdId, "holdId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(attemptId, "attemptId");
        Objects.requireNonNull(amount, "amount");
    }
}
