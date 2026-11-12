package catalogue.domain.exception;

/** A shelf has one place on the homepage. */
public class ShelfAlreadyOnHomepageException extends CatalogueRuleViolationException {

    public ShelfAlreadyOnHomepageException(String shelfId) {
        super("Shelf " + shelfId + " is already on the homepage");
    }
}
