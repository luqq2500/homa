package verification.domain;

import java.time.Instant;

/** Who removed a person from the team, when, and for Homa's removals, why. */
public record Removal(String removedBy, String reason, Instant removedAt) {
}
