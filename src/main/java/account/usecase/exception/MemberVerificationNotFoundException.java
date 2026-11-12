package account.usecase.exception;

/** No team member verification has this id. */
public class MemberVerificationNotFoundException extends AccountUsecaseException {

    public MemberVerificationNotFoundException(String verificationId) {
        super("There is no team member verification " + verificationId);
    }
}
