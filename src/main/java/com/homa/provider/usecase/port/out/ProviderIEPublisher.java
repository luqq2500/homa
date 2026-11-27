package com.homa.provider.usecase.port.out;

import com.homa.ie.port.ProviderIE;

/** Provider's single integration event publisher, called inside the use case's transaction (ADR 0002). */
public interface ProviderIEPublisher {

    void publish(ProviderIE event);
}
