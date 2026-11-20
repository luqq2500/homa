package com.homa.payment.domain.exception;

import com.homa.payment.domain.PaymentHoldId;
import com.homa.payment.domain.PaymentHoldStatus;

/** A payment is held only once, while its checkout is still waiting for it (PAY-1). */
public class PaymentNotPendingException extends PaymentRuleViolationException {

    private static final long serialVersionUID = 1L;

    public PaymentNotPendingException(PaymentHoldId holdId, PaymentHoldStatus status) {
        super("Payment hold " + holdId.value() + " is " + status + ", no longer waiting for payment");
    }
}
