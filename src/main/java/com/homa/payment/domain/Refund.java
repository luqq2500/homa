package com.homa.payment.domain;

import com.homa.kernel.Money;
import java.util.Objects;

/** Money returned to the customer from a held payment (PAY-4), ready to be sent through the gateway. */
public record Refund(PaymentHoldId holdId, CustomerId customerId, Money amount) {

    public Refund {
        Objects.requireNonNull(holdId, "holdId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(amount, "amount");
    }
}
