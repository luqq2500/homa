package com.homa.listing.usecase.ie;

import com.homa.ie.events.ProviderOpenedIE;
import com.homa.listing.domain.ProviderId;
import com.homa.listing.usecase.port.in.ProviderOpenedIEHandler;
import com.homa.listing.usecase.port.out.ListingOwners;
import com.homa.listing.usecase.port.out.ProcessedMessageStore;

/**
 * A provider opened (PRV-1): its owner becomes the one person who may create and change its listing. Publishes
 * nothing.
 */
public final class HandleProviderOpenedIE implements ProviderOpenedIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "listing.ProviderOpened";

    private final ProcessedMessageStore processedMessages;
    private final ListingOwners owners;

    public HandleProviderOpenedIE(ProcessedMessageStore processedMessages, ListingOwners owners) {
        this.processedMessages = processedMessages;
        this.owners = owners;
    }

    @Override
    public void handle(ProviderOpenedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        owners.record(new ProviderId(event.providerId()), event.ownerPersonId());
    }
}
