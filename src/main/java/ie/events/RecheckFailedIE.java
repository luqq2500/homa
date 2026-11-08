package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider or team member failed a re-check, or a lapsed one failed again; nothing changes yet, and they are asked to retry. {@code helperId} is empty when the re-check is the provider's own. */
public record RecheckFailedIE(
        String providerId,
        String helperId,
        Instant dueAt,
        Instant occurredAt) implements AccountIE {

    public RecheckFailedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(helperId, "helperId");
        Objects.requireNonNull(dueAt, "dueAt");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
