package com.homa.reservation.usecase.dto;

import com.homa.kernel.Money;
import java.time.Instant;

/**
 * The offer as the customer is shown it (ROS-8).
 *
 * @param helpers        how many helpers the smaller roster has
 * @param newTotal       what the reservation would cost if they accept, with the booking discount kept
 * @param customerRefund what would go back to them
 * @param answerBy       the start of the Check-in Period, when the offer lapses
 */
public record ProposeSmallerRosterResponse(String reservationId, int helpers, Money newTotal, Money customerRefund,
        Instant proposedAt, Instant answerBy) {
}
