package reservation.domain.exception;

/** A booking's hours must be inside Homa's duration range as well as the provider's. */
public class HoursOutsideHomaRangeException extends ReservationRuleViolationException {

    public HoursOutsideHomaRangeException(int hours, int min, int max) {
        super("The booking is for " + hours + " hours, outside Homa's range of " + min + " to " + max);
    }
}
