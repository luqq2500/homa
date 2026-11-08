package policy.domain.exception;

/** The check-in radius is more than zero metres. */
public class CheckInRadiusNotPositiveException extends PolicyRuleViolationException {

    public CheckInRadiusNotPositiveException(int metres) {
        super("The check-in radius must be more than 0 metres, was " + metres);
    }
}
