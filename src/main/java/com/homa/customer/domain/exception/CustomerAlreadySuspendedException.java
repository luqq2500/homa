package com.homa.customer.domain.exception;

/** CUS-4: Support suspends a customer who isn't suspended; one whose suspension is in force can't be suspended again. */
public class CustomerAlreadySuspendedException extends RuntimeException {

    public CustomerAlreadySuspendedException(String customerId) {
        super("CUS-4: customer " + customerId + " is already suspended");
    }
}
