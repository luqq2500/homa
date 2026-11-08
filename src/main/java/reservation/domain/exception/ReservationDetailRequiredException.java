package reservation.domain.exception;

/** Codes, cities, addresses and ids cannot be left empty. */
public class ReservationDetailRequiredException extends ReservationRuleViolationException {

    public ReservationDetailRequiredException(String what) {
        super(what + " is required");
    }
}
