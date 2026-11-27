package com.homa.payment.usecase.dto;

/**
 * What the webhook did.
 *
 * @param outcome HELD (the first confirmed payment, PaymentHeldIE published), REFUNDED (a later payment for the same
 *                checkout, refunded in full at once, RES-12), ALREADY_RECORDED (this attempt was already confirmed),
 *                FAILED (PaymentFailedIE published) or DUPLICATE (the gateway sent this notification before)
 */
public record ConfirmGatewayPaymentResponse(String outcome) {
}
