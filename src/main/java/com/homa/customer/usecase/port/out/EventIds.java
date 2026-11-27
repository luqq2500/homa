package com.homa.customer.usecase.port.out;

/** Issues the id of each integration event the Customer context publishes. */
public interface EventIds {

    /** A new, unique event id. */
    String next();
}
