package com.homa.payment.domain.exception;

import com.homa.payment.domain.PayoutId;
import com.homa.payment.domain.PayoutStatus;

/**
 * Only a payout still waiting to be released can be held by a report (RPT-6), reduced by a refund, changed or
 * voided (NSH-6): once released the money has left Homa, and a void payout will never be released.
 */
public class PayoutNotPendingException extends PaymentRuleViolationException {

    private static final long serialVersionUID = 1L;

    public PayoutNotPendingException(PayoutId payoutId, PayoutStatus status) {
        super("Payout " + payoutId.value() + " is " + status + ", no longer waiting to be released");
    }
}
