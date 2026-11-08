package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A re-verification re-check opened for a provider or a team member, at the reminder or early on Homa's request. {@code helperId} is empty when the re-check is the provider's own; {@code earlyRecheckReason} is empty for the regular re-check. */
public record RecheckOpenedIE(
        String providerId,
        String helperId,
        Instant dueAt,
        String earlyRecheckReason,
        Instant occurredAt) implements AccountIE {

    public RecheckOpenedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(helperId, "helperId");
        Objects.requireNonNull(dueAt, "dueAt");
        Objects.requireNonNull(earlyRecheckReason, "earlyRecheckReason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
