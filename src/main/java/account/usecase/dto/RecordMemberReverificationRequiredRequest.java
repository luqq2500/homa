package account.usecase.dto;

import java.time.Instant;

/** The team member, why, and the date to re-verify by. */
public record RecordMemberReverificationRequiredRequest(String memberId, String reason, Instant deadline) {
}
