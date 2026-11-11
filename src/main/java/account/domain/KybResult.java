package account.domain;

import account.domain.exception.VerificationDetailsRequiredException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A business's verification result from the KYC provider: KYB of the SSM registration, with the owner's
 * or director's KYC in the same flow. VERIFIED carries the registration and the date the verification is
 * good until; REJECTED carries nothing.
 */
public record KybResult(VerificationStatus status, Optional<BusinessRegistration> business, Optional<Instant> verifiedUntil) {

    public KybResult {
        VerificationResults.requireFinal(status);
        Objects.requireNonNull(business);
        Objects.requireNonNull(verifiedUntil);
        if (status == VerificationStatus.VERIFIED) {
            business.orElseThrow(() -> new VerificationDetailsRequiredException("the business registration"));
            VerificationResults.requireUntil(verifiedUntil);
        }
    }

    public static KybResult verified(BusinessRegistration business, Instant verifiedUntil) {
        return new KybResult(VerificationStatus.VERIFIED, Optional.ofNullable(business), Optional.ofNullable(verifiedUntil));
    }

    public static KybResult rejected() {
        return new KybResult(VerificationStatus.REJECTED, Optional.empty(), Optional.empty());
    }

    public boolean isVerified() {
        return status == VerificationStatus.VERIFIED;
    }
}
