package com.homa.reservation.usecase.dto;

/**
 * The customer answers the smaller roster the provider offered (ROS-8).
 *
 * @param accept true to accept it, false to decline it
 */
public record RespondToSmallerRosterRequest(String reservationId, String customerId, boolean accept) {
}
