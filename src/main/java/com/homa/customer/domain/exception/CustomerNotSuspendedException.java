package com.homa.customer.domain.exception;

/** CUS-6: Support lifts only a suspension that is in force. */
public class CustomerNotSuspendedException extends RuntimeException {

    public CustomerNotSuspendedException(String customerId) {
        super("CUS-6: customer " + customerId + " is not suspended, so there is no suspension to lift");
    }
}
