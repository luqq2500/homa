package account.usecase.exception;

/** A business acts only on its own team. */
public class NotYourTeamMemberException extends AccountUsecaseException {

    public NotYourTeamMemberException(String memberId, String providerId) {
        super("Team member " + memberId + " is not on the team of provider " + providerId);
    }
}
