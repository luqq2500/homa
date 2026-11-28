package com.homa.rating.usecase.exception;

/** The acting person may not do this: only a reservation's own parties rate it, and only Support removes a rating. */
public final class NotAllowedException extends RatingUsecaseException {

    private static final long serialVersionUID = 1L;

    public NotAllowedException(String message) {
        super(message);
    }
}
