package com.homa.provider.usecase;

import com.homa.provider.usecase.port.out.TimeSource;
import java.time.Instant;

/** A clock that reads what the test set. */
public final class FixedTimeSource implements TimeSource {

    private Instant now;

    public FixedTimeSource(Instant now) {
        this.now = now;
    }

    @Override
    public Instant now() {
        return now;
    }

    public void setNow(Instant now) {
        this.now = now;
    }
}
