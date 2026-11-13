package offering.domain.exception;

public class ProfileIncompleteException extends RuntimeException {

    public ProfileIncompleteException() {
        super("A profile has a name and a picture");
    }
}
