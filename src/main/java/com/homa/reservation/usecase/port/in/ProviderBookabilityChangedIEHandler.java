package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.ProviderBookabilityChangedIE;

/** PRV-2: the provider can or can no longer be booked. */
public interface ProviderBookabilityChangedIEHandler {

    void handle(ProviderBookabilityChangedIE event);
}
