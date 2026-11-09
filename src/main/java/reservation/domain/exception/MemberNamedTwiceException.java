package reservation.domain.exception;

/** The same person cannot be named twice, as a helper and as a supervisor. */
public class MemberNamedTwiceException extends ReservationRuleViolationException {

    public MemberNamedTwiceException(String memberId) {
        super("Team member " + memberId + " is named twice");
    }
}
