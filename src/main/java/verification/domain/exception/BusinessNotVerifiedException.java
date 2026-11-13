package verification.domain.exception;

public class BusinessNotVerifiedException extends RuntimeException {

    public BusinessNotVerifiedException(String businessId) {
        super("The business " + businessId + " is not verified, so it cannot add or re-verify team members");
    }
}
