package policy.domain.exception;

/** Every kind of person needs at least one accepted document, or they could never verify. */
public class NoAcceptedDocumentException extends PolicyRuleViolationException {

    public NoAcceptedDocumentException(Object holder) {
        super("No accepted document for " + holder);
    }
}
