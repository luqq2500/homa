package policy.domain.exception;

/** The provider no-show fine is the highest provider fine, so not showing up is never cheaper than cancelling late. */
public class NoShowFineBelowLateCancellationFineException extends PolicyRuleViolationException {

    public NoShowFineBelowLateCancellationFineException(Object noShowFine, Object lateFine) {
        super("The provider no-show fine of " + noShowFine + " is below the late cancellation fine of " + lateFine);
    }
}
