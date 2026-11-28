package com.homa.reservation.usecase.dto;

import com.homa.kernel.Money;
import java.time.Instant;

/**
 * What the customer's answer did (ROS-8).
 *
 * @param accepted       true when they accepted the smaller roster, false when they declined it
 * @param helpers        the helpers the reservation has now
 * @param total          what the reservation costs now: the new price once accepted, the booked one after a decline
 * @param customerRefund what goes back to them at once: the difference once accepted, zero after a decline
 * @param answeredAt     when they answered
 */
public record RespondToSmallerRosterResponse(String reservationId, boolean accepted, int helpers, Money total,
        Money customerRefund, Instant answeredAt) {
}
