package account.domain.exception;

/** A team member with no re-check open is not verified again; a rejection then changes nothing. */
public class TeamMemberAlreadyVerifiedException extends AccountRuleViolationException {

    public TeamMemberAlreadyVerifiedException(String memberId) {
        super("Team member " + memberId + " is already verified");
    }
}
