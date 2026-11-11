package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** The KYC provider's monitoring asks for a re-verification of a business or a team member by a date, for example after a director change or before a document expires; they keep working, and the verified-until date moves sooner. {@code memberId} is empty when it is the business's own. */
public record ReverificationRequiredIE(
        String providerId,
        String memberId,
        String reason,
        Instant verifiedUntil,
        Instant occurredAt) implements AccountIE {

    public ReverificationRequiredIE {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(verifiedUntil, "verifiedUntil");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
