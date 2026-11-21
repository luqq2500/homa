package com.homa.listing.domain;

import com.homa.listing.domain.exception.InvalidJobBufferException;
import java.time.Duration;
import java.util.Objects;

/**
 * The provider's buffer between jobs for travel and rest (OFR-10). 30 minutes by default. Reservation adds it to
 * the end of each calendar block (RES-9).
 */
public record JobBuffer(Duration duration) {

    public static final JobBuffer DEFAULT = new JobBuffer(ListingValues.DEFAULT_JOB_BUFFER);

    public JobBuffer {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new InvalidJobBufferException("The buffer between jobs can't be negative, was " + duration);
        }
    }

    public static JobBuffer of(Duration duration) {
        return new JobBuffer(duration);
    }
}
