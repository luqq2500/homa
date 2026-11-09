package account.domain;

import account.domain.exception.EkycDocumentRequiredException;
import account.domain.exception.EkycResultNotFinalException;
import java.util.Objects;
import java.util.Optional;

/**
 * A result from Kenal: VERIFIED, with the identity document it read, or REJECTED.
 *
 * @param document the document and registered name read by the eKYC; required when verified
 */
public record EkycResult(VerificationStatus status, Optional<IdentityDocument> document) {

    public EkycResult {
        if (status != VerificationStatus.VERIFIED && status != VerificationStatus.REJECTED) {
            throw new EkycResultNotFinalException();
        }
        Objects.requireNonNull(document);
        if (status == VerificationStatus.VERIFIED && document.isEmpty()) {
            throw new EkycDocumentRequiredException();
        }
    }

    public static EkycResult verified(IdentityDocument document) {
        return new EkycResult(VerificationStatus.VERIFIED, Optional.of(document));
    }

    public static EkycResult rejected() {
        return new EkycResult(VerificationStatus.REJECTED, Optional.empty());
    }

    public boolean isVerified() {
        return status == VerificationStatus.VERIFIED;
    }

    /** The verified document; only called on a verified result. */
    IdentityDocument verifiedDocument() {
        return document.orElseThrow();
    }
}
