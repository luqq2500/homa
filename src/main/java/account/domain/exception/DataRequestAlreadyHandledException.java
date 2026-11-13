package account.domain.exception;

public class DataRequestAlreadyHandledException extends RuntimeException {

    public DataRequestAlreadyHandledException(String requestId) {
        super("The data request " + requestId + " is already handled");
    }
}
