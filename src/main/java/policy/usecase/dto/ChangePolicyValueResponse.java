package policy.usecase.dto;

import java.time.Instant;

/** The new version, from when it is in force, and the value as it now stands. */
public record ChangePolicyValueResponse(int version, Instant effectiveFrom, String value, String amount) {
}
