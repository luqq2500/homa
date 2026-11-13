package catalogue.domain.exception;

public class FeaturedSearchInvalidException extends RuntimeException {

    public FeaturedSearchInvalidException(String problem) {
        super(problem);
    }
}
