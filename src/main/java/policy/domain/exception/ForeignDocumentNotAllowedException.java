package policy.domain.exception;

/** A provider and an independent provider's team members are Malaysian; only a business provider's team members can use a foreign document. */
public class ForeignDocumentNotAllowedException extends PolicyRuleViolationException {

    public ForeignDocumentNotAllowedException(Object holder) {
        super("Only Malaysian documents can be accepted for " + holder);
    }
}
