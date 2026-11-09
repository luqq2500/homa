package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A team member passed re-verification, or a lapsed one passed and rejoined the team. */
public record TeamMemberReverifiedIE(
        String memberId,
        String providerId,
        String role,
        Instant occurredAt) implements AccountIE {

    public TeamMemberReverifiedIE {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
