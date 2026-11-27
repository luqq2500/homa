package com.homa.provider.usecase.port.in;

import com.homa.ie.events.PersonVerifiedIE;

/** Keeps what Verification decided about an owner or member, for opening a provider and adding members. */
public interface PersonVerifiedIEHandler {

    void handle(PersonVerifiedIE event);
}
