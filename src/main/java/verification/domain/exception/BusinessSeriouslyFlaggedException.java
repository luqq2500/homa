package verification.domain.exception;

public class BusinessSeriouslyFlaggedException extends RuntimeException {

    public BusinessSeriouslyFlaggedException(String businessId) {
        super("The business " + businessId + " is seriously flagged");
    }
}
