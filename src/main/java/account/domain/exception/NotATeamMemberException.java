package account.domain.exception;

/** Re-verification is for current team members only; a lapsed one is already off the team. */
public class NotATeamMemberException extends AccountRuleViolationException {

    public NotATeamMemberException(String memberId, Object status) {
        super("Team member " + memberId + " is not on the team, but " + status);
    }
}
