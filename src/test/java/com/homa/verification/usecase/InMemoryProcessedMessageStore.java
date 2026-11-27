package com.homa.verification.usecase;

import com.homa.verification.usecase.port.out.ProcessedMessageStore;
import java.util.HashSet;
import java.util.Set;

/** (eventId, handler) pairs in memory, like the processed_message table's primary key. */
public final class InMemoryProcessedMessageStore implements ProcessedMessageStore {

    private final Set<String> handled = new HashSet<>();

    @Override
    public boolean recordFirstHandling(String eventId, String handler) {
        return handled.add(handler + "|" + eventId);
    }

    public boolean handled(String eventId, String handler) {
        return handled.contains(handler + "|" + eventId);
    }
}
