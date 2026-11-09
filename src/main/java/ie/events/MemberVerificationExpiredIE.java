package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A person being added did not finish eKYC within the verification link expiry; nothing is added, and the provider can start again. */
public record MemberVerificationExpiredIE(
        String verificationId,
        String providerId,
        Instant occurredAt) implements AccountIE {

    public MemberVerificationExpiredIE {
        Objects.requireNonNull(verificationId, "verificationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
