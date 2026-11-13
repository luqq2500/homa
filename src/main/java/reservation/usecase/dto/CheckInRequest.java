package reservation.usecase.dto;

import kernel.GeoPoint;

public record CheckInRequest(String providerId, String reservationId, String memberId, String selfie, GeoPoint deviceLocation) {}
