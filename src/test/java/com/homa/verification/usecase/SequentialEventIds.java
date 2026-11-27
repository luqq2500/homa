package com.homa.verification.usecase;

import com.homa.verification.usecase.port.out.EventIds;

/** event-1, event-2, ... */
public final class SequentialEventIds implements EventIds {

    private int last;

    @Override
    public String next() {
        last++;
        return "event-" + last;
    }
}
