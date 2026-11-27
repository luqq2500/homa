package com.homa.listing.usecase;

import com.homa.listing.usecase.port.out.EventIds;

/** listing-event-1, listing-event-2, ... */
final class SequentialEventIds implements EventIds {

    private int last;

    @Override
    public String next() {
        last++;
        return "listing-event-" + last;
    }
}
