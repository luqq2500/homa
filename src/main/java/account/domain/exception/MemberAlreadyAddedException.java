package account.domain.exception;

/** A verification adds one team member, once. */
public class MemberAlreadyAddedException extends AccountRuleViolationException {

    public MemberAlreadyAddedException(String verificationId) {
        super("A team member was already added from verification " + verificationId);
    }
}
