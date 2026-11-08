package reservation.domain.exception;

/** Every job booked is one the provider offers. */
public class JobNotOfferedException extends ReservationRuleViolationException {

    public JobNotOfferedException(Object job) {
        super("The provider does not offer " + job);
    }
}
