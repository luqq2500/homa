package verification.usecase.exception;

public class TeamMemberNotFoundException extends RuntimeException {

    public TeamMemberNotFoundException(String memberId) {
        super("No such person " + memberId + " with this business");
    }
}
