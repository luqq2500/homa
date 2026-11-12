package policy.usecase.exception;

/** This admin action needs a reason. */
public class AdminReasonRequiredException extends PolicyUsecaseException {

    public AdminReasonRequiredException(String action) {
        super("A reason is required to " + action);
    }
}
