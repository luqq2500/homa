package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider or team member failed a re-check, or a lapsed one failed again; nothing changes yet, and they are asked to retry. {@code memberId} is empty when the re-check is the provider's own. */
public record RecheckFailedIE(
        String providerId,
        String memberId,
        Instant dueAt,
        Instant occurredAt) implements AccountIE {

    public RecheckFailedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(dueAt, "dueAt");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
