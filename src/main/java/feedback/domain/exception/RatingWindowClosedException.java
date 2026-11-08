package feedback.domain.exception;

/** A reservation can be rated only within the rating window after it became Completed. */
public class RatingWindowClosedException extends FeedbackRuleViolationException {

    public RatingWindowClosedException(Object closedAt) {
        super("The rating window closed at " + closedAt);
    }
}
