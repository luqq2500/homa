package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** Adding a team member ended with nothing added: {@code reason} is REJECTED or EXPIRED. The provider can start again. */
public record MemberVerificationEndedIE(
        String verificationId,
        String providerId,
        String reason,
        Instant occurredAt) implements AccountIE {

    public MemberVerificationEndedIE {
        Objects.requireNonNull(verificationId, "verificationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
