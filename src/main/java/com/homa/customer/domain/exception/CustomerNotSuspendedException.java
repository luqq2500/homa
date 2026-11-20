package com.homa.customer.domain.exception;

/** CUS-6: only a suspended customer can be reactivated. */
public class CustomerNotSuspendedException extends RuntimeException {

    public CustomerNotSuspendedException(String customerId) {
        super("CUS-6: customer " + customerId + " is not suspended, so there is nothing to reactivate");
    }
}
