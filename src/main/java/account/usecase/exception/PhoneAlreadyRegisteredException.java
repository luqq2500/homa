package account.usecase.exception;

/** The phone number already has an account of this kind; one phone number has one customer account and one provider account at most. */
public class PhoneAlreadyRegisteredException extends AccountUsecaseException {

    public PhoneAlreadyRegisteredException(String kind) {
        super("This phone number already has a " + kind + " account");
    }
}
