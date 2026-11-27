package com.homa.listing.usecase;

import com.homa.listing.usecase.port.out.ProcessedMessageStore;
import java.util.HashSet;
import java.util.Set;

final class InMemoryProcessedMessageStore implements ProcessedMessageStore {

    private final Set<String> processed = new HashSet<>();

    @Override
    public boolean recordFirstHandling(String eventId, String handler) {
        return processed.add(eventId + "|" + handler);
    }

    boolean handled(String eventId, String handler) {
        return processed.contains(eventId + "|" + handler);
    }
}
