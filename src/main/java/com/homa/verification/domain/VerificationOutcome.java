package com.homa.verification.domain;

/** Whether the verification service passed a person's identity check (§11). Homa decides what follows (EXT-1). */
public enum VerificationOutcome {
    PASSED,
    FAILED
}
