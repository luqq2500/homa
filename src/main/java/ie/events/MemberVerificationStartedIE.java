package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider started adding a helper or supervisor, who is doing eKYC on the spot on the provider's phone; until verified they are waiting for verification and not on the team. */
public record MemberVerificationStartedIE(
        String verificationId,
        String providerId,
        String role,
        String documentType,
        String issuingCountry,
        Instant occurredAt) implements AccountIE {

    public MemberVerificationStartedIE {
        Objects.requireNonNull(verificationId, "verificationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(documentType, "documentType");
        Objects.requireNonNull(issuingCountry, "issuingCountry");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
