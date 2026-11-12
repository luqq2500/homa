package reservation.domain.exception;

/** A request's address cannot change: the customer cancels it for free and requests again. */
public class AddressNotEditableException extends ReservationRuleViolationException {

    public AddressNotEditableException(Object address) {
        super("The address of a request cannot change from " + address);
    }
}
