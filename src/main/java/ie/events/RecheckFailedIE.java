package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** The KYC provider reports a failed re-verification of a business or a team member; nothing changes, and they are asked to retry. {@code memberId} is empty when it is the business's own. */
public record RecheckFailedIE(
        String providerId,
        String memberId,
        Instant occurredAt) implements AccountIE {

    public RecheckFailedIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
