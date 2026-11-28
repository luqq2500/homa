package com.homa.reservation.usecase.dto;

/** One reservation's roster locks at its Check-in Period (ROS-1); the moment comes from the TimeSource. */
public record LockRosterRequest(String reservationId) {
}
