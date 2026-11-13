package offering.usecase.exception;

public class OfferingNotFoundException extends RuntimeException {

    public OfferingNotFoundException(String providerId) {
        super("No provider " + providerId);
    }
}
