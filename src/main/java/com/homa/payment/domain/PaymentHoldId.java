package com.homa.payment.domain;

import java.util.UUID;

/** Identifies a {@link PaymentHold}. Payment owns it and generates it. */
public record PaymentHoldId(String value) {

    public PaymentHoldId {
        Ids.requireText(value, "PaymentHoldId");
    }

    /** A new, unique id. */
    public static PaymentHoldId generate() {
        return new PaymentHoldId(UUID.randomUUID().toString());
    }
}
