package com.homa.provider.usecase;

import com.homa.provider.usecase.port.out.EventIds;

/** Event ids event-1, event-2, ... in the order asked for. */
public final class SequentialEventIds implements EventIds {

    private int issued;

    @Override
    public String next() {
        issued++;
        return "event-" + issued;
    }
}
