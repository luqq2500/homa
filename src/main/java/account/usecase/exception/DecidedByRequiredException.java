package account.usecase.exception;

/** An admin action needs the id of the founder who does it. */
public class DecidedByRequiredException extends AccountUsecaseException {

    public DecidedByRequiredException() {
        super("An admin action needs the founder who decided it");
    }
}
