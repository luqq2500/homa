package account.usecase.exception;

public class DataRequestNotFoundException extends RuntimeException {

    public DataRequestNotFoundException(String requestId) {
        super("No data request " + requestId);
    }
}
