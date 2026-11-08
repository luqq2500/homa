package resolution.domain.exception;

/** Ids cannot be left empty. */
public class ResolutionDetailRequiredException extends ResolutionRuleViolationException {

    public ResolutionDetailRequiredException(String what) {
        super(what + " is required");
    }
}
