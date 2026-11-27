package com.homa.customer.usecase.exception;

/** No customer account has this id. */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String customerId) {
        super("No customer account " + customerId);
    }
}
