package com.homa.customer.usecase.port.out;

import java.time.Instant;

/** The Customer context's clock. */
public interface TimeSource {

    Instant now();
}
