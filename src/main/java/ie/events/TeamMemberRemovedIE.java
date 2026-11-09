package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** The provider, or Homa, removed a team member. */
public record TeamMemberRemovedIE(
        String memberId,
        String providerId,
        String role,
        Instant occurredAt) implements AccountIE {

    public TeamMemberRemovedIE {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
