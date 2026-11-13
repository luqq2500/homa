package reservation.usecase.exception;

public class ProviderNotFoundException extends RuntimeException {

    public ProviderNotFoundException(String providerId) {
        super("No provider " + providerId);
    }
}
