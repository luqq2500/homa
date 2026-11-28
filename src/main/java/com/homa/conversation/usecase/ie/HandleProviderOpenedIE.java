package com.homa.conversation.usecase.ie;

import com.homa.conversation.domain.ProviderId;
import com.homa.conversation.usecase.port.in.ProviderOpenedIEHandler;
import com.homa.conversation.usecase.port.out.ProcessedMessageStore;
import com.homa.conversation.usecase.port.out.ProviderOwners;
import com.homa.ie.events.ProviderOpenedIE;

/**
 * A provider opened (PRV-1): its owner becomes the person who speaks for it in its chats, and a customer can ask the
 * provider a question. Until Provider's roster reaches Conversation (PRV-11, slice 4), the owner is the only one.
 * Publishes nothing.
 */
public final class HandleProviderOpenedIE implements ProviderOpenedIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "conversation.ProviderOpened";

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
