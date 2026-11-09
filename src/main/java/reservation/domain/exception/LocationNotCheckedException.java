package reservation.domain.exception;

/** Face checks come after the location check. */
public class LocationNotCheckedException extends ReservationRuleViolationException {

    public LocationNotCheckedException() {
        super("The location check has not passed yet");
    }
}
