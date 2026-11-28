package com.homa.rating.domain.exception;

import java.time.Instant;

/**
 * RTG-2: the Rating Window closes 7 days after the Protection Window ends. Nothing can be rated after it, and a job
 * marked Completed after it gets no ratings at all.
 */
public class RatingWindowClosedException extends RatingRuleViolationException {

    private static final long serialVersionUID = 1L;

    public RatingWindowClosedException(Instant at, Instant closesAt) {
        super("RTG-2: the Rating Window closed at " + closesAt + ", so nothing can be rated or opened at " + at);
    }
}
