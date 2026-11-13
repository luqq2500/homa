package availability.domain.exception;

public class InvalidBufferException extends RuntimeException {

    public InvalidBufferException() {
        super("A buffer between jobs cannot be negative");
    }
}
