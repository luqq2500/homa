package account.domain.exception;

public class PhoneCodeNotAcceptedException extends RuntimeException {

    public PhoneCodeNotAcceptedException(String check) {
        super("The SMS code was not accepted: " + check);
    }
}
