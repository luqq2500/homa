package account.usecase.dto;

import java.time.Instant;

/** The KYC provider's final result for a person being added. The registered name and verified-until date come only with a pass. */
public record RecordMemberVerificationResultRequest(String verificationId, boolean verified, String registeredName, Instant verifiedUntil) {
}
