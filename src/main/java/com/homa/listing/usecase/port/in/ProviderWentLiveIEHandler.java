package com.homa.listing.usecase.port.in;

import com.homa.ie.events.ProviderWentLiveIE;

/** Shows a provider that went live in search. */
public interface ProviderWentLiveIEHandler {

    void handle(ProviderWentLiveIE event);
}
