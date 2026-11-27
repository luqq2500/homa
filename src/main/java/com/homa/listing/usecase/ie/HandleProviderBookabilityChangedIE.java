package com.homa.listing.usecase.ie;

import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.ProviderStanding;
import com.homa.listing.usecase.port.in.ProviderBookabilityChangedIEHandler;
import com.homa.listing.usecase.port.out.ProcessedMessageStore;
import com.homa.listing.usecase.port.out.ProviderStandingRepository;

/** A provider that stopped being bookable leaves search, and returns when it is bookable again (PRV-2). */
public final class HandleProviderBookabilityChangedIE implements ProviderBookabilityChangedIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "listing.ProviderBookabilityChanged";

    private final ProcessedMessageStore processedMessages;
    private final ProviderStandingRepository standings;

    public HandleProviderBookabilityChangedIE(ProcessedMessageStore processedMessages,
                                              ProviderStandingRepository standings) {
        this.processedMessages = processedMessages;
        this.standings = standings;
    }

    @Override
    public void handle(ProviderBookabilityChangedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ProviderId providerId = new ProviderId(event.providerId());
        ProviderStanding standing = standings.findById(providerId).orElseGet(() -> ProviderStanding.unknown(providerId));
        standing.changeBookability(event.bookable());
        standings.save(standing);
    }
}
