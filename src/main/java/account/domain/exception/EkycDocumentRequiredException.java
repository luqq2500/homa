package account.domain.exception;

/** A verified eKYC result carries the identity document it read. */
public class EkycDocumentRequiredException extends AccountRuleViolationException {

    public EkycDocumentRequiredException() {
        super("A verified eKYC result needs its identity document");
    }
}
