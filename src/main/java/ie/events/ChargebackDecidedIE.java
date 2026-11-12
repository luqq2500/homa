package ie.events;

import ie.port.ResolutionIE;
import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/** The founders decided a chargeback case. What the provider covers is recovered from their next payouts like a fine; RM0.00 when Homa contests it. */
public record ChargebackDecidedIE(
        String caseId,
        String reservationId,
        Money providerCovers,
        String reason,
        Instant occurredAt) implements ResolutionIE {

    public ChargebackDecidedIE {
        Objects.requireNonNull(caseId, "caseId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(providerCovers, "providerCovers");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
