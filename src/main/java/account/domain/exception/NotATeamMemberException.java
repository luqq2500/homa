package account.domain.exception;

/** Only a member on the team can lapse; a lapsed one is already off it. */
public class NotATeamMemberException extends AccountRuleViolationException {

    public NotATeamMemberException(String memberId, Object status) {
        super("Team member " + memberId + " is not on the team, but " + status);
    }
}
