package com.homa.payment.domain.exception;

import com.homa.payment.domain.PaymentHoldId;

/** Compensation goes back to the payment that counted (PAY-4), so there must be a confirmed payment to pay it to. */
public class NoPaymentToCompensateException extends PaymentRuleViolationException {

    private static final long serialVersionUID = 1L;

    public NoPaymentToCompensateException(PaymentHoldId holdId) {
        super("Payment hold " + holdId.value() + " has no confirmed payment, so there is nothing to pay compensation to");
    }
}
