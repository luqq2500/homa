package feedback.domain.exception;

/** Only helpers who were named for the reservation can be rated. */
public class HelperNotOnReservationException extends FeedbackRuleViolationException {

    public HelperNotOnReservationException(String helperId) {
        super("Helper " + helperId + " was not named for this reservation");
    }
}
