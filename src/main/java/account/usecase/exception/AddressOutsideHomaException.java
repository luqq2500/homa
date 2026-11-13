package account.usecase.exception;

public class AddressOutsideHomaException extends RuntimeException {

    public AddressOutsideHomaException() {
        super("Homa does not operate at this address yet");
    }
}
