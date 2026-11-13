package account.usecase.dto;

public record ConfirmPhoneCodeResponse(String phone, String customerId, String businessId) {}
