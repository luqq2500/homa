package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A verified person was added to a provider's team, with their registered name and the lawful-employment confirmation from the add form. */
public record TeamMemberAddedIE(
        String memberId,
        String providerId,
        String verificationId,
        String role,
        String registeredName,
        String employmentTermsVersion,
        Instant employmentConfirmedAt,
        Instant occurredAt) implements AccountIE {

    public TeamMemberAddedIE {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(verificationId, "verificationId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(registeredName, "registeredName");
        Objects.requireNonNull(employmentTermsVersion, "employmentTermsVersion");
        Objects.requireNonNull(employmentConfirmedAt, "employmentConfirmedAt");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
