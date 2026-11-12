package policy.usecase.exception;

/** The amount is not written the way this value's kind is. */
public class InvalidPolicyAmountException extends PolicyUsecaseException {

    public InvalidPolicyAmountException(String value, String amount, String expected) {
        super(value + " needs " + expected + ", was " + amount);
    }
}
