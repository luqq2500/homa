package com.homa.reservation.usecase.dto;

import com.homa.kernel.Money;
import java.time.Instant;

/**
 * The reservation is Cancelled, with the fine on the provider and what the customer gets.
 *
 * @param cause        PROVIDER_CANCELLED before the Check-in Period, PROVIDER_LAST_MINUTE from it
 * @param providerFine the fine added to the provider's account (PAY-4, PAY-5)
 * @param compensation the extra Homa pays the customer on top of the full refund; zero more than 24 hours ahead
 */
public record ProviderCancelReservationResponse(String reservationId, String status, String cause,
        Instant cancelledAt, Money providerFine, Money customerRefund, Money compensation) {
}
