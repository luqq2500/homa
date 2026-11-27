package com.homa.payment.usecase.port.out;

import java.time.Instant;

/** The current time. Use cases never read the clock themselves. */
public interface TimeSource {

    Instant now();
}
