package reservation.domain.exception;

/** Every helper named is a helper on the provider's team. */
public class HelperNotOnRosterException extends ReservationRuleViolationException {

    public HelperNotOnRosterException(String helperId) {
        super("Helper " + helperId + " is not a helper on the provider's team");
    }
}
