package reservation.domain.exception;

/** Check-in is enabled only once the team is named. */
public class TeamNotNamedException extends ReservationRuleViolationException {

    public TeamNotNamedException() {
        super("The team has not been named yet");
    }
}
