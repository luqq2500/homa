package catalogue.domain.exception;

/** The shelf is not on the homepage. */
public class ShelfNotOnHomepageException extends CatalogueRuleViolationException {

    public ShelfNotOnHomepageException(String shelfId) {
        super("Shelf " + shelfId + " is not on the homepage");
    }
}
