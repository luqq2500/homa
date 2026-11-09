package offering.domain.exception;

/** Each team member has one role: a helper or a supervisor. */
public class TeamMemberInTwoRolesException extends OfferingRuleViolationException {

    public TeamMemberInTwoRolesException(String memberId) {
        super("Team member " + memberId + " cannot be both a helper and a supervisor");
    }
}
