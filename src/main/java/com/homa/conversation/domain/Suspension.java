package com.homa.conversation.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * The suspension Support decided for a customer, as Conversation needs to know it (CUS-4, CUS-6): when it starts, and
 * when it ends by itself. A 7- or 30-day suspension has an end; one until Support lifts it has none, and ends only
 * when Support lifts it, which Customer tells with {@code CustomerReactivatedIE}.
 *
 * @param from   when the suspension starts
 * @param endsAt when it ends by itself, or {@code null} for a suspension until it is lifted
 */
public record Suspension(Instant from, Instant endsAt) {

    public Suspension {
        Objects.requireNonNull(from, "from");
        if (endsAt != null && !endsAt.isAfter(from)) {
            throw new IllegalArgumentException("A suspension that ends must end after it starts");
        }
    }

    /** True from the moment the suspension starts until it ends: at its end for a timed one, or when lifted (CUS-6). */
    public boolean isInForceAt(Instant now) {
        return !now.isBefore(from) && (endsAt == null || now.isBefore(endsAt));
    }
}
