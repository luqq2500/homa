package com.homa.payment.domain.exception;

import com.homa.kernel.Money;

/**
 * Payment only moves positive amounts already rounded to the sen. Settlement lines arrive rounded (PAY-8), so an
 * amount with a fraction of a sen is a figure Payment did not receive from a Settlement.
 */
public class AmountNotPayableException extends PaymentRuleViolationException {

    private static final long serialVersionUID = 1L;

    public AmountNotPayableException(String what, Money amount) {
        super(what + " must be a positive amount in whole sen, was " + amount);
    }
}
