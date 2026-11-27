package com.homa.customer.usecase;

import com.homa.customer.usecase.port.out.ProcessedMessageStore;
import java.util.HashSet;
import java.util.Set;

/** The (eventId, handler) pairs handled so far, like the processed_message table's primary key. */
final class InMemoryProcessedMessageStore implements ProcessedMessageStore {

    private final Set<String> handled = new HashSet<>();

    @Override
    public boolean recordFirstHandling(String eventId, String handler) {
        return handled.add(handler + "|" + eventId);
    }

    boolean hasHandled(String eventId, String handler) {
        return handled.contains(handler + "|" + eventId);
    }
}
