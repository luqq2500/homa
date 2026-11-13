package reservation.usecase.dto;

public record CancelByCustomerRequest(String customerId, String reservationId) {}
