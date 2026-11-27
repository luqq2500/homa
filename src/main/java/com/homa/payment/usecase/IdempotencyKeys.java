package com.homa.payment.usecase;

import com.homa.payment.domain.CheckoutId;
import com.homa.payment.domain.PaymentAttemptId;
import com.homa.payment.domain.PayoutId;

/**
 * The idempotency key of every money call (ADR 0002). Each key names the one money line it pays, so the same line
 * always gets the same key: a retried webhook, a redelivered event or a rerun scheduler never charges, refunds or
 * pays twice. Slice 1 has no settlement lines yet; its lines are a payment attempt, a payment refunded in full
 * (RES-11, RES-12) and a payout. Settlement refunds from slice 2 on use their settlement line id.
 */
public final class IdempotencyKeys {

    private IdempotencyKeys() {
    }

    /** Starting one payment attempt at the gateway. */
    public static String paymentAttempt(PaymentAttemptId attemptId) {
        return "payment-attempt:" + attemptId.value();
    }

    /** A later confirmed payment for a checkout that already has one, refunded in full (RES-12). */
    public static String duplicatePaymentRefund(PaymentAttemptId attemptId) {
        return "refund:duplicate-payment:" + attemptId.value();
    }

    /** The held payment of a checkout that could not become a reservation, refunded in full (RES-11). */
    public static String rejectedCheckoutRefund(CheckoutId checkoutId) {
        return "refund:rejected-checkout:" + checkoutId.value();
    }

    /** Sending a released payout to the provider. */
    public static String payout(PayoutId payoutId) {
        return "payout:" + payoutId.value();
    }
}
