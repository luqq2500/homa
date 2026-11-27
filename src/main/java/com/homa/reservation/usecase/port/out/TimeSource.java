package com.homa.reservation.usecase.port.out;

import java.time.Instant;

/** The current moment, so no use case reads the system clock. */
public interface TimeSource {

    Instant now();
}
