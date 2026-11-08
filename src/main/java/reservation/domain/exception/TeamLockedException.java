package reservation.domain.exception;

/** The provider can change the team only until the booked start. */
public class TeamLockedException extends ReservationRuleViolationException {

    public TeamLockedException(Object start) {
        super("The team cannot change from the booked start at " + start);
    }
}
