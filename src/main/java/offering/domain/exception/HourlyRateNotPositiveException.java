package offering.domain.exception;

/** The hourly rate per helper is above zero. */
public class HourlyRateNotPositiveException extends OfferingRuleViolationException {

    public HourlyRateNotPositiveException(Object rate) {
        super("The hourly rate per helper must be above zero, was " + rate);
    }
}
