package com.homa.app.outbox.sample.registration.usecase.port.out;

import com.homa.ie.port.CustomerIE;

/** The sample publishing context's single IE publisher, as a context declares it in usecase/port/out. */
public interface RegistrationIEPublisher {

    void publish(CustomerIE event);
}
