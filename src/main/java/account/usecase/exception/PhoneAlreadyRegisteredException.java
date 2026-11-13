package account.usecase.exception;

public class PhoneAlreadyRegisteredException extends RuntimeException {

    public PhoneAlreadyRegisteredException(String accountKind) {
        super("This phone number already has " + accountKind + " account");
    }
}
