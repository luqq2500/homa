package com.homa.listing.usecase.ie;

import com.homa.ie.events.ProviderWentLiveIE;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.domain.ProviderStanding;
import com.homa.listing.usecase.port.in.ProviderWentLiveIEHandler;
import com.homa.listing.usecase.port.out.ListingOwners;
import com.homa.listing.usecase.port.out.ProcessedMessageStore;
import com.homa.listing.usecase.port.out.ProviderStandingRepository;

/**
 * A provider that went live shows in search (PRV-2, RES-1), and the owner Provider names becomes the one person who
 * may change its listing, replacing whoever created it. Publishes nothing.
 */
public final class HandleProviderWentLiveIE implements ProviderWentLiveIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "listing.ProviderWentLive";

    private final ProcessedMessageStore processedMessages;
    private final ProviderStandingRepository standings;
    private final ListingOwners owners;

    public HandleProviderWentLiveIE(ProcessedMessageStore processedMessages, ProviderStandingRepository standings,
                                    ListingOwners owners) {
        this.processedMessages = processedMessages;
        this.standings = standings;
        this.owners = owners;
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
        owners.record(providerId, event.ownerPersonId());
    }
}
