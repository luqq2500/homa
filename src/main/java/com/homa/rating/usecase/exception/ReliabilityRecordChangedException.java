package com.homa.rating.usecase.exception;

/**
 * A reliability record was saved by someone else after it was loaded (the optimistic lock), so nothing was saved.
 * The event being handled is retried and sees the other change.
 */
public final class ReliabilityRecordChangedException extends RatingUsecaseException {

    private static final long serialVersionUID = 1L;

    public ReliabilityRecordChangedException(String partyId) {
        super("The reliability record of " + partyId + " changed since it was loaded");
    }
}
