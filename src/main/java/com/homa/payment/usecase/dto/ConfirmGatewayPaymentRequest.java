package com.homa.payment.usecase.dto;

import java.time.Instant;

/**
 * The gateway's webhook about one payment attempt, as infra translated it (EXT-1, EXT-2).
 *
 * @param gatewayEventId   the gateway's own id for this notification; a redelivered webhook repeats it
 * @param paymentReference the attempt's reference, handed to the gateway when the payment started
 * @param checkoutId       the checkout the payment was started for, handed back by the gateway
 * @param confirmed        true when the gateway confirmed the payment, false when it refused it
 * @param occurredAt       when the payment was confirmed (paidAt) or refused
 * @param failureReason    the gateway's reason when refused; null when confirmed
 */
public record ConfirmGatewayPaymentRequest(String gatewayEventId, String paymentReference, String checkoutId,
        boolean confirmed, Instant occurredAt, String failureReason) {
}
