package com.homa.customer.usecase.port.in;

import com.homa.ie.events.PersonVerifiedIE;

/** Records on the customer account what Verification found when the customer verified (CUS-3, CUS-10). */
public interface PersonVerifiedIEHandler {

    void handle(PersonVerifiedIE event);
}
