package com.homa.app.outbox.sample.registration.infra.event;

import com.homa.app.outbox.sample.registration.usecase.port.out.RegistrationIEPublisher;
import com.homa.ie.port.CustomerIE;
import org.springframework.context.ApplicationEventPublisher;

/**
 * The pattern for every context's IE publisher: hand the event to Spring, nothing else. The outbox writer in
 * com.homa.app records it in the caller's transaction, so this class never sees the outbox.
 */
public class SpringRegistrationIEPublisher implements RegistrationIEPublisher {

    private final ApplicationEventPublisher events;

    public SpringRegistrationIEPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override
    public void publish(CustomerIE event) {
        events.publishEvent(event);
    }
}
