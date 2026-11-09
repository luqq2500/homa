package account.domain.exception;

/** A person becomes a team member only from a verified eKYC. */
public class MemberNotVerifiedException extends AccountRuleViolationException {

    public MemberNotVerifiedException(String verificationId, Object status) {
        super("Verification " + verificationId + " is not verified, but " + status);
    }
}
