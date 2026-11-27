package com.homa.verification.usecase.ie;

import com.homa.ie.events.ProviderOpenedIE;
import com.homa.verification.domain.PersonId;
import com.homa.verification.usecase.port.in.ProviderOpenedIEHandler;
import com.homa.verification.usecase.port.out.ProcessedMessageStore;
import com.homa.verification.usecase.port.out.ProviderOwners;

/**
 * A provider opened (PRV-1): Verification records its owner, so the provider's payout account is checked against the
 * owner's verified identity (PRV-2, IDN-3). Publishes nothing.
 */
public class HandleProviderOpenedIE implements ProviderOpenedIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "verification.ProviderOpened";

    private final ProcessedMessageStore processedMessages;
    private final ProviderOwners providerOwners;

    public HandleProviderOpenedIE(ProcessedMessageStore processedMessages, ProviderOwners providerOwners) {
        this.processedMessages = processedMessages;
        this.providerOwners = providerOwners;
    }

    @Override
    public void handle(ProviderOpenedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        providerOwners.record(event.providerId(), new PersonId(event.ownerPersonId()));
    }
}
