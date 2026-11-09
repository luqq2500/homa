package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** The eKYC of a person being added was rejected; nothing is added, and the provider can start again. */
public record MemberVerificationRejectedIE(
        String verificationId,
        String providerId,
        Instant occurredAt) implements AccountIE {

    public MemberVerificationRejectedIE {
        Objects.requireNonNull(verificationId, "verificationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
