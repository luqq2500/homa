package com.homa.verification.usecase.port.out;

import com.homa.verification.domain.PersonStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The verification service's answer to one check: the facts it confirmed (§11), or that it didn't pass the
 * document. Never a document image or biometric data (EXT-4).
 */
public sealed interface VerificationAnswer {

    /** When the service checked. */
    Instant checkedAt();

    /**
     * The service passed the document and confirmed these facts.
     *
     * @param documentType   the document as the country calls it, such as "MyKad"
     * @param documentExpiry the document's expiry, or null when it has none (a MyKad has none)
     */
    record Passed(
            String name,
            PersonStatus status,
            String documentType,
            String documentNumber,
            LocalDate dateOfBirth,
            LocalDate documentExpiry,
            Instant checkedAt) implements VerificationAnswer {

        /** Withholds the number (IDN-3), the name and the date of birth. */
        @Override
        public String toString() {
            return "Passed[status=" + status + ", documentType=" + documentType + ", checkedAt=" + checkedAt + "]";
        }
    }

    /** The service didn't pass the document. */
    record NotPassed(Instant checkedAt) implements VerificationAnswer {
    }
}
