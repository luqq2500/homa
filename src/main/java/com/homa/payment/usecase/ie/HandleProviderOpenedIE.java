package com.homa.payment.usecase.ie;

import com.homa.ie.events.ProviderOpenedIE;
import com.homa.payment.domain.ProviderId;
import com.homa.payment.usecase.port.in.ProviderOpenedIEHandler;
import com.homa.payment.usecase.port.out.ProcessedMessageStore;
import com.homa.payment.usecase.port.out.ProviderOwners;

/**
 * A provider opened (PRV-1): Payment records its owner, the one person who may submit its payout account (PRV-2).
 * Publishes nothing.
 */
public class HandleProviderOpenedIE implements ProviderOpenedIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "payment.ProviderOpened";

    private final ProcessedMessageStore processedMessages;
    private final ProviderOwners owners;

    public HandleProviderOpenedIE(ProcessedMessageStore processedMessages, ProviderOwners owners) {
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
