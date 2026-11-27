package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.ProviderWentLiveIE;

/** PRV-1, PRV-2: the provider can be booked. */
public interface ProviderWentLiveIEHandler {

    void handle(ProviderWentLiveIE event);
}
