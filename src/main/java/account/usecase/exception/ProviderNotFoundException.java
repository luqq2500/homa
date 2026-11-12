package account.usecase.exception;

/** No provider has this id. */
public class ProviderNotFoundException extends AccountUsecaseException {

    public ProviderNotFoundException(String providerId) {
        super("There is no provider " + providerId);
    }
}
