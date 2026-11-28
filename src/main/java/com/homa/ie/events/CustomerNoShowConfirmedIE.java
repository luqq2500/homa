package com.homa.ie.events;

import com.homa.ie.port.SupportIE;
import java.time.Instant;

/**
 * Support confirmed a Customer No Show from the provider's report with its photo and location (NSH-4). Reservation
 * cancels the reservation under the Customer No Show policy (NSH-5) and answers with {@code ReportEffectAppliedIE}.
 *
 * @param reportId    the report Support confirmed
 * @param confirmedAt when Support confirmed it; the cancellation is as of this moment
 */
public record CustomerNoShowConfirmedIE(
        String eventId,
        String reservationId,
        String reportId,
        String customerId,
        String providerId,
        Instant confirmedAt,
        Instant occurredAt) implements SupportIE {
}
