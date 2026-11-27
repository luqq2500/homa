package com.homa.verification.usecase.port.out;

import java.time.Instant;

/** Verification's clock. */
public interface TimeSource {

    Instant now();
}
