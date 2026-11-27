package com.homa.verification.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A verification result Homa keeps (IDN-2): the outcome, when it was checked and whether the age check passed. The
 * age check is kept as its result, not as the date of birth, which IDN-2 doesn't list. For Siti: passed,
 * 20 November, of age.
 *
 * <p>Hook (slice 6): re-verification adds later results, and a failed or missed one lapses the person (MON-3).
 */
public record VerificationResult(VerificationOutcome outcome, Instant checkedAt, boolean ageCheckPassed) {

    public VerificationResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(checkedAt, "checkedAt");
    }
}
