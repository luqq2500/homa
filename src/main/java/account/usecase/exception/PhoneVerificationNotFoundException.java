package account.usecase.exception;

public class PhoneVerificationNotFoundException extends RuntimeException {

    public PhoneVerificationNotFoundException(String verificationId) {
        super("No phone verification " + verificationId);
    }
}
