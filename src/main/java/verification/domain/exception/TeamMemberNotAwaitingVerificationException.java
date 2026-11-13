package verification.domain.exception;

public class TeamMemberNotAwaitingVerificationException extends RuntimeException {

    public TeamMemberNotAwaitingVerificationException(String memberId) {
        super("The person " + memberId + " is not waiting for verification");
    }
}
