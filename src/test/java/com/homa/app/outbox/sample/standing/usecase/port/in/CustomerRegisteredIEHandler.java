package com.homa.app.outbox.sample.standing.usecase.port.in;

import com.homa.ie.events.CustomerRegisteredIE;

public interface CustomerRegisteredIEHandler {

    void handle(CustomerRegisteredIE event);
}
