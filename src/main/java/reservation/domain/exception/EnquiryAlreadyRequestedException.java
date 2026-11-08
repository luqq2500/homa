package reservation.domain.exception;

/** An enquiry turns into one reservation only. */
public class EnquiryAlreadyRequestedException extends ReservationRuleViolationException {

    public EnquiryAlreadyRequestedException(String reservationId) {
        super("The enquiry was already requested as reservation " + reservationId);
    }
}
