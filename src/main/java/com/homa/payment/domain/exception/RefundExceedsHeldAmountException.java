package com.homa.payment.domain.exception;

import com.homa.kernel.Money;

/** A refund is paid from the held payment (PAY-4), so it can never be more than is still held. */
public class RefundExceedsHeldAmountException extends PaymentRuleViolationException {

    private static final long serialVersionUID = 1L;

    public RefundExceedsHeldAmountException(Money refund, Money held) {
        super("Cannot refund " + refund + ": only " + held + " is held");
    }
}
