package account.usecase.dto;

import java.time.Instant;

/** The KYC provider's final result for a business. The SSM number, registered name and verified-until date come only with a pass. */
public record RecordKybResultRequest(String providerId, boolean verified, String ssmNumber, String registeredName, Instant verifiedUntil) {
}
