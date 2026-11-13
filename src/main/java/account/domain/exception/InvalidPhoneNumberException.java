package account.domain.exception;

public class InvalidPhoneNumberException extends RuntimeException {

    public InvalidPhoneNumberException(String number) {
        super("Not a Malaysian mobile number: " + number);
    }
}
