package com.homa.payment.domain.exception;

import com.homa.payment.domain.PaymentHoldId;
import com.homa.payment.domain.PaymentHoldStatus;

/**
 * A customer can start a payment attempt only while no payment for the checkout is confirmed. Once one is held, any
 * other payment would be refunded in full at once (RES-12); once everything is refunded, the checkout is over.
 */
public class CheckoutNotAwaitingPaymentException extends PaymentRuleViolationException {

    private static final long serialVersionUID = 1L;

    public CheckoutNotAwaitingPaymentException(PaymentHoldId holdId, PaymentHoldStatus status) {
        super("Payment hold " + holdId.value() + " is " + status + ", so it takes no further payment");
    }
}
