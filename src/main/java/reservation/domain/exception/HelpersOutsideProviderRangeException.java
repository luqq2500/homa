package reservation.domain.exception;

/** The helpers booked sit within the provider's minimum and maximum. */
public class HelpersOutsideProviderRangeException extends ReservationRuleViolationException {

    public HelpersOutsideProviderRangeException(int helpers, int min, int max) {
        super(helpers + " helpers is outside the provider's range of " + min + " to " + max);
    }
}
