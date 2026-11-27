package com.homa.provider.usecase;

import com.homa.provider.usecase.port.out.ProcessedMessageStore;
import java.util.HashSet;
import java.util.Set;

/** The processed_message table as a set of (eventId, handler) keys. */
public final class InMemoryProcessedMessageStore implements ProcessedMessageStore {

    private final Set<String> keys = new HashSet<>();

    @Override
    public boolean recordFirstHandling(String eventId, String handler) {
        return keys.add(eventId + "|" + handler);
    }

    /** True when this handler has recorded this event. */
    public boolean hasHandled(String eventId, String handler) {
        return keys.contains(eventId + "|" + handler);
    }
}
