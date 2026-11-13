package account.domain.exception;

public class PhoneNotVerifiedException extends RuntimeException {

    public PhoneNotVerifiedException() {
        super("The phone number is not verified");
    }
}
