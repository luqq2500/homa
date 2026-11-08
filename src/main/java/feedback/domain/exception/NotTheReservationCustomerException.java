package feedback.domain.exception;

/** Only the reservation's customer can rate it. */
public class NotTheReservationCustomerException extends FeedbackRuleViolationException {

    public NotTheReservationCustomerException(String customerId) {
        super("Customer " + customerId + " is not this reservation's customer");
    }
}
