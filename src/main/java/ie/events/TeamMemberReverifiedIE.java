package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** The KYC provider re-verified a team member, or a lapsed one passed and rejoined the team. */
public record TeamMemberReverifiedIE(
        String memberId,
        String providerId,
        String role,
        Instant verifiedUntil,
        Instant occurredAt) implements AccountIE {

    public TeamMemberReverifiedIE {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(verifiedUntil, "verifiedUntil");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
