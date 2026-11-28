package com.homa.conversation.usecase.port.in;

import com.homa.ie.events.CustomerRegisteredIE;

/** A customer registered: they are in good standing, so they can start enquiries (CHT-1). */
public interface CustomerRegisteredIEHandler {

    void handle(CustomerRegisteredIE event);
}
