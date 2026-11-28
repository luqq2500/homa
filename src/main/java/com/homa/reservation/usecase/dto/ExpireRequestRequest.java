package com.homa.reservation.usecase.dto;

/** One Requested reservation past its acceptance deadline expires (REQ-4). */
public record ExpireRequestRequest(String reservationId) {
}
