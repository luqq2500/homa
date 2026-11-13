package policy.usecase.exception;

public class UnknownHomaValueException extends RuntimeException {

    public UnknownHomaValueException(String value) {
        super("Homa sets no value called " + value);
    }
}
