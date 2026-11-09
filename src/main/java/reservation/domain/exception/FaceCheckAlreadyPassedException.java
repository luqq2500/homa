package reservation.domain.exception;

/** A person who passed their face check does not take another. */
public class FaceCheckAlreadyPassedException extends ReservationRuleViolationException {

    public FaceCheckAlreadyPassedException(String memberId) {
        super("Team member " + memberId + " already passed their face check");
    }
}
