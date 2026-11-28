package com.homa.payment.usecase;

import com.homa.payment.domain.CheckoutId;
import com.homa.payment.domain.PaymentAttemptId;
import com.homa.payment.domain.PayoutId;
import com.homa.payment.domain.ReservationId;

/**
 * The idempotency key of every money call (ADR 0002). Each key names the one money line it pays, so the same line
 * always gets the same key: a retried webhook, a redelivered event or a rerun scheduler never charges, refunds or
 * pays twice. A payment attempt, a payment refunded in full (RES-11, RES-12) and a payout have their own ids; a
 * Requested reservation that ends is refunded once, so its reservation id names the line; a cancellation's refund
 * and compensation use the settlement's id and the name of the line, as the plan says.
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

    /** The held payment of a reservation the customer withdrew, refunded in full (REQ-2). */
    public static String withdrawalRefund(ReservationId reservationId) {
        return "refund:withdrawn:" + reservationId.value();
    }

    /** The held payment of a reservation the provider declined, refunded in full (REQ-3). */
    public static String declineRefund(ReservationId reservationId) {
        return "refund:declined:" + reservationId.value();
    }

    /** The held payment of a reservation that expired, refunded in full (REQ-4). */
    public static String expiryRefund(ReservationId reservationId) {
        return "refund:expired:" + reservationId.value();
    }

    /** The customer's refund line of a cancellation settlement (PAY-4, PAY-7). */
    public static String settlementRefund(String settlementId) {
        return "settlement:" + settlementId + ":customerRefund";
    }

    /** The compensation line of a cancellation settlement, paid by Homa (PAY-4). */
    public static String settlementCompensation(String settlementId) {
        return "settlement:" + settlementId + ":compensation";
    }

    /** Sending a released payout to the provider. */
    public static String payout(PayoutId payoutId) {
        return "payout:" + payoutId.value();
    }
}
