package account.domain;

import account.domain.exception.EkycResultNotFinalException;
import account.domain.exception.EkybDetailsRequiredException;
import java.util.Objects;
import java.util.Optional;

/**
 * A result from the eKYB provider for a business: VERIFIED, with the SSM registration it checked and the
 * identity document of the owner or director whose eKYC is part of the same flow, or REJECTED.
 *
 * @param business the business registration; required when verified
 * @param director the owner's or director's document from the eKYC within the eKYB; required when verified
 */
public record EkybResult(VerificationStatus status, Optional<BusinessRegistration> business, Optional<IdentityDocument> director) {

    public EkybResult {
        if (status != VerificationStatus.VERIFIED && status != VerificationStatus.REJECTED) {
            throw new EkycResultNotFinalException();
        }
        Objects.requireNonNull(business);
        Objects.requireNonNull(director);
        if (status == VerificationStatus.VERIFIED) {
            requireDetails(business, director);
        }
    }

    private static void requireDetails(Optional<BusinessRegistration> business, Optional<IdentityDocument> director) {
        if (business.isEmpty() || director.isEmpty()) {
            throw new EkybDetailsRequiredException();
        }
    }

    public static EkybResult verified(BusinessRegistration business, IdentityDocument director) {
        return new EkybResult(VerificationStatus.VERIFIED, Optional.of(business), Optional.of(director));
    }

    public static EkybResult rejected() {
        return new EkybResult(VerificationStatus.REJECTED, Optional.empty(), Optional.empty());
    }

    public boolean isVerified() {
        return status == VerificationStatus.VERIFIED;
    }
}
