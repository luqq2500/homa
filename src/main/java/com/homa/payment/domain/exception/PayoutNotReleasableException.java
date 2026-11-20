package com.homa.payment.domain.exception;

import com.homa.payment.domain.PayoutId;
import java.time.Instant;

/**
 * A payout is released once, when the job is marked Completed, the Protection Window has ended and no report is
 * open (PAY-2, RPT-6).
 */
public class PayoutNotReleasableException extends PaymentRuleViolationException {

    private static final long serialVersionUID = 1L;

    public PayoutNotReleasableException(PayoutId payoutId, Instant now) {
        super("Payout " + payoutId.value() + " is not releasable at " + now);
    }
}
