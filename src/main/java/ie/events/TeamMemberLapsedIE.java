package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A team member missed their re-verification due date and left the team until they pass. */
public record TeamMemberLapsedIE(
        String memberId,
        String providerId,
        String role,
        Instant occurredAt) implements AccountIE {

    public TeamMemberLapsedIE {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
