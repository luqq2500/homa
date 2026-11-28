package com.homa.ie.events;

import com.homa.ie.port.SupportIE;
import java.time.Instant;

/**
 * Support confirmed a Provider No Show from the customer's report (NSH-1, NSH-2): the team never arrived, or left
 * before the job end without cancelling. Reservation cancels the reservation under the Provider No Show policy,
 * from Reserved or from Completed inside the Protection Window (NSH-3, NSH-6), and answers with
 * {@code ReportEffectAppliedIE}.
 *
 * @param reportId    the report Support confirmed
 * @param confirmedAt when Support confirmed it; the cancellation is as of this moment
 */
public record ProviderNoShowConfirmedIE(
        String eventId,
        String reservationId,
        String reportId,
        String customerId,
        String providerId,
        Instant confirmedAt,
        Instant occurredAt) implements SupportIE {
}
