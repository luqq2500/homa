package com.homa.customer.domain.exception;

/**
 * CUS-10: one verified identity holds one customer account, so an account verified as one person can't be verified
 * as another. Otherwise the first person's identity would come free for a second account.
 */
public class AccountHoldsAnotherIdentityException extends RuntimeException {

    public AccountHoldsAnotherIdentityException(String customerId) {
        super("CUS-10: customer " + customerId + " is already verified as another person");
    }
}
