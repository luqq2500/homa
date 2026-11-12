package ie.events;

import ie.port.ResolutionIE;
import java.time.Instant;
import java.util.Objects;

/** A seriously flagged provider's reservation was taken over by the flag, so its open case closed with no decision. */
public record CaseClosedForProviderFlagIE(
        String caseId,
        String reservationId,
        Instant occurredAt) implements ResolutionIE {

    public CaseClosedForProviderFlagIE {
        Objects.requireNonNull(caseId, "caseId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
