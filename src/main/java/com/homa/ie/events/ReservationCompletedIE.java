package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import com.homa.kernel.Money;
import java.time.Instant;

/**
 * The provider marked the job Completed (JOB-3). Payment creates the JOB payout, releasable when the protection
 * window ends (PAY-2); Rating opens its window (RTG-2).
 */
public record ReservationCompletedIE(
        String reservationId,
        String customerId,
        String providerId,
        Money payoutGross,
        Instant protectionWindowEnd,
        Instant completedAt,
        Instant occurredAt) implements ReservationIE {
}
