package com.homa.listing.usecase.port.out;

import java.time.Instant;

/** The current time. */
public interface TimeSource {

    Instant now();
}
