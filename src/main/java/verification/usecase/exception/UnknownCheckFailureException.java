package verification.usecase.exception;

public class UnknownCheckFailureException extends RuntimeException {

    public UnknownCheckFailureException(String result) {
        super("The KYC provider reported an unknown result: " + result);
    }
}
