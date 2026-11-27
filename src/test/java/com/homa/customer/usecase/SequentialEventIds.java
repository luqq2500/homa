package com.homa.customer.usecase;

import com.homa.customer.usecase.port.out.EventIds;

/** Issues event-1, event-2, ... so tests can assert each event's id. */
final class SequentialEventIds implements EventIds {

    private int issued;

    @Override
    public String next() {
        issued++;
        return "event-" + issued;
    }
}
