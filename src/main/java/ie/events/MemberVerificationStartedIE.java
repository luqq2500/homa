package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider started adding a helper or supervisor, who verifies with the KYC provider on the spot on the provider's phone; until verified they are waiting for verification and not on the team. */
public record MemberVerificationStartedIE(
        String verificationId,
        String providerId,
        String role,
        Instant occurredAt) implements AccountIE {

    public MemberVerificationStartedIE {
        Objects.requireNonNull(verificationId, "verificationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
