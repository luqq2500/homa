package reservation.domain.exception;

/** A booking that reaches the provider's supervisor rule is named with at least one supervisor. */
public class SupervisorRequiredException extends ReservationRuleViolationException {

    public SupervisorRequiredException() {
        super("This booking needs at least one supervisor");
    }
}
