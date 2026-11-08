package reservation.domain.exception;

/** Every helper named is on the provider's verified roster. */
public class HelperNotOnRosterException extends ReservationRuleViolationException {

    public HelperNotOnRosterException(String helperId) {
        super("Helper " + helperId + " is not on the provider's verified roster");
    }
}
