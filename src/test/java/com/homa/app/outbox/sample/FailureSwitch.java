package com.homa.app.outbox.sample;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Test only: makes one consumer's effect fail for one customer until cleared. */
public class FailureSwitch {

    private final Set<String> failing = ConcurrentHashMap.newKeySet();

    public void failFor(String consumer, String customerId) {
        failing.add(consumer + ":" + customerId);
    }

    public void clear() {
        failing.clear();
    }

    public void failIfSet(String consumer, String customerId) {
        if (failing.contains(consumer + ":" + customerId)) {
            throw new IllegalStateException(consumer + " cannot record " + customerId + " (simulated)");
        }
    }
}
