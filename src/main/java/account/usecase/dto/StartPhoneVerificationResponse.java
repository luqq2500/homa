package account.usecase.dto;

import java.time.Instant;

/** The verification to confirm, and when its code expires. */
public record StartPhoneVerificationResponse(String verificationId, Instant expiresAt) {
}
