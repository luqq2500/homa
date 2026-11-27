package com.homa.payment.domain;

/**
 * In whose name a payout account is held. Either way the account must be in the provider's name (PRV-2); one in the
 * business's name also counts toward Business status (PRV-7).
 */
public enum PayoutAccountHolder {
    /** The account is in the owner's own name, checked against the owner's verified identity (IDN-3). */
    OWNER,
    /** The account is in the registered business's name, checked against the business registration (PRV-7). */
    BUSINESS
}
