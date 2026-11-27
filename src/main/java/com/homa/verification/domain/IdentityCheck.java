package com.homa.verification.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The facts the verification service confirmed about one person (§11): their name, local or foreign status, the
 * identity document they verified with, their date of birth and the document's expiry, the outcome and when it was
 * checked. Homa decides what follows from them (EXT-1).
 *
 * <p>It has no field for a document image or biometric data, so none can reach Homa (IDN-2, EXT-4). The date of
 * birth and the document's expiry are read once, by {@link VerifiedPerson#verify}, and only the results are kept.
 *
 * @param documentExpiry the document's expiry date, or null when it has none (a MyKad has none)
 */
public record IdentityCheck(
        PersonName name,
        PersonStatus status,
        IdentityDocument document,
        LocalDate dateOfBirth,
        LocalDate documentExpiry,
        VerificationOutcome outcome,
        Instant checkedAt) {

    public IdentityCheck {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(dateOfBirth, "dateOfBirth");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(checkedAt, "checkedAt");
    }
}
