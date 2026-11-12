package account.usecase.dto;

/** The code the person entered for a verification. */
public record ConfirmPhoneRequest(String verificationId, String code) {
}
