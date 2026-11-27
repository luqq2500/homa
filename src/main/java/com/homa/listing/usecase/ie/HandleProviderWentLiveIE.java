package com.homa.listing.usecase.ie;

import com.homa.ie.events.ProviderWentLiveIE;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.ProviderStanding;
import com.homa.listing.usecase.port.in.ProviderWentLiveIEHandler;
import com.homa.listing.usecase.port.out.ProcessedMessageStore;
import com.homa.listing.usecase.port.out.ProviderStandingRepository;

/**
 * A provider that went live shows in search (PRV-2, RES-1). Its owner was recorded when it opened
 * ({@code ProviderOpenedIE}). Publishes nothing.
 */
public final class HandleProviderWentLiveIE implements ProviderWentLiveIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "listing.ProviderWentLive";

    private final ProcessedMessageStore processedMessages;
    private final ProviderStandingRepository standings;

    public HandleProviderWentLiveIE(ProcessedMessageStore processedMessages, ProviderStandingRepository standings) {
        this.processedMessages = processedMessages;
        this.standings = standings;
    }

    @Override
    public void handle(ProviderWentLiveIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ProviderId providerId = new ProviderId(event.providerId());
        ProviderStanding standing = standings.findById(providerId).orElseGet(() -> ProviderStanding.unknown(providerId));
        standing.wentLive();
        standings.save(standing);
    }
}
