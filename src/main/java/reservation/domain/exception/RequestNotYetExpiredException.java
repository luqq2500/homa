package reservation.domain.exception;

/** A request expires only when its expiry time is reached. */
public class RequestNotYetExpiredException extends ReservationRuleViolationException {

    public RequestNotYetExpiredException(Object expiresAt) {
        super("The request does not expire until " + expiresAt);
    }
}
