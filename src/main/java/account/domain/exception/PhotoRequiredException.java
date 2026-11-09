package account.domain.exception;

/** A team member is added with a profile photo. */
public class PhotoRequiredException extends AccountRuleViolationException {

    public PhotoRequiredException() {
        super("A profile photo is required");
    }
}
