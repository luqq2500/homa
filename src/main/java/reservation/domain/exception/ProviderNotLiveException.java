package reservation.domain.exception;

public class ProviderNotLiveException extends RuntimeException {

    public ProviderNotLiveException(String providerId) {
        super("The provider " + providerId + " is not taking requests");
    }
}
