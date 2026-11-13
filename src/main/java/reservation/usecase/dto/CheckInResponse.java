package reservation.usecase.dto;

public record CheckInResponse(String reservationId, boolean checkedIn, String faceCheck) {}
