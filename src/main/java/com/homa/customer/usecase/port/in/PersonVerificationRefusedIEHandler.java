package com.homa.customer.usecase.port.in;

import com.homa.ie.events.PersonVerificationRefusedIE;

/** Records on the customer account why Verification refused the customer's identity (CUS-10). */
public interface PersonVerificationRefusedIEHandler {

    void handle(PersonVerificationRefusedIE event);
}
