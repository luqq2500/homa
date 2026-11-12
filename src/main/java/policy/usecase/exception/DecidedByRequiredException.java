package policy.usecase.exception;

/** An admin action needs the id of the founder who does it. */
public class DecidedByRequiredException extends PolicyUsecaseException {

    public DecidedByRequiredException() {
        super("An admin action needs the founder who decided it");
    }
}
