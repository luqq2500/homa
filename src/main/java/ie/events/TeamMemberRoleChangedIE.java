package ie.events;

import ie.port.AccountIE;
import java.time.Instant;
import java.util.Objects;

/** A provider changed a team member between helper and supervisor, without a new check. */
public record TeamMemberRoleChangedIE(
        String memberId,
        String providerId,
        String role,
        Instant occurredAt) implements AccountIE {

    public TeamMemberRoleChangedIE {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
