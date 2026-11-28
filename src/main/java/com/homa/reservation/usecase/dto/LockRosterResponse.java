package com.homa.reservation.usecase.dto;

import java.time.Instant;

/**
 * What the Check-in Period did to one reservation.
 *
 * @param cancelled true when there was no complete roster and the reservation was cancelled under the No Roster
 *                  Policy (NRS-1); false when the roster locked and the reservation goes on (ROS-1)
 * @param at        when the roster locked or the reservation was cancelled
 */
public record LockRosterResponse(String reservationId, boolean cancelled, Instant at) {
}
