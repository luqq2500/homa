package com.homa.verification.usecase;

import com.homa.verification.usecase.port.out.TimeSource;
import java.time.Instant;

/** A clock that shows what the test sets. */
public final class FixedTimeSource implements TimeSource {

    private Instant now;

    public FixedTimeSource(Instant now) {
        this.now = now;
    }

    @Override
    public Instant now() {
        return now;
    }

    public void set(Instant instant) {
        now = instant;
    }
}
