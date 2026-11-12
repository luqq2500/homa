package catalogue.usecase.exception;

/** No shelf has this id. */
public class ShelfNotFoundException extends CatalogueUsecaseException {

    public ShelfNotFoundException(String shelfId) {
        super("There is no shelf " + shelfId);
    }
}
