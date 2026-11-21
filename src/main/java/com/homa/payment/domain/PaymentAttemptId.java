package com.homa.payment.domain;

import java.util.UUID;

/**
 * One attempt by the customer to pay for a checkout: a card payment, a retry, an online banking payment. A checkout
 * can see several attempts; only the first one confirmed counts (P1, RES-12). Payment generates it and hands it to
 * the gateway as the payment's reference, so a refund goes back to the payment it came from.
 */
public record PaymentAttemptId(String value) {

    public PaymentAttemptId {
        Ids.requireText(value, "PaymentAttemptId");
    }

    /** A new, unique id. */
    public static PaymentAttemptId generate() {
        return new PaymentAttemptId(UUID.randomUUID().toString());
    }
}
