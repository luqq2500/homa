package account.usecase.exception;

/** No phone verification has this id, or it was already used to register. */
public class PhoneVerificationNotFoundException extends AccountUsecaseException {

    public PhoneVerificationNotFoundException(String verificationId) {
        super("There is no phone verification " + verificationId);
    }
}
