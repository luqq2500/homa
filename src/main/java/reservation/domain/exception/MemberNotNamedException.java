package reservation.domain.exception;

/** Only a person named for the reservation takes a face check or works on it. */
public class MemberNotNamedException extends ReservationRuleViolationException {

    public MemberNotNamedException(String memberId) {
        super("Team member " + memberId + " is not named for this reservation");
    }
}
