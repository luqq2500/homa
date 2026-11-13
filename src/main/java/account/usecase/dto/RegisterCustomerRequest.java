package account.usecase.dto;

public record RegisterCustomerRequest(String verificationId, String name, String addressLine, double latitude, double longitude, String language) {}
