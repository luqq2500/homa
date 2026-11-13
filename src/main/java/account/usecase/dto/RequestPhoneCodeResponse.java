package account.usecase.dto;

import java.time.Instant;

public record RequestPhoneCodeResponse(String verificationId, Instant expiresAt) {}
