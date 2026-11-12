package account.domain;

import account.domain.exception.VerificationDetailsRequiredException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A person's verification result from the KYC provider: VERIFIED, with the registered name from their
 * identity document, the date their verification is good until and, when the check gives it, their date of
 * birth; or REJECTED. Which documents are
 * accepted, how the face is matched and when re-verification is due are the KYC provider's rules.
 */
public record KycResult(
        VerificationStatus status, Optional<String> registeredName, Optional<Instant> verifiedUntil, Optional<LocalDate> dateOfBirth) {

    public KycResult {
        VerificationResults.requireFinal(status);
        Objects.requireNonNull(registeredName);
        Objects.requireNonNull(verifiedUntil);
        Objects.requireNonNull(dateOfBirth);
        if (status == VerificationStatus.VERIFIED) {
            registeredName = Optional.of(AccountText.require(
                    registeredName.orElseThrow(() -> new VerificationDetailsRequiredException("a registered name")),
                    "A registered name"));
            VerificationResults.requireUntil(verifiedUntil);
        }
    }

    /** A pass with no date of birth, as a re-verification reports it. */
    public static KycResult verified(String registeredName, Instant verifiedUntil) {
        return new KycResult(VerificationStatus.VERIFIED, Optional.ofNullable(registeredName), Optional.ofNullable(verifiedUntil), Optional.empty());
    }

    /** A pass with the date of birth from the document, as the check of a person being added reports it. */
    public static KycResult verified(String registeredName, Instant verifiedUntil, LocalDate dateOfBirth) {
        return new KycResult(
                VerificationStatus.VERIFIED, Optional.ofNullable(registeredName), Optional.ofNullable(verifiedUntil), Optional.ofNullable(dateOfBirth));
    }

    public static KycResult rejected() {
        return new KycResult(VerificationStatus.REJECTED, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public boolean isVerified() {
        return status == VerificationStatus.VERIFIED;
    }
}
