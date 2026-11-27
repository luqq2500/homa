package com.homa.verification.domain;

import java.util.UUID;

/** Identifies one verification a person started: one check by the verification service, for one role. */
public record VerificationAttemptId(String value) {

    public VerificationAttemptId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A verification attempt id must not be blank");
        }
    }

    /** A new, unique verification attempt id. */
    public static VerificationAttemptId generate() {
        return new VerificationAttemptId(UUID.randomUUID().toString());
    }
}
