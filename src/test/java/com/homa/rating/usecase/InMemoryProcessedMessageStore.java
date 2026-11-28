package com.homa.rating.usecase;

import com.homa.rating.usecase.port.out.ProcessedMessageStore;
import java.util.ArrayList;
import java.util.List;

/** Test only: the (eventId, handler) pairs handled, like the processed_message table's primary key. */
public class InMemoryProcessedMessageStore implements ProcessedMessageStore {

    private final List<String> recorded = new ArrayList<>();

    @Override
    public boolean recordFirstHandling(String eventId, String handler) {
        String key = eventId + "|" + handler;
        if (recorded.contains(key)) {
            return false;
        }
        recorded.add(key);
        return true;
    }

    public List<String> recorded() {
        return List.copyOf(recorded);
    }
}
