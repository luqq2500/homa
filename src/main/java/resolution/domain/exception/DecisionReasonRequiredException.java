package resolution.domain.exception;

/** Each decision is recorded with a short reason. */
public class DecisionReasonRequiredException extends ResolutionRuleViolationException {

    public DecisionReasonRequiredException() {
        super("A decision needs a reason");
    }
}
