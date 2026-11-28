package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.ProviderOpenedIE;

/** PRV-1, PRV-11: a verified person opened a provider as its owner, who acts on all its reservations. */
public interface ProviderOpenedIEHandler {

    void handle(ProviderOpenedIE event);
}
