package com.homa.listing.usecase;

import com.homa.listing.usecase.port.out.TimeSource;
import java.time.Instant;

final class FixedTimeSource implements TimeSource {

    private final Instant now;

    FixedTimeSource(Instant now) {
        this.now = now;
    }

    @Override
    public Instant now() {
        return now;
    }
}
