package ie.events;

import ie.port.ResolutionIE;
import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/** The admins decided a case, with a reason. {@code providerReceives} is what is at stake for a release, RM0.00 for a full refund, or the decided amount for a partial refund. */
public record CaseDecidedIE(
        String caseId,
        String reservationId,
        String outcome,
        Money providerReceives,
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
