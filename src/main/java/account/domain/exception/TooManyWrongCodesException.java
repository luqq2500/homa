package account.domain.exception;

public class TooManyWrongCodesException extends RuntimeException {

    public TooManyWrongCodesException() {
        super("Too many wrong codes; ask for a new one");
    }
}
