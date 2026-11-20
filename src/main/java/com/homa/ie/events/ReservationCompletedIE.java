package com.homa.ie.events;

import com.homa.ie.port.ReservationIE;
import com.homa.kernel.Money;
import java.time.Instant;
import java.util.List;

/**
 * The provider marked the job Completed (JOB-3). Payment creates the JOB payout, releasable when the protection
 * window ends (PAY-2); Rating opens its window (RTG-2); Support keeps the completion photo refs as evidence
 * (JOB-6, NSH-6).
 */
public record ReservationCompletedIE(
        String eventId,
        String reservationId,
        String customerId,
        String providerId,
        Money payoutGross,
        Instant protectionWindowEnd,
        Instant completedAt,
        List<String> completionPhotoRefs,
        Instant occurredAt) implements ReservationIE {

    public ReservationCompletedIE {
        completionPhotoRefs = List.copyOf(completionPhotoRefs);
    }
}
