package reservation.domain.exception;

/** A request the provider did not answer in time cannot be answered or edited. */
public class RequestExpiredException extends ReservationRuleViolationException {

    public RequestExpiredException(Object expiredAt) {
        super("The request expired at " + expiredAt);
    }
}
