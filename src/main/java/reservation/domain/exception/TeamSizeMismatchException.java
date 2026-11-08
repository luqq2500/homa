package reservation.domain.exception;

/** The team named matches the number of helpers booked. */
public class TeamSizeMismatchException extends ReservationRuleViolationException {

    public TeamSizeMismatchException(int named, int booked) {
        super(named + " helpers named, but " + booked + " were booked");
    }
}
