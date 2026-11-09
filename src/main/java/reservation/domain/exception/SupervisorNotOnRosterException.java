package reservation.domain.exception;

/** Every supervisor named is a supervisor on the provider's team. */
public class SupervisorNotOnRosterException extends ReservationRuleViolationException {

    public SupervisorNotOnRosterException(String supervisorId) {
        super("Supervisor " + supervisorId + " is not a supervisor on the provider's team");
    }
}
