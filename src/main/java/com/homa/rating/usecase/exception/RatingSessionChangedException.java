package com.homa.rating.usecase.exception;

/**
 * A rating session was saved by someone else after it was loaded (the optimistic lock), so nothing was saved. The
 * caller asks again and sees the other change, such as the other side's rating or the reveal.
 */
public final class RatingSessionChangedException extends RatingUsecaseException {

    private static final long serialVersionUID = 1L;

    public RatingSessionChangedException(String reservationId) {
        super("The ratings of reservation " + reservationId + " changed since they were loaded");
    }
}
