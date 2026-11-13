package offering.domain.exception;

public class InvalidHoursException extends RuntimeException {

    public InvalidHoursException(String problem) {
        super(problem);
    }
}
