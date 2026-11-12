package catalogue.domain.exception;

/** No featured search in the showcase has this id. */
public class FeaturedSearchNotInShowcaseException extends CatalogueRuleViolationException {

    public FeaturedSearchNotInShowcaseException(String featuredSearchId) {
        super("There is no featured search " + featuredSearchId + " in the showcase");
    }
}
