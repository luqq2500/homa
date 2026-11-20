package com.homa.provider.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The result of a candidate helper's or supervisor's identity verification (KYC), which the domain accepts and
 * judges. {@code outcome} says whether the identity documents verified the candidate as a citizen (HLP-2) or as
 * legally allowed to work in the country (HLP-3); {@code dateOfBirth} is read from those documents (HLP-1).
 */
public record MemberVerification(VerificationOutcome outcome, LocalDate dateOfBirth, Instant verifiedAt) {

    public MemberVerification {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(dateOfBirth, "dateOfBirth");
        Objects.requireNonNull(verifiedAt, "verifiedAt");
    }
}
