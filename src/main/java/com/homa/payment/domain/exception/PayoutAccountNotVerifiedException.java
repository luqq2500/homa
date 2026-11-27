package com.homa.payment.domain.exception;

import com.homa.payment.domain.PayoutAccountId;
import com.homa.payment.domain.PayoutAccountStatus;
import com.homa.payment.domain.ProviderId;

/** Payouts are sent only to a verified payout account (PRV-2); they wait while it is pending or not verified (PRV-10). */
public class PayoutAccountNotVerifiedException extends PaymentRuleViolationException {

    private static final long serialVersionUID = 1L;

    public PayoutAccountNotVerifiedException(ProviderId providerId, PayoutAccountId accountId,
            PayoutAccountStatus status) {
        super("Payout account " + accountId.value() + " of provider " + providerId.value() + " is " + status
                + ", so no payout can be sent to it");
    }
}
