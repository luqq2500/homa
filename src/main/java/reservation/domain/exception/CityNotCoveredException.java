package reservation.domain.exception;

/** The customer's city is one the provider covers. */
public class CityNotCoveredException extends ReservationRuleViolationException {

    public CityNotCoveredException(Object city) {
        super("The provider does not cover " + city);
    }
}
