package com.homa.rating.domain;

import com.homa.kernel.TimeInterval;
import com.homa.rating.domain.exception.RatingWindowClosedException;
import java.time.Instant;
import java.util.Objects;

/**
 * The Rating Window of one reservation: it opens when the job is marked Completed and closes 7 days after the
 * Protection Window ends (RTG-2). The window is fixed when it opens, with the value version in force when the
 * reservation was Requested (P3). It is half open: a rating at the closing moment is too late.
 */
public record RatingWindow(TimeInterval period) {

    public RatingWindow {
        Objects.requireNonNull(period, "period");
    }

    /**
     * The window of a job marked Completed at {@code completedAt}. A job marked Completed only after the window
     * would have closed gets no ratings (RTG-2).
     */
    static RatingWindow opening(Instant completedAt, Instant protectionWindowEnd, RatingValues.Version values) {
        Objects.requireNonNull(completedAt, "completedAt");
        Objects.requireNonNull(protectionWindowEnd, "protectionWindowEnd");
        Instant closesAt = values.closesAfter(protectionWindowEnd);
        if (!closesAt.isAfter(completedAt)) {
            throw new RatingWindowClosedException(completedAt, closesAt);
        }
        return new RatingWindow(new TimeInterval(completedAt, closesAt));
    }

    /** Rebuilds a stored window. */
    public static RatingWindow of(Instant opensAt, Instant closesAt) {
        return new RatingWindow(new TimeInterval(opensAt, closesAt));
    }

    public Instant opensAt() {
        return period.start();
    }

    public Instant closesAt() {
        return period.end();
    }

    /** True from the closing moment on. */
    public boolean hasClosedBy(Instant now) {
        Objects.requireNonNull(now, "now");
        return !now.isBefore(period.end());
    }
}
