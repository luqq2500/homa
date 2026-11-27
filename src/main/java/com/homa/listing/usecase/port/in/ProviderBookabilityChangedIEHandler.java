package com.homa.listing.usecase.port.in;

import com.homa.ie.events.ProviderBookabilityChangedIE;

/** Hides a provider that stopped being bookable from search, or shows it again (PRV-2). */
public interface ProviderBookabilityChangedIEHandler {

    void handle(ProviderBookabilityChangedIE event);
}
