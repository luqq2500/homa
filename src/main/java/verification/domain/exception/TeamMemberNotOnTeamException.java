package verification.domain.exception;

public class TeamMemberNotOnTeamException extends RuntimeException {

    public TeamMemberNotOnTeamException(String memberId) {
        super("The person " + memberId + " is not a team member");
    }
}
