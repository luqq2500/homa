package catalogue.domain.exception;

public class PlaceOutsideShowcaseException extends RuntimeException {

    public PlaceOutsideShowcaseException(int place) {
        super("The showcase has no place " + place);
    }
}
