package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** Adding a person ended without adding them, with the reason: the check was rejected, they are under 18, or the photo didn't match. The provider can start again. */
public record MemberVerificationRejectedIE(
        String verificationId,
        String providerId,
        String reason,
        Instant occurredAt) implements AccountIE {

    public MemberVerificationRejectedIE {
        Objects.requireNonNull(verificationId, "verificationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
