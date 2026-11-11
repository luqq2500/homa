package account.domain;

import account.domain.exception.VerificationDetailsRequiredException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A person's verification result from the KYC provider: VERIFIED, with the registered name from their
 * identity document and the date their verification is good until, or REJECTED. Which documents are
 * accepted, how the face is matched and when re-verification is due are the KYC provider's rules.
 */
public record KycResult(VerificationStatus status, Optional<String> registeredName, Optional<Instant> verifiedUntil) {

    public KycResult {
        VerificationResults.requireFinal(status);
        Objects.requireNonNull(registeredName);
        Objects.requireNonNull(verifiedUntil);
        if (status == VerificationStatus.VERIFIED) {
            registeredName = Optional.of(AccountText.require(
                    registeredName.orElseThrow(() -> new VerificationDetailsRequiredException("a registered name")),
                    "A registered name"));
            VerificationResults.requireUntil(verifiedUntil);
        }
    }

    public static KycResult verified(String registeredName, Instant verifiedUntil) {
        return new KycResult(VerificationStatus.VERIFIED, Optional.ofNullable(registeredName), Optional.ofNullable(verifiedUntil));
    }

    public static KycResult rejected() {
        return new KycResult(VerificationStatus.REJECTED, Optional.empty(), Optional.empty());
    }

    public boolean isVerified() {
        return status == VerificationStatus.VERIFIED;
    }
}
