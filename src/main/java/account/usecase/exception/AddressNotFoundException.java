package account.usecase.exception;

/** The geoservice could not find the address, so Homa cannot tell its city or locate check-in. */
public class AddressNotFoundException extends AccountUsecaseException {

    public AddressNotFoundException(String street) {
        super("The address could not be found: " + street);
    }
}
