package reservation.usecase.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String customerId) {
        super("No customer " + customerId);
    }
}
