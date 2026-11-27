package com.homa.listing.usecase.port.in;

import com.homa.ie.events.ProviderOpenedIE;

/** Records a newly opened provider's owner, the one person who may change its listing (PRV-1). */
public interface ProviderOpenedIEHandler {

    void handle(ProviderOpenedIE event);
}
