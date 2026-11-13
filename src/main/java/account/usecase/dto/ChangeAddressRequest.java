package account.usecase.dto;

public record ChangeAddressRequest(String customerId, String addressLine, double latitude, double longitude) {}
