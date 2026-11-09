package account.domain.exception;

/** A removed team member cannot come back onto the team. */
public class TeamMemberRemovedException extends AccountRuleViolationException {

    public TeamMemberRemovedException(String memberId) {
        super("Team member " + memberId + " has been removed from the team");
    }
}
