package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.List;

/** The provider marked the job completed with evidence. */
public record CompletionSubmittedIE(String reservationId, String customerId, String providerId, List<String> evidence, Instant disputeWindowEndsAt, Instant occurredAt) implements ReservationIE {

    public CompletionSubmittedIE {
        evidence = List.copyOf(evidence);
    }
}
