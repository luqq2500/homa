package account.usecase.exception;

/** No customer has this id. */
public class CustomerNotFoundException extends AccountUsecaseException {

    public CustomerNotFoundException(String customerId) {
        super("There is no customer " + customerId);
    }
}
