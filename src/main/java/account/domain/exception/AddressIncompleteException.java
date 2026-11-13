package account.domain.exception;

public class AddressIncompleteException extends RuntimeException {

    public AddressIncompleteException() {
        super("An address has its text, its pin on the map and its city");
    }
}
