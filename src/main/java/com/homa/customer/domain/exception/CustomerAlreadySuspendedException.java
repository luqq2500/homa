package com.homa.customer.domain.exception;

/** CUS-4: Support suspends an active customer; a customer who is already suspended can't be suspended again. */
public class CustomerAlreadySuspendedException extends RuntimeException {

    public CustomerAlreadySuspendedException(String customerId) {
        super("CUS-4: customer " + customerId + " is already suspended");
    }
}
