package account.domain.exception;

/** Re-verification is for team members only: an invited helper is still being verified, and a lapsed one is already off the team. */
public class HelperNotTeamMemberException extends AccountRuleViolationException {

    public HelperNotTeamMemberException(String helperId, Object status) {
        super("Helper " + helperId + " is not a team member, but " + status);
    }
}
