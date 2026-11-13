package account.usecase.dto;

public record ConfirmPhoneCodeRequest(String verificationId, String code) {}
