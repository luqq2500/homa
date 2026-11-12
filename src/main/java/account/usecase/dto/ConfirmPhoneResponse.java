package account.usecase.dto;

/** Whether the phone is now confirmed, and how many wrong codes are left before a new one is needed. */
public record ConfirmPhoneResponse(String verificationId, boolean confirmed, int attemptsLeft) {
}
