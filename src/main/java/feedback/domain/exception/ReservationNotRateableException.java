package feedback.domain.exception;

/** Only a Completed reservation can be rated. */
public class ReservationNotRateableException extends FeedbackRuleViolationException {

    public ReservationNotRateableException(Object status) {
        super("A " + status + " reservation cannot be rated; only a Completed one can");
    }
}
