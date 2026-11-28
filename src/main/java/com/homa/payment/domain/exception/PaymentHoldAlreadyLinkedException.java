package com.homa.payment.domain.exception;

import com.homa.payment.domain.PaymentHoldId;
import com.homa.payment.domain.ReservationId;

/** A payment hold belongs to one reservation (RES-7): it cannot be linked to another one. */
public class PaymentHoldAlreadyLinkedException extends PaymentRuleViolationException {

    private static final long serialVersionUID = 1L;

    public PaymentHoldAlreadyLinkedException(PaymentHoldId holdId, ReservationId linkedTo) {
        super("Payment hold " + holdId.value() + " already belongs to reservation " + linkedTo.value());
    }
}
