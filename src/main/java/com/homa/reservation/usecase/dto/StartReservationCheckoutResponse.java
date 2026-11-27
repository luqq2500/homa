package com.homa.reservation.usecase.dto;

import com.homa.kernel.Money;

/** Either the payment opens for the checkout, or the customer is asked to verify their identity first (CUS-3).
 *
 * @param outcome    STARTED or VERIFICATION_REQUIRED
 * @param checkoutId the checkout Payment takes the payment for; null when verification is required
 * @param total      the Total Reservation Fee to pay; null when verification is required */
public record StartReservationCheckoutResponse(String outcome, String checkoutId, Money total) {
}
