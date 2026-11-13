package reservation.domain.exception;

public class ProviderNotVerifiedForJobException extends RuntimeException {

    public ProviderNotVerifiedForJobException() {
        super("The provider is not verified for the whole job");
    }
}
