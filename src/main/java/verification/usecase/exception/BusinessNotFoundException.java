package verification.usecase.exception;

public class BusinessNotFoundException extends RuntimeException {

    public BusinessNotFoundException(String businessId) {
        super("No business " + businessId);
    }
}
