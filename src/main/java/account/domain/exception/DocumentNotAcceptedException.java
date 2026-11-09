package account.domain.exception;

/** A person verifies only with a document on the accepted documents list; an independent provider's team members are always Malaysian. */
public class DocumentNotAcceptedException extends AccountRuleViolationException {

    public DocumentNotAcceptedException(Object holder, Object type, Object country) {
        super("A " + type + " issued by " + country + " is not accepted for " + holder);
    }
}
