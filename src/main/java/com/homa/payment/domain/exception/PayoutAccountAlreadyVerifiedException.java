package com.homa.payment.domain.exception;

import com.homa.payment.domain.PayoutAccountId;
import com.homa.payment.domain.ProviderId;

/**
 * Another account can be submitted only while the current one is not verified. A verified account is changed under
 * PRV-10, so payouts wait for the new account and the owner is told of the change.
 */
public class PayoutAccountAlreadyVerifiedException extends PaymentRuleViolationException {

    private static final long serialVersionUID = 1L;

    public PayoutAccountAlreadyVerifiedException(ProviderId providerId, PayoutAccountId accountId) {
        super("Provider " + providerId.value() + " already has verified payout account " + accountId.value()
                + "; changing it is a payout account change (PRV-10)");
    }
}
