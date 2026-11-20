package com.homa.app.outbox.sample.registration.usecase.port.out;

/** The sample publishing context's repository. */
public interface Customers {

    void add(String customerId, String city);
}
