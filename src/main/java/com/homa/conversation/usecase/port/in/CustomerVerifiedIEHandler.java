package com.homa.conversation.usecase.port.in;

import com.homa.ie.events.CustomerVerifiedIE;

/** A customer's identity is verified: the fact is kept with their standing (CUS-3). */
public interface CustomerVerifiedIEHandler {

    void handle(CustomerVerifiedIE event);
}
