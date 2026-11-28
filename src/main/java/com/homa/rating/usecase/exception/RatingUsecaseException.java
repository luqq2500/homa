package com.homa.rating.usecase.exception;

/** An orchestration failure of a Rating use case: something is missing, changed meanwhile, or the actor may not act. */
public abstract class RatingUsecaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected RatingUsecaseException(String message) {
        super(message);
    }
}
