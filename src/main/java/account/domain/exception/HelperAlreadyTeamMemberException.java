package account.domain.exception;

/** A team member is already verified; a new eKYC result does not change their place on the team. */
public class HelperAlreadyTeamMemberException extends AccountRuleViolationException {

    public HelperAlreadyTeamMemberException(String helperId) {
        super("Helper " + helperId + " is already a team member");
    }
}
