package com.homa.app.outbox.sample.registration.usecase;

import com.homa.app.outbox.sample.registration.usecase.port.out.Customers;
import com.homa.app.outbox.sample.registration.usecase.port.out.RegistrationIEPublisher;
import com.homa.ie.events.CustomerRegisteredIE;
import java.time.Instant;

/**
 * The sample publishing usecase: saves the aggregate, then publishes its event. Spring-free, like a real usecase;
 * the caller (a transaction decorator in app, here the test) supplies the transaction both writes share.
 */
public class RegisterCustomer {

    private final Customers customers;
    private final RegistrationIEPublisher publisher;

    public RegisterCustomer(Customers customers, RegistrationIEPublisher publisher) {
        this.customers = customers;
        this.publisher = publisher;
    }

    public void register(String eventId, String customerId, String city, Instant now) {
        customers.add(customerId, city);
        publisher.publish(new CustomerRegisteredIE(eventId, customerId, city, "ACTIVE", now));
    }
}
