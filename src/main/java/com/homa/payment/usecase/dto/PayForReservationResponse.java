package com.homa.payment.usecase.dto;

import com.homa.kernel.Money;

/**
 * The gateway session the customer completes the payment in.
 *
 * @param paymentReference this attempt's reference at the gateway
 */
public record PayForReservationResponse(String checkoutId, String paymentReference, String sessionId,
        String redirectUrl, Money amount) {
}
