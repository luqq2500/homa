package account.usecase.dto;

import java.time.Instant;

/** The KYC provider's final re-verification result for a team member. The registered name and verified-until date come only with a pass. */
public record RecordTeamMemberResultRequest(String memberId, boolean verified, String registeredName, Instant verifiedUntil) {
}
