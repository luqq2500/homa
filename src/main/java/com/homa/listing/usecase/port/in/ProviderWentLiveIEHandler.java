package com.homa.listing.usecase.port.in;

import com.homa.ie.events.ProviderWentLiveIE;

/** Shows a provider that went live in search, and records its owner. */
public interface ProviderWentLiveIEHandler {

    void handle(ProviderWentLiveIE event);
}
