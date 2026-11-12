package resolution.domain.exception;

/** A case closed without a decision takes no decision and no more evidence. */
public class CaseNotOpenException extends ResolutionRuleViolationException {

    public CaseNotOpenException(String caseId) {
        super("Case " + caseId + " was closed without a decision");
    }
}
