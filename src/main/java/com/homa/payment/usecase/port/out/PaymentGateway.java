package com.homa.payment.usecase.port.out;

import com.homa.kernel.Money;
import java.util.Objects;

/**
 * The payment provider, for customers' money: it takes a payment for a checkout and pays refunds back to the payment
 * they came from. It only confirms facts (EXT-1, EXT-2); whether a payment counts and how much is refunded are
 * decided by Payment's domain. Every call carries an idempotency key, so a call repeated after a failure, a retry or
 * a redelivered event never charges or refunds twice. A confirmation or failure comes back later through the
 * gateway's webhook, with the payment reference and checkout id given here.
 */
public interface PaymentGateway {

    /** Opens a payment session the customer completes with the gateway (card, online banking, ...). */
    Session startPayment(SessionRequest request);

    /** Sends a refund now (PAY-7). "Sent" is what the customer is told; arrival depends on the banks (EXT-3). */
    void refund(RefundRequest request);

    /**
     * One payment attempt for a checkout.
     *
     * @param paymentReference Payment's id for the attempt; the gateway hands it back in its webhook
     * @param checkoutId       handed back in the webhook too, so the confirmation finds its checkout
     */
    record SessionRequest(String idempotencyKey, String paymentReference, String checkoutId, Money amount) {

        public SessionRequest {
            Objects.requireNonNull(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(paymentReference, "paymentReference");
            Objects.requireNonNull(checkoutId, "checkoutId");
            Objects.requireNonNull(amount, "amount");
        }
    }

    /** What the customer's app needs to complete the payment with the gateway. */
    record Session(String sessionId, String redirectUrl) {
    }

    /**
     * A refund back to one payment.
     *
     * @param paymentReference the payment attempt the money goes back to
     */
    record RefundRequest(String idempotencyKey, String paymentReference, Money amount) {

        public RefundRequest {
            Objects.requireNonNull(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(paymentReference, "paymentReference");
            Objects.requireNonNull(amount, "amount");
        }
    }
}
