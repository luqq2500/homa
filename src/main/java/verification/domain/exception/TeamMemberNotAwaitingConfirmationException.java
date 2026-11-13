package verification.domain.exception;

public class TeamMemberNotAwaitingConfirmationException extends RuntimeException {

    public TeamMemberNotAwaitingConfirmationException(String memberId) {
        super("The person " + memberId + " is not waiting for the provider's confirmation");
    }
}
