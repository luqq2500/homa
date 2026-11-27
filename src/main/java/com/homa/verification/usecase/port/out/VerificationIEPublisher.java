package com.homa.verification.usecase.port.out;

import com.homa.ie.port.VerificationIE;

/** Verification's single integration event publisher, called inside the use case's transaction. */
public interface VerificationIEPublisher {

    void publish(VerificationIE event);
}
