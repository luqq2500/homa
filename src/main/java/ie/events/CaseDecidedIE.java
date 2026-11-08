package ie.events;

import ie.port.ResolutionIE;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import kernel.Money;

/** The founders decided a case, with a reason. */
public record CaseDecidedIE(
        String caseId,
        String reservationId,
        String outcome,
        Optional<Money> providerReceives,
        String reason,
        Instant occurredAt) implements ResolutionIE {

    public CaseDecidedIE {
        Objects.requireNonNull(caseId, "caseId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(providerReceives, "providerReceives");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
