package catalogue.domain.exception;

public class FeaturedSearchNotFoundException extends RuntimeException {

    public FeaturedSearchNotFoundException(String searchId) {
        super("No featured search " + searchId + " in the showcase");
    }
}
