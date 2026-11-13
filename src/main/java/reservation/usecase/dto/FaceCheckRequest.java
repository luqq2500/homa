package reservation.usecase.dto;

public record FaceCheckRequest(String providerId, String reservationId, String memberId, String selfie) {}
