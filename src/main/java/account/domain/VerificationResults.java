package account.domain;

import account.domain.exception.VerificationDetailsRequiredException;
import account.domain.exception.VerificationResultNotFinalException;
import java.time.Instant;
import java.util.Optional;

/** The checks shared by every result from the KYC provider. */
final class VerificationResults {

    private VerificationResults() {
    }

    static void requireFinal(VerificationStatus status) {
        if (status != VerificationStatus.VERIFIED && status != VerificationStatus.REJECTED) {
            throw new VerificationResultNotFinalException();
        }
    }

    static void requireUntil(Optional<Instant> verifiedUntil) {
        if (verifiedUntil.isEmpty()) {
            throw new VerificationDetailsRequiredException("a verified-until date");
        }
    }
}
