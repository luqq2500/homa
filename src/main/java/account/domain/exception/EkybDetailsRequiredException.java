package account.domain.exception;

/** A verified eKYB result carries the business registration and the owner's or director's document. */
public class EkybDetailsRequiredException extends AccountRuleViolationException {

    public EkybDetailsRequiredException() {
        super("A verified eKYB result needs the business registration and the owner's or director's document");
    }
}
