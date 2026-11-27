package com.homa.provider.usecase.port.out;

import java.time.Instant;

/** The current time, as Provider's clock decides it. */
public interface TimeSource {

    Instant now();
}
