package offering.domain.exception;

public class InvalidHelperRangeException extends RuntimeException {

    public InvalidHelperRangeException(String problem) {
        super(problem);
    }
}
