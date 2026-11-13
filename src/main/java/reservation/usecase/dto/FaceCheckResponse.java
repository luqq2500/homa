package reservation.usecase.dto;

public record FaceCheckResponse(String reservationId, String faceCheck, boolean canWork) {}
