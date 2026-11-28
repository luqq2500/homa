package com.homa.rating.usecase;

import com.homa.rating.usecase.port.out.TimeSource;
import java.time.Instant;

/** Test only: a clock that shows whatever time the test sets. */
public class FixedTimeSource implements TimeSource {

    private Instant now;

    public FixedTimeSource(Instant now) {
        this.now = now;
    }

    public void set(Instant now) {
        this.now = now;
    }

    @Override
    public Instant now() {
        return now;
    }
}
