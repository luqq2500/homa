package com.homa.customer.usecase;

import com.homa.customer.usecase.port.out.TimeSource;
import java.time.Instant;

/** A clock that stays where a test sets it. */
final class FixedTimeSource implements TimeSource {

    private Instant now;

    FixedTimeSource(Instant now) {
        this.now = now;
    }

    @Override
    public Instant now() {
        return now;
    }

    void setNow(Instant now) {
        this.now = now;
    }
}
