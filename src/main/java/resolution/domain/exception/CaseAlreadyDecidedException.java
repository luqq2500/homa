package resolution.domain.exception;

/** A case is decided once. */
public class CaseAlreadyDecidedException extends ResolutionRuleViolationException {

    public CaseAlreadyDecidedException(String caseId) {
        super("Case " + caseId + " is already decided");
    }
}
