package reservation.domain.exception;

/** An enquiry is for a time still to come. */
public class EnquiryDateInPastException extends ReservationRuleViolationException {

    public EnquiryDateInPastException(Object start) {
        super("An enquiry must be for a time still to come, was " + start);
    }
}
