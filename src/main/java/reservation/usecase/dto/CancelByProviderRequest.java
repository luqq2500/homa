package reservation.usecase.dto;

public record CancelByProviderRequest(String providerId, String reservationId) {}
