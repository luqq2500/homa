package com.homa.payment.domain.exception;

import com.homa.payment.domain.PayoutId;

/**
 * Only a job payout whose job is not yet marked Completed can be marked completed (JOB-3) or turned into a fee payout
 * when the reservation ends with a fee (PAY-6): a fee payout has no job to complete, and a job is marked Completed
 * once.
 */
public class PayoutNotAwaitingCompletionException extends PaymentRuleViolationException {

    private static final long serialVersionUID = 1L;

    public PayoutNotAwaitingCompletionException(PayoutId payoutId) {
        super("Payout " + payoutId.value() + " is not a job payout waiting for its job to be marked Completed");
    }
}
