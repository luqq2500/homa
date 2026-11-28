package com.homa.rating.usecase;

import com.homa.rating.usecase.port.out.EventIds;

/** Test only: event-1, event-2, ... so tests can assert event ids. */
public class SequentialEventIds implements EventIds {

    private int issued;

    @Override
    public String next() {
        issued++;
        return "event-" + issued;
    }
}
