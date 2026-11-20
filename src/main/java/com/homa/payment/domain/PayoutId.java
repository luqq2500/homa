package com.homa.payment.domain;

import java.util.UUID;

/** Identifies a {@link Payout}. Payment owns it and generates it. */
public record PayoutId(String value) {

    public PayoutId {
        Ids.requireText(value, "PayoutId");
    }

    /** A new, unique id. */
    public static PayoutId generate() {
        return new PayoutId(UUID.randomUUID().toString());
    }
}
