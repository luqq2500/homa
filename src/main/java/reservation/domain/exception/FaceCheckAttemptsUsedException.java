package reservation.domain.exception;

/** A person who failed every face check attempt cannot work on the job. */
public class FaceCheckAttemptsUsedException extends ReservationRuleViolationException {

    public FaceCheckAttemptsUsedException(String memberId, int attempts) {
        super("Team member " + memberId + " used all " + attempts + " face check attempts");
    }
}
