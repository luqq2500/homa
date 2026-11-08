package account.domain.exception;

/** Coordinates are a latitude from -90 to 90 and a longitude from -180 to 180. */
public class InvalidCoordinatesException extends AccountRuleViolationException {

    public InvalidCoordinatesException(double latitude, double longitude) {
        super("Not a place on Earth: " + latitude + ", " + longitude);
    }
}
