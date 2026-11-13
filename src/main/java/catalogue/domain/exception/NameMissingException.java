package catalogue.domain.exception;

public class NameMissingException extends RuntimeException {

    public NameMissingException() {
        super("A name is needed");
    }
}
