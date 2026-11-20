package com.homa.app.outbox.sample.conversation.usecase.port.in;

import com.homa.ie.events.CustomerRegisteredIE;

public interface CustomerRegisteredIEHandler {

    void handle(CustomerRegisteredIE event);
}
