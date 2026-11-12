package catalogue.domain.exception;

/** A featured search needs a title, such as "Weekend quick refresh". */
public class FeaturedSearchTitleRequiredException extends CatalogueRuleViolationException {

    public FeaturedSearchTitleRequiredException() {
        super("A featured search needs a title");
    }
}
