package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** The KYC provider verified a person being added; the provider confirms this named person before they join the team. */
public record MemberVerifiedIE(
        String verificationId,
        String providerId,
        String registeredName,
        Instant occurredAt) implements AccountIE {

    public MemberVerifiedIE {
        Objects.requireNonNull(verificationId, "verificationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(registeredName, "registeredName");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
