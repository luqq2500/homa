package com.homa.payment.domain.exception;

import com.homa.payment.domain.PaymentHoldId;
import com.homa.payment.domain.PaymentHoldStatus;

/** Refunds come from the held payment (PAY-4), so nothing can be refunded while nothing is held. */
public class PaymentNotHeldException extends PaymentRuleViolationException {

    private static final long serialVersionUID = 1L;

    public PaymentNotHeldException(PaymentHoldId holdId, PaymentHoldStatus status) {
        super("Payment hold " + holdId.value() + " is " + status + ", so it holds nothing to refund");
    }
}
