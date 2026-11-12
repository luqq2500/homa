package account.usecase.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The KYC provider's final result for a person being added. The registered name, verified-until date and
 * date of birth come only with a pass. {@code photoMatches} is the KYC provider's comparison of the profile
 * photo with the verified selfie.
 */
public record RecordMemberVerificationResultRequest(
        String verificationId, boolean verified, String registeredName, Instant verifiedUntil, LocalDate dateOfBirth, boolean photoMatches) {
}
