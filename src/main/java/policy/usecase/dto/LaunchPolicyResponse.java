package policy.usecase.dto;

import java.time.Instant;

/** The version put in force, and from when. */
public record LaunchPolicyResponse(int version, Instant effectiveFrom) {
}
