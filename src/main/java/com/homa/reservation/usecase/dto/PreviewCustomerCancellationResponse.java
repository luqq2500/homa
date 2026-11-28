package com.homa.reservation.usecase.dto;

import com.homa.kernel.Money;
import java.time.Instant;

/**
 * What cancelling would cost the customer if they confirmed at {@code asOf}. Nothing was cancelled. The fee can be
 * higher a moment later, for example after a tier's cut-off.
 *
 * @param cause       CUSTOMER_CANCELLED before the Check-in Period, CUSTOMER_LAST_MINUTE from it
 * @param customerFee zero when the cancellation is free (CCX-1, CCX-3 to CCX-5)
 */
public record PreviewCustomerCancellationResponse(String reservationId, String cause, Instant asOf,
        Money customerFee, Money customerRefund) {
}
