package reservation.domain.exception;

/** The hours booked sit within the provider's minimum and maximum. */
public class HoursOutsideProviderRangeException extends ReservationRuleViolationException {

    public HoursOutsideProviderRangeException(int hours, int min, int max) {
        super(hours + " hours is outside the provider's range of " + min + " to " + max);
    }
}
