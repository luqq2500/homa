package com.homa.reservation.domain;

import com.homa.reservation.domain.settlement.Settlement;
import java.time.Instant;
import java.util.Objects;

/**
 * How a Reserved reservation was cancelled: when, why ({@link CancellationCause}), and the {@link Settlement} that
 * Payment executes. Reliability counts (RTG-10) read the cause and whether the customer paid a fee. A withdrawal, a
 * decline and an expiry are not cancellations, so they have none.
 */
public record Cancellation(Instant at, CancellationCause cause, Settlement settlement) {

    public Cancellation {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(cause, "cause");
        Objects.requireNonNull(settlement, "settlement");
    }
}
