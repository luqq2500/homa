package catalogue.usecase.exception;

/** A catalogue usecase could not be carried out. Every catalogue usecase exception is one of these. */
public abstract class CatalogueUsecaseException extends RuntimeException {

    protected CatalogueUsecaseException(String message) {
        super(message);
    }
}
