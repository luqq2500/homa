package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** The provider marked the job completed with evidence. */
public record CompletionSubmittedIE(
        String reservationId,
        List<String> evidence,
        Instant disputeWindowEndsAt,
        Instant occurredAt) implements ReservationIE {

    public CompletionSubmittedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        evidence = List.copyOf(evidence);
        Objects.requireNonNull(disputeWindowEndsAt, "disputeWindowEndsAt");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
