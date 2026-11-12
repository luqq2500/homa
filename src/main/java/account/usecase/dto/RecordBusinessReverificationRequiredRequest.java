package account.usecase.dto;

import java.time.Instant;

/** The business, why, and the date to re-verify by. */
public record RecordBusinessReverificationRequiredRequest(String providerId, String reason, Instant deadline) {
}
