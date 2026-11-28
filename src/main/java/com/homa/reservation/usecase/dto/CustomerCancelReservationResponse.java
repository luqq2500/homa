package com.homa.reservation.usecase.dto;

import com.homa.kernel.Money;
import java.time.Instant;

/**
 * The reservation is Cancelled, with what the customer paid and what comes back to them.
 *
 * @param cause       CUSTOMER_CANCELLED before the Check-in Period, CUSTOMER_LAST_MINUTE from it
 * @param customerFee zero when the cancellation was free
 */
public record CustomerCancelReservationResponse(String reservationId, String status, String cause,
        Instant cancelledAt, Money customerFee, Money customerRefund) {
}
